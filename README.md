# SpotterAI

Encuentra a la persona con la que entrenar. No a cualquiera: a la que coincide contigo en gimnasio, nivel, objetivo **y horario real**.

> Evolución del TFG *FitConnect*, reconstruida sobre un motor de compatibilidad que cruza horarios al minuto y una interfaz rediseñada.

---

## Qué hace

1. **Lo mínimo, primero** — antes de entrar pintas tu semana y dices gimnasio, nivel, objetivo, rutina y edad. Es la regla que gobierna el perfil: **obligatorio lo que se declara, opcional lo que se mide**.

   Cinco desplegables y una rejilla cuestan poco y a cambio hacen que todas las puntuaciones sean comparables, porque dejan de calcularse unas con seis factores y otras con tres. Los levantamientos, en cambio, son una medición: obligarlos no produce datos, produce números inventados por quien no los sabe, y entrarían derechos al factor del que depende el nombre del producto. Un dato ausente al menos sabe que lo está.

   Cada campo exigido tiene una salida honesta que **es una respuesta**: «sin rutina fija» no es un hueco, es un dato. Sin esa salida, exigir un campo solo consigue que la gente marque la primera opción.
2. **Match** — SpotterAI puntúa de 0 a 100 y te enseña **en qué franjas concretas** coincidís, distinguiendo las que los dos tenéis fijas.
3. **Conexión** — solicitud, aceptación y chat cuando hay match mutuo. Todo en tiempo real por SSE: mensajes, solicitudes y sesiones llegan sin recargar.
4. **Sesión** — el paso que convierte un porcentaje en un plan: propones un día y una hora concretos desde el chat o desde su ficha, y el formulario ya viene relleno con el próximo hueco que compartís, sacado del mismo solape con el que se calcula la compatibilidad. El otro acepta o rechaza, y la ficha de cada persona lleva la cuenta de las veces que ya habéis quedado.

   Si cada uno entrena en un sitio, el formulario pregunta **dónde**: el tuyo o el suyo. Antes no lo preguntaba y la sesión se guardaba sin sitio, o sea que dos personas quedaban a una hora y en ninguna parte. Ninguno viene marcado por defecto — uno de los dos tiene que desplazarse y darlo por hecho sería decidir por ellos.
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

## Las pantallas

Cada una hace un trabajo, y solo uno. No fue así al principio: el tablero llegó a
concentrar la mitad de la aplicación —la lista de gente, las solicitudes, el
historial, cuatro modales— mientras Explorar era un duplicado de esa misma lista
con filtros.

| Ruta | Para qué |
|---|---|
| `/dashboard` | **Lo tuyo.** Tus sesiones, cómo vas la semana, qué le falta a tu perfil, y lo que han hecho últimamente tus compañeros |
| `/explorar` | **Buscar.** Toda la comunidad, con filtros, y el modo de fichas de una en una |
| `/u/:id` · `/yo` | **Una persona.** Su semana, sus marcas, cuántas veces habéis quedado. Y en la tuya, el formulario para cambiarte |
| `/conexiones` | **Hablar y quedar.** Chat, el plan en marcha y proponer |
| `/solicitudes` | **Lo que espera tu respuesta.** Solicitudes de conexión y propuestas de sesión |

Dos decisiones de este reparto:

**Las personas son lugares.** La ficha de alguien era un panel flotante incrustado
a la vez en el tablero y en Explorar: no había URL a la que enlazar, el botón
«atrás» no hacía lo esperable y el mismo componente vivía en dos pantallas. Es lo
que más separaba esto de una red social y lo que menos costaba arreglar.

**Y no hay muro.** La única concesión social es la actividad de tus compañeros en
el tablero: qué han entrenado, qué marcas han apuntado. Solo de la gente con la
que ya has conectado, sin «me gusta» ni comentarios ni contadores de seguidores.
Un muro abierto invita a publicar para que lo vean, y nada de eso ayuda a que dos
personas coincidan un martes, que es la vara con la que se ha medido todo lo
demás.

---

## Cómo funciona el match

El emparejamiento tiene dos capas, deliberadamente separadas:

**1. Motor determinista** (`matching/CalculadoraCompatibilidad`) — puntúa de 0 a 100 cruzando horarios reales, fuerza, nivel, objetivo, rutina, gimnasio y edad. Mismo input, mismo resultado. Sin coste, sin red, testeado.

| Factor | Peso | |
|---|---:|---|
| Solape horario | 40 | y el gimnasio lo condiciona |
| Objetivo | 12 | |
| **Constancia** | **10** | **lo único que se mide y no se declara** |
| Nivel | 10 | |
| Fuerza | 10 | |
| Gimnasio | 8 | además condiciona el horario |
| Rutina | 5 | |
| Edad | 5 | |

Cinco decisiones del motor que no son obvias:

**Coincidir en horario en gimnasios distintos no es coincidir.** El gimnasio no es un mérito que suma aparte: es la condición bajo la cual el solape significa algo. Tú a las seis en McFit y ella a las seis en Basic-Fit no estáis juntos, estáis en dos edificios de la ciudad a la misma hora — y la aplicación llegó a decir *«los dos vais siempre un día a la misma hora»* de una pareja así. Ahora el solape en otro gimnasio vale una cuarta parte, y la frase lo dice.

**...salvo que alguien esté dispuesto a moverse.** Esa cuarta parte era una media aplicada a todo el mundo por igual, y hay una diferencia real entre quien coge el metro tres paradas y quien no piensa moverse — que la aplicación no podía conocer porque nunca lo preguntaba. Es el único dato del emparejamiento que no se deduce de ningún otro: ni del horario, ni del gimnasio, ni del historial. Ahora se pregunta en el perfil, y basta con que lo diga **uno** de los dos, porque para que la pareja funcione solo hace falta que se mueva una persona. Sube el solape a 0,60, no a 1: desplazarse cuesta tiempo y a menudo una entrada, así que nunca empata con compartir sala. Y el factor gimnasio sigue dando cero — no comparten gimnasio, y estar dispuesto a viajar no cambia ese hecho; lo que cambia es lo que *significa* coincidir en horario.

**Un rato compartido no es una sesión.** Cualquier solape positivo contaba: quien estuviera libre de 19:55 a 21:00 compartía cinco minutos contigo y —si los dos lo teníais marcado como fijo— eso valía por «día ancla», o sea el 75 % del factor horario. Ahora hay un mínimo de 45 minutos, que es lo que dura la sesión más corta que sigue siendo una sesión.

**La constancia es el único dato que no se declara.** Todo lo demás sale de lo que alguien dice de sí mismo; esto sale de lo que ha hecho. Alguien puede encajar contigo al noventa por ciento y llevar mes y medio sin pisar el gimnasio: eso no es un buen compañero, es un buen compañero hipotético. Cuenta la del que menos aparece de los dos, porque una pareja entrena tan a menudo como su miembro menos constante. Y sin historial no se juzga: quien acaba de registrarse no ha hecho nada mal.

**La fuerza y la rutina son las que justifican el nombre.** Un spotter que no puede con tu peso es un testigo, no un spotter: por eso se comparan las marcas principales (1RM estimado con Epley) y no los ejercicios que le gustan a cada uno. Y la rutina decide si compartís sesión de verdad: coincidir un martes con quien ese día hace pierna mientras tú haces pecho es coincidir en el gimnasio, no entrenar juntos.

**Disponibilidad no es compromiso.** Una franja puede estar marcada como *voy siempre* (máximo 3) o solo como *podría ir*. Coincidir en una hora que los dos tenéis fija vale mucho más que coincidir en una que los dos "quizá". Es también lo que evita que un horario pintado con desgana para pasar la pantalla de bienvenida pese como uno real.

**Menos datos no puede dar mejor nota.** Al repartir el peso de los factores que faltan, un perfil vacío llegó a puntuar 100 y colocarse por encima de gente con seis horas reales de solape. Se corrigió con un descuento por evidencia, y hay una prueba (`masDatosNuncaPuntuanPeorQueMenos`) que impide que vuelva.

**2. Explicación** (`matching/ExplicadorCompatibilidad`) — hila los textos que ya trae cada factor, sin inventar nada. Solo menciona factores que sumaron puntos, así que la explicación nunca puede contradecir a la nota que acompaña.

**3. El desglose** — y debajo, si se pide, los ocho factores uno a uno: lo que aportó cada uno, sobre cuánto podía aportar y por qué. La apuesta de todo esto es que un porcentaje sin explicación no vale nada, y durante un tiempo se servía exactamente eso: un porcentaje y una frase, porque el desglose se calculaba entero y se aplastaba a una cadena antes de salir del backend. Enseñarlo es lo que hace visibles las dos decisiones más caras del motor:

- Un factor **sin datos** aparece aparte y sin barra, diciendo que no resta. Una barra vacía se leería como «puntuó cero», que es lo contrario de lo que pasa.
- Su peso **se reparte** entre los demás. Por eso puede salir «Edad 13/13» donde la tabla de arriba dice 5 — y sin decirlo, ese 13 parecería un error.

Lo que el desglose **no** dice es cuál de los dos perfiles está incompleto. La calculadora tiene los dos delante y aun así escribe «de alguno de los dos» sin señalar: afirmar que es el tuyo cuando puede ser el del otro sería inventar, que es justo lo que se evita al distinguir «no aplicable» de «cero puntos».

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

Para un despliegue de verdad, parte de la plantilla:

```bash
cp .env.example .env
```

Las fotos y vídeos van en un volumen (`medios`), no dentro de la imagen: si no, cada despliegue borraría lo que haya subido la gente.

### Antes de que entre alguien que no seas tú

Esto no es una lista de buenas prácticas genéricas: son las cuatro cosas que en este proyecto están puestas para desarrollo y que en producción hacen daño.

1. **`JWT_SECRET` propio.** `openssl rand -base64 48`. El valor por defecto del `docker-compose.yml` está escrito en un repositorio público, así que cualquiera puede firmarse un token.
2. **`SPRING_PROFILES` vacío.** Con `demo` se siembran catorce personas inventadas en cada arranque, y quien se registre de verdad va a encontrárselas en Explorar.
3. **`CORREO_URL_BASE` con tu dominio.** Es el fallo más fácil de cometer aquí: si se queda en `localhost`, los avisos salen con enlaces que no le funcionan a nadie y no te enteras, porque desde el servidor que los manda abren perfectamente.
4. **HTTPS delante, y `COOKIE_SEGURA=true`.** La galleta de sesión viaja en cada petición; sin TLS va en claro por la red. `COOKIE_SEGURA` está en `false` por defecto porque en `http://localhost` una galleta `Secure` no se manda y nadie podría entrar — pero en producción, sin ponerla a `true`, la sesión se puede leer en cualquier red por la que pase.

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

### Avisos por correo

Sin configurar nada **no se manda ningún correo**: los avisos se escriben en el registro con destinatario, asunto y cuerpo, que es lo que permite verlos en desarrollo sin tener un servidor SMTP. Para que salgan de verdad:

| Variable | Obligatoria | Para qué |
|---|---|---|
| `CORREO_ACTIVO` | — | `true` para mandar de verdad. Por defecto `false`. |
| `CORREO_URL_BASE` | Si `CORREO_ACTIVO=true` | Raíz de los enlaces del correo. **Si se queda en `localhost`, los avisos salen con enlaces que no le funcionan a nadie**, y no se nota porque desde el propio servidor abren bien. |
| `CORREO_REMITENTE` | Si `CORREO_ACTIVO=true` | Dirección desde la que se manda. El servidor de abajo tiene que permitirla. |
| `CORREO_HOST`, `CORREO_PUERTO` | Si `CORREO_ACTIVO=true` | Servidor SMTP. Por defecto `localhost:587`. |
| `CORREO_USUARIO`, `CORREO_CLAVE` | Según el servidor | Credenciales SMTP. |
| `CORREO_TLS`, `CORREO_AUTH` | No | Ambas `true` por defecto. |
| `CORREO_CADA_CUANTO_MS` | No | Cada cuánto se busca qué avisar. Por defecto 60 000. |

Ojo con `CORREO_HOST`: con `CORREO_ACTIVO=true` y sin ningún `spring.mail.host`, la aplicación **no arranca**. Por eso tiene valor por defecto.

Se avisa de dos cosas —una solicitud nueva y una propuesta de sesión— y solo si **siguen sin responder diez minutos después**. Quien lo vio en directo y contestó no recibe nada.

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

- **La sesión no está al alcance del JavaScript.** El token vivía en `localStorage`, que es un almacén al que llega cualquier script de la página: eso convierte **cualquier** XSS —propio, de una dependencia, de un anuncio— en robo de sesión, y el token robado sigue valiendo veinticuatro horas aunque la víctima cierre el navegador. Ahora va en una galleta `HttpOnly` que el navegador manda sola y el JavaScript no puede leer.

  Eso obliga a traer detrás la **protección CSRF**, porque una galleta se manda sola y por tanto otra página puede provocar peticiones con la sesión puesta. Van juntas `SameSite=Lax` y el token de doble envío: el servidor deja el suyo en una galleta que el JavaScript sí puede leer, la página lo copia a una cabecera, y otro sitio no puede leer nuestras galletas para hacer lo mismo.

  Y **cerrar sesión pasa a cerrarla de verdad**: antes era olvidarse del token, porque lo teníamos nosotros; ahora hay que pedirle al servidor que borre la galleta, y hasta que no lo hace la sesión sigue viva.

- **Se puede recuperar la contraseña, y cambiarla.** No se podía: quien la olvidaba se quedaba fuera de su cuenta para siempre y sin recurso. El enlace del correo dura una hora, sirve una sola vez y en la base solo vive su huella SHA-256 — a diferencia de la llave de baja de los avisos, que sí se guarda tal cual, porque con aquélla lo máximo que se consigue es dejar a alguien sin correos y con ésta se entra en su cuenta.

  Pedir el enlace **responde siempre lo mismo**, exista la cuenta o no: contestar distinto convertiría ese formulario en un comprobador de quién está registrado, abierto y sin sesión.

  Y **cambiar la contraseña echa a las sesiones abiertas**, que es lo que resuelve de paso la revocación del JWT: se guarda desde cuándo valen los tokens de cada persona y el filtro rechaza los anteriores. Sin eso, quien te hubiera robado la sesión seguiría dentro veinticuatro horas y el cambio solo tranquilizaría. El precio es que autenticar pasa a costar una consulta por petición: revocar exige estado en el servidor y no hay forma de evitarlo con tokens firmados.

- **Se puede bloquear a alguien.** No se podía. «Deshacer relación» solo borraba la fila de la solicitud, así que la otra persona podía mandarte otra al segundo siguiente y te seguía viendo en Explorar — con tu gimnasio y tu horario delante. En una aplicación que le enseña eso a un desconocido, no tener forma de cortar no es un hueco de comodidad.

  **Y también se puede reportar**, que es otra cosa. Bloquear te protege a ti; reportar es lo que permite enterarse de que la misma persona se ha portado mal con varias, algo que bloqueando por tu cuenta no se sabe nunca. Durante un tiempo aquí ponía que no habría botón de denunciar, porque *«una denuncia necesita a alguien que la lea y un botón sin moderación detrás es teatro —uno peligroso, porque quien lo pulsa se queda creyendo que ha hecho algo—»*. El argumento sigue en pie: por eso el reporte no llega solo hasta la base de datos, sino hasta una pantalla donde se lee (`/admin/reportes`), y quien puede entrar se decide con la variable `ADMIN_EMAILS`. Vacía significa nadie, y entonces el botón sí sería teatro: si despliegas esto y quieres moderar, hay que poner un correo ahí.

  Reportar **no bloquea** por su cuenta: quien reporta puede querer seguir viendo la conversación, por ejemplo como prueba de lo que está contando.

  El bloqueo cierra **las tres puertas**: la lista de gente, la ficha de la persona y la solicitud. Una que se escape basta para volver a tener delante a quien te querías quitar de encima. Y es **silencioso**: al bloqueado no se le avisa, y ni la ficha ni la solicitud dicen que hay un bloqueo, porque enterarse es justo lo que convierte un bloqueo en un motivo.

- **Se puede borrar la cuenta.** En la UE el derecho de supresión no es opcional, y aquí pesa más de lo normal por lo que se guarda: en qué gimnasio estás y a qué horas, que es dónde encontrarte. Se pide la contraseña —una sesión abierta en un ordenador prestado no debería bastar para borrarle la cuenta a alguien— y se borra todo en una transacción: perfil, horarios, marcas, entrenamientos, hitos, solicitudes, sesiones y mensajes.

  **También las conversaciones enteras**, y es la decisión incómoda del cambio. Conservar tus mensajes con el autor anonimizado dejaría escrito lo que escribiste después de pedir que te borraran; borrar solo los tuyos no se puede, porque cada mensaje apunta a las dos personas. Se borra el hilo, la otra persona pierde también lo suyo, y la pantalla lo dice antes de confirmar.

  Las claves ajenas están en `RESTRICT` a propósito: si aparece una tabla nueva que referencie al usuario y nadie la añade al borrado, éste falla en vez de dejar basura.

- **Hay una regla de contraseña, y vive en un solo sitio.** Antes no se comprobaba nada: se podía registrar una cuenta con la contraseña `a`. Ahora son doce caracteres mínimos y ninguna exigencia de mayúsculas ni símbolos, que no producen contraseñas mejores sino `Password1!` y un post-it.

---

## Lo que falta

- **La moderación se lee, pero no se gestiona.** Se puede reportar y los reportes se leen en `/admin/reportes`, así que la aplicación ya se entera de que alguien se ha portado mal con varias personas. Lo que no hay es qué hacer después: no se pueden marcar como revisados, ni suspender a nadie. Un panel que solo acumula deja de ser legible a los diez reportes. Y `ADMIN_EMAILS` es una lista de correos en una variable de entorno, no un sistema de permisos: es la única decisión de autorización de toda la API y está puesta ahí a sabiendas de que es temporal.


- **Chat sin indicador de escritura.** Los mensajes llegan al instante y se ve si el otro los ha leído, pero no si está escribiendo.
- **No se avisa de los mensajes por correo, solo de solicitudes y propuestas.** Es deliberado: un correo por cada mensaje de un chat es la forma más rápida de que alguien silencie el remitente, y entonces se pierden también los avisos que sí importaban. Lo que falta de verdad es un resumen —"tienes 3 mensajes sin leer"— y eso pide decidir cada cuánto, que es una decisión de producto, no de código.
- **Sin paginación.** Con decenas de usuarios sobra; con miles no.
- **`DisponibilidadController` expone un CRUD que nadie llama** — los horarios se gestionan dentro de `PUT /perfil`.
- **Accesibilidad, solo lo básico.** Está el equivalente textual de la rejilla, el foco atrapado en los diálogos y el contraste medido; falta pasarle un lector de pantalla de verdad y revisar el orden de tabulación pantalla por pantalla.
