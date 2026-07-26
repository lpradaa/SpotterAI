# SpotterAI

Encuentra a la persona con la que entrenar. No a cualquiera: a la que coincide contigo en gimnasio, nivel, objetivo **y horario real**.

> Evolución del TFG *FitConnect*, reconstruida sobre un motor de compatibilidad que cruza horarios al minuto y una interfaz rediseñada.

---

## Qué hace

1. **Horario primero** — antes de entrar, pintas tu semana en una rejilla. Es obligatorio: sin saber cuándo entrenas no hay forma de cruzarte con nadie, y el horario pesa el 40 % de la compatibilidad.
2. **Match** — SpotterAI puntúa de 0 a 100 y te enseña **en qué franjas concretas** coincidís, distinguiendo las que los dos tenéis fijas.
3. **Conexión** — solicitud, aceptación y chat cuando hay match mutuo. Todo en tiempo real por SSE: mensajes y solicitudes llegan sin recargar.
4. **Diario** — registro de entrenamientos con meta semanal y progreso.

Si al perfil le falta algo, la aplicación no dice "completo al 60 %": dice cuántos de los 100 puntos de compatibilidad quedan fuera de juego y por qué. Eso se puede decidir; un porcentaje de relleno, no.

El nombre viene de *spotter*: quien te asiste en el banco, el compañero que te permite levantar más de lo que levantarías solo.

---

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 4 · Java 21 · Maven |
| Persistencia | Spring Data JPA · MySQL/MariaDB · Flyway |
| Tiempo real | SSE (`SseEmitter`), con tickets de un solo uso |
| Seguridad | Spring Security · JWT · BCrypt |
| Frontend | Angular 21 (standalone + signals) · SCSS |
| Pruebas | JUnit 5 · Mockito · H2 |

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

Dos decisiones del motor que no son obvias:

**Disponibilidad no es compromiso.** Una franja puede estar marcada como *voy siempre* (máximo 3) o solo como *podría ir*. Coincidir en una hora que los dos tenéis fija vale mucho más que coincidir en una que los dos "quizá". Es también lo que evita que un horario pintado con desgana para pasar la pantalla de bienvenida pese como uno real.

**Menos datos no puede dar mejor nota.** Al repartir el peso de los factores que faltan, un perfil vacío llegó a puntuar 100 y colocarse por encima de gente con seis horas reales de solape. Se corrigió con un descuento por evidencia, y hay una prueba (`masDatosNuncaPuntuanPeorQueMenos`) que impide que vuelva.

**2. Explicación** (`matching/ExplicadorCompatibilidad`) — hila los textos que ya trae cada factor, sin inventar nada. Solo menciona factores que sumaron puntos, así que la explicación nunca puede contradecir a la nota que acompaña.

> Hubo una versión que pasaba este desglose por la API de Claude para darle mejor prosa. Está aparcada en [`docs/ia-aparcada/`](docs/ia-aparcada/) con el motivo y las instrucciones para devolverla.

---

## Arrancar en local

**Requisitos:** JDK 21, Node 20+, MySQL en marcha.

Crea la base de datos, vacía:

```bash
mysql -u root -p -e "CREATE DATABASE spotterai_db CHARACTER SET utf8mb4;"
```

Las tablas las crea **Flyway** al arrancar, desde `backend/src/main/resources/db/migration`. Hibernate va en `validate`: comprueba que las entidades cuadran con el esquema y se niega a arrancar si no, en lugar de cambiar la base por su cuenta. Para tocar el esquema se añade un `V2__…sql`, nunca se edita una migración ya aplicada.

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
  matching/     El motor: solape, pesos, explicación
  eventos/      Canal SSE y tickets de suscripción
  repositories/ Spring Data JPA
  models/       Entidades JPA
  dtos/         Contratos de entrada/salida
backend/src/main/resources/db/migration/  Migraciones Flyway

frontend/src/app/
  components/   Vistas standalone
  services/     Cliente HTTP, canal de eventos, tema
  guards/       Protección de rutas (sesión y horario)
  interceptors/ JWT y manejo de sesión caducada
frontend/src/scss/  Tokens de diseño y componentes compartidos
```

Dos piezas que conviene mirar si vienes de fuera: `matching/CalculadoraCompatibilidad` es donde vive el producto, y `components/rejilla-semana` es lo que lo hace visible.

---

## Lo que falta

- **Chat sin indicador de escritura ni presencia.** Los mensajes llegan al instante, pero no se ve si el otro está escribiendo.
- **Sin paginación.** Con decenas de usuarios sobra; con miles no.
- **`DisponibilidadController` expone un CRUD que nadie llama** — los horarios se gestionan dentro de `PUT /perfil`.
- **El par invertido de solicitudes** (A pide a B y B pide a A) se comprueba en Java, no en la base: necesitaría columnas generadas con `LEAST`/`GREATEST`.
- **Sin CI ni Docker.**
