# SpotterAI

Encuentra a la persona con la que entrenar. No a cualquiera: a la que coincide contigo en gimnasio, nivel, objetivo **y horario real**.

> Evolución del TFG *FitConnect*, reconstruida sobre un motor de compatibilidad que cruza horarios al minuto y una interfaz rediseñada.

---

## Qué hace

1. **Perfil** — nivel, objetivo, gimnasio habitual y franjas horarias en las que entrenas.
2. **Match** — SpotterAI puntúa la compatibilidad y te explica *por qué* encajáis.
3. **Conexión** — solicitud, aceptación y chat cuando hay match mutuo.
4. **Diario** — registro de entrenamientos con meta semanal y progreso.

El nombre viene de *spotter*: quien te asiste en el banco, el compañero que te permite levantar más de lo que levantarías solo.

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 4 · Java 21 · Maven |
| Persistencia | Spring Data JPA · MySQL |
| Seguridad | Spring Security · JWT · BCrypt |
| Frontend | Angular 21 (standalone + signals) · SCSS |

---

## Cómo funciona el match

El emparejamiento tiene dos capas, deliberadamente separadas:

**1. Motor determinista** (`matching/CalculadoraCompatibilidad`) — puntúa de 0 a 100 cruzando horarios reales, nivel, objetivo, gimnasio y edad. Mismo input, mismo resultado. Sin coste, sin red, testeado.

| Factor | Peso |
|---|---:|
| Solape horario | 40 |
| Nivel | 20 |
| Objetivo | 20 |
| Gimnasio | 15 |
| Edad | 5 |

**2. Explicación** (`matching/ExplicadorCompatibilidad`) — hila los textos que ya trae cada factor, sin inventar nada. Solo menciona factores que sumaron puntos, así que la explicación nunca puede contradecir a la nota que acompaña.

> Hubo una versión que pasaba este desglose por la API de Claude para darle mejor prosa. Está aparcada en [`docs/ia-aparcada/`](docs/ia-aparcada/) con el motivo y las instrucciones para devolverla.

---

## Arrancar en local

**Requisitos:** JDK 21, Node 20+, MySQL en marcha.

Crea la base de datos:

```bash
mysql -u root -p -e "CREATE DATABASE spotterai_db CHARACTER SET utf8mb4;"
```

Variables de entorno del backend:

| Variable | Obligatoria | Para qué |
|---|---|---|
| `JWT_SECRET` | Sí | Firma de tokens. Mínimo 32 caracteres — la app no arranca sin ella. |
| `DB_USER`, `DB_PASSWORD`, `DB_NAME` | No | Por defecto `root` / vacía / `spotterai_db`. |
| `FRONTEND_ORIGIN` | No | Por defecto `http://localhost:4200`. |

Genera el secreto JWT:

```bash
openssl rand -base64 48
```

Backend (`:8080`):

```bash
cd backend && ./mvnw spring-boot:run
```

Frontend (`:4200`):

```bash
cd frontend && npm install && npm start
```

---

## Estructura

```
backend/src/main/java/com/spotterai/backend/
  config/       Seguridad, JWT, CORS
  controllers/  Endpoints REST
  services/     Lógica de negocio (interfaz + Impl)
  repositories/ Spring Data JPA
  models/       Entidades JPA
  dtos/         Contratos de entrada/salida

frontend/src/app/
  components/   Vistas standalone
  services/     Cliente HTTP
  guards/       Protección de rutas
  interceptors/ Inyección del JWT
frontend/src/scss/  Tokens de diseño
```
