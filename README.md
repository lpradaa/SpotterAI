# SpotterAI

Encuentra a la persona con la que entrenar. No a cualquiera: a la que coincide contigo en gimnasio, nivel, objetivo **y horario real**.

> Evolución del TFG *FitConnect*, reconstruida sobre un motor de compatibilidad que cruza horarios al minuto y una interfaz rediseñada.

---

## Qué hace

1. **Horario primero** — antes de entrar, pintas tu semana en una rejilla. Es obligatorio: sin saber cuándo entrenas no hay forma de cruzarte con nadie, y el horario pesa el 40 % de la compatibilidad.
2. **Match** — SpotterAI puntúa de 0 a 100 y te enseña **en qué franjas concretas** coincidís, distinguiendo las que los dos tenéis fijas.
3. **Conexión** — solicitud, aceptación y chat cuando hay match mutuo. Todo en tiempo real por SSE: mensajes, solicitudes y sesiones llegan sin recargar.
4. **Sesión** — el paso que convierte un porcentaje en un plan: propones un día y una hora concretos desde el chat o desde su ficha, y el formulario ya viene relleno con el próximo hueco que compartís, sacado del mismo solape con el que se calcula la compatibilidad. El otro acepta o rechaza, y la ficha de cada persona lleva la cuenta de las veces que ya habéis quedado.
5. **Diario** — registro de entrenamientos con meta semanal y progreso. Una sesión que ya ha pasado se apunta con un clic, y cada uno apunta la suya: que el otro diga que entrenasteis no prueba que tú fueras.

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
| Pruebas | JUnit 5 · Mockito · H2 · Vitest |

---

## Cómo funciona el match

El emparejamiento tiene dos capas, deliberadamente separadas:

**1. Motor determinista** (`matching/CalculadoraCompatibilidad`) — puntúa de 0 a 100 cruzando horarios reales, fuerza, nivel, objetivo, rutina, gimnasio y edad. Mismo input, mismo resultado. Sin coste, sin red, testeado.

| Factor | Peso |
|---|---:|
| Solape horario | 40 |
| Objetivo | 15 |
| Gimnasio | 15 |
| Nivel | 10 |
| Fuerza | 10 |
| Edad | 5 |
| Rutina | 5 |

Tres decisiones del motor que no son obvias:

**La fuerza y la rutina son las que justifican el nombre.** Un spotter que no puede con tu peso es un testigo, no un spotter: por eso se comparan las marcas principales (1RM estimado con Epley) y no los ejercicios que le gustan a cada uno. Y la rutina decide si compartís sesión de verdad: coincidir un martes con quien ese día hace pierna mientras tú haces pecho es coincidir en el gimnasio, no entrenar juntos.

**Disponibilidad no es compromiso.** Una franja puede estar marcada como *voy siempre* (máximo 3) o solo como *podría ir*. Coincidir en una hora que los dos tenéis fija vale mucho más que coincidir en una que los dos "quizá". Es también lo que evita que un horario pintado con desgana para pasar la pantalla de bienvenida pese como uno real.

**Menos datos no puede dar mejor nota.** Al repartir el peso de los factores que faltan, un perfil vacío llegó a puntuar 100 y colocarse por encima de gente con seis horas reales de solape. Se corrigió con un descuento por evidencia, y hay una prueba (`masDatosNuncaPuntuanPeorQueMenos`) que impide que vuelva.

**2. Explicación** (`matching/ExplicadorCompatibilidad`) — hila los textos que ya trae cada factor, sin inventar nada. Solo menciona factores que sumaron puntos, así que la explicación nunca puede contradecir a la nota que acompaña.

> Hubo una versión que pasaba este desglose por la API de Claude para darle mejor prosa. Está aparcada en [`docs/ia-aparcada/`](docs/ia-aparcada/) con el motivo y las instrucciones para devolverla.

---

## Arrancar

### Con Docker — un comando

**Requisitos:** Docker.

```bash
docker compose up
```

La aplicación queda en **http://localhost:4200**, y no vacía:

| | |
|---|---|
| **Usuario** | `demo@spotterai.test` |
| **Contraseña** | `Demo1234` |

Levanta MariaDB, backend y frontend; Flyway crea el esquema en el primer arranque. El backend no publica puerto: todo pasa por nginx, que reenvía `/api` al contenedor del backend. Eso es lo que permite que el frontend compilado no lleve dentro ninguna dirección concreta, y de paso elimina el CORS.

### Los datos de demostración

Una aplicación de emparejar gente no se puede enseñar vacía: sin nadie con quien cruzarte no hay compatibilidad que calcular, y el motor —que es lo único que merece la pena mirar— queda invisible. Por eso el arranque con Docker trae catorce personas, tres gimnasios, horarios que solapan de verdad, marcas de fuerza, un par de conversaciones y planes en marcha: una propuesta esperando tu respuesta, una sesión aceptada por delante y otra ya pasada sin apuntar.

Vive en `demo/SembradorDemo`, detrás del perfil `demo`, y **no** en una migración: los datos de mentira dentro de una migración acaban en cualquier instalación de verdad y ya no hay quien los separe de los reales. Las fechas son relativas al día en que arrancas, para que la demostración no se pudra sola. Nadie tiene foto porque las fotos son datos de usuario y no van al repositorio; se ven las iniciales sobre color, que es exactamente lo que hace la aplicación cuando no hay foto.

Para arrancar con la base limpia:

```bash
SPRING_PROFILES=default docker compose up
```

Para un despliegue de verdad, pon tu propio secreto en un `.env`:

```bash
echo "JWT_SECRET=$(openssl rand -base64 48)" > .env
```

Las fotos y vídeos van en un volumen (`medios`), no dentro de la imagen: si no, cada despliegue borraría lo que haya subido la gente.

### Sin Docker

**Requisitos:** JDK 21, Node 20+, MySQL o MariaDB en marcha.

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
| `SPRING_PROFILES_ACTIVE` | No | `demo` siembra los datos de ejemplo. Sin valor, la base se queda como esté. |

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

El frontend llama al backend con rutas relativas (`/api/…`) y quien las reenvía es el proxy de `ng serve`, configurado en `frontend/proxy.conf.json`.

---

## Integración continua

Cada push a `main` y cada pull request ejecuta [este workflow](.github/workflows/ci.yml):

- **Backend** — `./mvnw verify`, la suite entera sobre H2 en memoria. Si algo falla, sube los informes de surefire como artefacto.
- **Frontend** — `npm ci` y compilación de producción. `ci` y no `install`: falla si `package.json` y el lock se han desincronizado, que es justo lo que interesa saber.
- **Imágenes** — construye las dos imágenes Docker (sin publicarlas). Comprueba que los `Dockerfile` siguen siendo válidos, que es lo que se rompe sin que nadie lo note hasta que alguien intenta levantar el proyecto.

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

## Seguridad

Lo que hay, y por qué:

- **Contraseñas con BCrypt**, nunca en claro ni reversibles.
- **JWT** firmado con `JWT_SECRET`, que no tiene valor por defecto: la aplicación se niega a arrancar sin él y exige 32 caracteres mínimo.
- **Freno a la fuerza bruta** en el login (`seguridad/ControlDeIntentos`). Cinco intentos fallidos por correo y treinta por dirección, en ventana de quince minutos, y responde `429` con `Retry-After`.

  Tres decisiones que no son obvias. El tope por dirección es seis veces más alto porque detrás de una IP puede haber un gimnasio entero compartiendo salida. Los correos que **no existen** también gastan cupo: si solo contaran los fallos de cuentas reales, ver cuál se bloquea diría quién está registrado y el freno acabaría siendo un listador de usuarios. Y estando bloqueado ni se llega a comprobar la contraseña, porque el coste de BCrypt es justo lo que un ataque quiere consumir.

  Vive en memoria. Con varias instancias haría falta algo compartido, porque cada una contaría por su lado.

- **Una sola relación por pareja**, en la base y no solo en Java. Dos peticiones simultáneas pueden leer las dos que no hay nada y las dos insertar; una comprobación en el servicio no para eso. `uk_solicitud_pareja` va sobre dos columnas generadas con `LEAST`/`GREATEST`, de modo que "A y B" y "B y A" producen la misma clave.

- **Los medios subidos se sirven desde rutas propias.** El perfil solo acepta `fotoUrl` que empiece por `/api/medios/`: sin eso, el campo sería un hueco para que cualquier navegador que abriera un perfil pidiera la URL que le pusieran.

### Accesibilidad

- **La rejilla semanal tiene equivalente textual.** Es el elemento central —dónde podéis entrenar juntos— y transmitía todo en color y posición: las cabeceras iban con `aria-hidden` y los tramos eran `span` vacíos. Ahora el dibujo es decorativo y al lado va la misma información en palabras, tramo a tramo: *«Coincidís en 2 franjas. Lunes de 18:00 a 20:00: coincidís y los dos vais siempre»*.
- **Los diálogos se comportan como diálogos** (`directivas/modal-accesible`): cierran con `Escape`, atrapan el foco —el tabulador se escapaba a los botones de detrás del velo— y lo devuelven al salir.
- **Foco visible.** No había ninguna regla de foco en todo el proyecto; ahora hay un anillo con `:focus-visible`, que aparece al tabular y no al hacer clic.
- **Contraste medido, no estimado.** `--sa-texto-tenue` daba 2,84:1 en oscuro y 2,21:1 en claro, contra los 4,5 que pide el nivel AA. Ahora 4,71 y 4,54 en el peor caso.

---

Lo que no hay, dicho claro: el token vive en `localStorage`, así que un XSS lo lee. Para esto —proyecto local, sin datos sensibles— es una decisión asumida; en un despliegue de verdad tocaría cookie `HttpOnly` con protección CSRF.

---

## Lo que falta

- **Chat sin indicador de escritura ni presencia.** Los mensajes llegan al instante, pero no se ve si el otro está escribiendo.
- **Sin paginación.** Con decenas de usuarios sobra; con miles no.
- **`DisponibilidadController` expone un CRUD que nadie llama** — los horarios se gestionan dentro de `PUT /perfil`.
- **Accesibilidad, solo lo básico.** Está el equivalente textual de la rejilla, el foco atrapado en los diálogos y el contraste medido; falta pasarle un lector de pantalla de verdad y revisar el orden de tabulación pantalla por pantalla.
