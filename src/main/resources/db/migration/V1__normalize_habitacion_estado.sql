-- Normalize `estado` column values in `habitaciones` to the canonical strings
-- that the application stores (matching HabitacionEstado converter):
--   DISPONIBLE   -> `disponible`
--   RESERVADO    -> `reservado`
--   OCUPADO      -> `ocupado`
--   NO_DISPONIBLE-> `no-disponible`

-- Make comparisons case-insensitive and trim spaces
UPDATE habitaciones
SET estado = 'disponible'
WHERE estado IS NOT NULL AND LOWER(TRIM(estado)) IN ('disponible','disponible ','disponible','available','disp','disponible');

UPDATE habitaciones
SET estado = 'reservado'
WHERE estado IS NOT NULL AND LOWER(TRIM(estado)) IN ('reservado','reservada','reserved');

UPDATE habitaciones
SET estado = 'ocupado'
WHERE estado IS NOT NULL AND LOWER(TRIM(estado)) IN ('ocupado','ocupada','occupied');

UPDATE habitaciones
SET estado = 'no-disponible'
WHERE estado IS NOT NULL AND LOWER(TRIM(estado)) IN ('no disponible','no-disponible','no_disponible','not-available','not available');

-- Optionally, set NULL or empty to 'disponible' if desired (safe fallback)
UPDATE habitaciones
SET estado = 'disponible'
WHERE estado IS NULL OR TRIM(estado) = '';
