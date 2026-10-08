# Roadmap - Slate Backend

## Sprint 1 - Setup & Autenticación + Clientes
- [ ] `chore/initial-spring-boot-setup`: Estructura inicial de Spring Boot (Web, JPA, Postgres, Security).
- [ ] `docs/roadmap-and-pr-template`: Agregar ROADMAP.md y plantilla de PR.
- [ ] `chore/env-and-db-config`: Configuración de PostgreSQL y `.env.example`.
- [ ] `feat/auth-endpoints`: Endpoints de Registro e Iniciar Sesión con JWT.
- [ ] `feat/client-crud`: Endpoints CRUD de Clientes asociados al comerciante autenticado.

## Sprint 2 - Fiados y Pagos
- [ ] `feat/transactions-crud`: Endpoints para registrar fiados y pagos por cliente.
- [ ] `feat/client-balance`: Endpoint de saldo acumulado y lista "Quién me debe".

## Sprint 3 - Recordatorios y Scoring
- [ ] `feat/risk-score-engine`: Lógica de puntaje de riesgo por cliente según reglas negocio.
- [ ] `test/core-services-tests`: Pruebas unitarias básicas de cálculos de saldo y scoring.

## Colchón - Ajustes y Entrega
- [ ] `docs/final-readme`: Documentación de API y setup de desarrollo.
- [ ] `fix/pre-demo-adjustments`: Correcciones finales detectadas en pruebas integradas.