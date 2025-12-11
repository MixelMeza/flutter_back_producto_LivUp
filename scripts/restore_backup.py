#!/usr/bin/env python3
"""
Restore JSON DB backup created by the application's DatabaseBackupService.

Usage examples:
  pip install pymysql
  python scripts/restore_backup.py --backup backups/20251210_230043_startup --props src/main/resources/application.properties --truncate

The script will read backup JSON files (one per table) and INSERT rows into the target
MySQL database. By default it won't truncate tables; pass --truncate to TRUNCATE each table
before inserting.

WARNING: Running this against a production database may overwrite data. Stop the app
before restoring.
"""
import argparse
import json
import os
import re
import sys
from typing import Dict, Any, List

try:
    import pymysql
except ImportError:
    print("pymysql is required. Install with: pip install pymysql")
    sys.exit(1)


def read_properties(path: str) -> Dict[str, str]:
    props = {}
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                k, v = line.split('=', 1)
                props[k.strip()] = v.strip()
    return props


def parse_jdbc_url(url: str):
    # Example formats:
    # jdbc:mysql://user:pass@host:port/dbname?params
    # jdbc:mysql://host:port/dbname
    m = re.match(r'^jdbc:mysql://(?:(?P<user>[^:@/]+):(?P<pass>[^@/]+)@)?(?P<host>[^:/?#]+)(?::(?P<port>\d+))?/(?P<db>[^?]+)', url)
    if not m:
        return None
    return m.groupdict()


DEFAULT_ORDER = [
    'roles', 'persona', 'usuarios', 'residencias', 'habitaciones',
    'imagen_residencia', 'imagen_habitacion', 'contratos', 'pagos', 'abonos',
    'reviews', 'notificaciones', 'accesos', 'device_tokens', 'favoritos', 'gastos_residencia'
]


def restore_table(cursor, table: str, rows: List[Dict[str, Any]], truncate: bool):
    if not rows:
        print(f"Skipping empty table {table}")
        return
    cols = list(rows[0].keys())
    cols_sql = ','.join([f'`{c}`' for c in cols])
    placeholders = ','.join(['%s'] * len(cols))
    if truncate:
        cursor.execute(f"TRUNCATE TABLE `{table}`;")
        print(f"Truncated table {table}")
    sql = f"INSERT INTO `{table}` ({cols_sql}) VALUES ({placeholders})"
    values = []
    for r in rows:
        rowvals = []
        for c in cols:
            v = r.get(c)
            # Convert boolean to int for MySQL if needed
            if isinstance(v, bool):
                v = int(v)
            rowvals.append(v)
        values.append(tuple(rowvals))
    cursor.executemany(sql, values)
    print(f"Inserted {len(values)} rows into {table}")


def main():
    p = argparse.ArgumentParser(description='Restore DB from JSON backups')
    p.add_argument('--backup', required=True, help='Path to backup folder (contains *.json files)')
    p.add_argument('--props', required=False, help='Path to application.properties (to auto-detect DB creds)')
    p.add_argument('--host', help='DB host')
    p.add_argument('--port', type=int, help='DB port')
    p.add_argument('--db', help='DB name')
    p.add_argument('--user', help='DB user')
    p.add_argument('--password', help='DB password')
    p.add_argument('--truncate', action='store_true', help='TRUNCATE tables before insert')
    args = p.parse_args()

    if not os.path.isdir(args.backup):
        print('Backup folder not found:', args.backup)
        sys.exit(1)

    host = args.host
    port = args.port
    db = args.db
    user = args.user
    password = args.password

    if args.props and os.path.isfile(args.props):
        props = read_properties(args.props)
        if not host and 'spring.datasource.url' in props:
            parsed = parse_jdbc_url(props['spring.datasource.url'])
            if parsed:
                host = parsed.get('host')
                port = int(parsed.get('port')) if parsed.get('port') else 3306
                db = parsed.get('db')
                if parsed.get('user') and parsed.get('pass'):
                    user = parsed.get('user')
                    password = parsed.get('pass')
        if not user and 'spring.datasource.username' in props:
            user = props['spring.datasource.username']
        if not password and 'spring.datasource.password' in props:
            password = props['spring.datasource.password']

    if not host or not db or not user:
        print('Missing DB connection info. Provide --host/--db/--user or --props pointing to application.properties')
        sys.exit(1)

    if not password:
        print('Warning: no DB password provided; attempting empty password')

    print(f"Connecting to MySQL {host}:{port or 3306}/{db} as user {user}")
    conn = pymysql.connect(host=host, port=port or 3306, user=user, password=password or '', database=db, charset='utf8mb4')
    try:
        cur = conn.cursor()
        cur.execute('SET FOREIGN_KEY_CHECKS=0;')
        # Determine files present
        files = { os.path.splitext(f)[0]: os.path.join(args.backup, f) for f in os.listdir(args.backup) if f.endswith('.json') }
        order = DEFAULT_ORDER + [k for k in files.keys() if k not in DEFAULT_ORDER]
        for table in order:
            if table not in files:
                continue
            path = files[table]
            with open(path, 'r', encoding='utf-8') as fh:
                try:
                    rows = json.load(fh)
                except Exception as ex:
                    print('Failed to parse', path, ex)
                    continue
            if not isinstance(rows, list):
                print('Skipping non-array file', path)
                continue
            restore_table(cur, table, rows, args.truncate)
            conn.commit()

        cur.execute('SET FOREIGN_KEY_CHECKS=1;')
        conn.commit()
        print('Restore completed successfully')
    finally:
        conn.close()


if __name__ == '__main__':
    main()
