# 🏠 Residencias Backend — Gestión de Residencias y Habitaciones Estudiantiles

Este repositorio contiene el **backend monolítico** del sistema de gestión de **residencias universitarias**, desarrollado con **Spring Boot** y arquitectura **en capas híbridas (Clean Architecture - N Capas)**.

---

## 🎯 Objetivo del Proyecto

Construir un sistema modular que permita administrar residencias, habitaciones, reservas, pagos y usuarios (estudiantes y administradores), brindando una base sólida para su consumo por parte de una **aplicación móvil**.

---

## ⚙️ Arquitectura y Tecnologías

- **Framework:** Spring Boot 3
- **Arquitectura:** Hybrid Architecture (Clean Architecture - N Capas) 
- **Persistencia:** JPA / Hibernate  
- **Base de Datos:** MySQL  
- **Seguridad:** Spring Security + JWT
- **IDE Recomendado:** Spring Tools Suite 4 o IntelliJ IDEA  
- **Control de versiones:** Git + GitHub  
- **Metodología:** Scrum

---
## 📁 Estructura del Proyecto

```
edu.pe.residencias
├── config
├── controller
├── exception
├── model
│   ├── dto
│   ├── entity
│   └── enums
├── repository
├── service
│   └── impl
└── utils
    ├── mappers
    └── validators
```
---

## 🌿 Flujo de Ramas (Git Flow Adaptado)

Para mantener orden en el desarrollo, seguimos esta estructura:

- **`main`** → Rama oficial, estable y validada.  
- **`develop-sprintX`** → Rama de integración de cada sprint (ej: `develop-sprint1`).
  
---

## 🛠️ Guía de Uso — Comandos Principales

**Clonar el repositorio**
   ```bash
   git clone https://github.com/axell726-cp/flutter-app-backend.git
   cd flutter-app-backend
 ```
---
## 👥 Equipo y Colaboración

Team Backend
