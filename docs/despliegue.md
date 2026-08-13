# Desplegar SpotterAI

El objetivo de este documento es un enlace que alguien pueda abrir, **por 0 €**.

## Qué se despliega y qué no

| Pieza | Va | Memoria medida |
|---|---|---|
| Frontend (Angular + nginx) | sí | despreciable — 717 KB compilados |
| Backend (Spring Boot) | sí | **337 MB** |
| Base de datos (MariaDB) | sí | ~120 MB |
| **Servicio de embeddings** | **no** | **756 MB** |

El servicio de embeddings se queda fuera a propósito: 756 MB no caben en ninguna
capa gratuita, que van de 512. La aplicación está construida para funcionar sin
él —`EMBEDDINGS_URL` vacío y el noveno factor del motor queda «sin datos», que la
calculadora ya sabe repartir— y la gente de demostración se siembra con **sus
vectores ya calculados** (`backend/src/main/resources/demo/vectores-biografia.tsv`),
así que el factor semántico se ve funcionando.

**La consecuencia, dicha claramente:** en la instancia desplegada, quien escriba
una biografía *nueva* no tendrá vector. Su factor de afinidad saldrá «sin datos»
hasta que alguien levante el servicio de embeddings contra esa base y deje que el
repaso de arranque los calcule.

Quitar esa limitación es cuantizar el modelo a ONNX int8 (~120 MB en disco,
~250-300 MB en memoria), que entonces sí cabe en una capa gratuita. Está sin
hacer.

## Forma del despliegue

`docker-compose.yml` levanta las tres piezas y nginx hace de proxy, así que el
frontend compilado no lleva dentro ninguna dirección. Eso hace que **el
despliegue natural sea una sola máquina que corra Docker Compose**, no tres
servicios sueltos.

### Recomendado: Oracle Cloud Always Free

4 núcleos ARM y 24 GB de RAM, gratis de forma indefinida. Piden tarjeta para
verificar, no cobran. Dos avisos honestos: la capacidad ARM se agota a ratos en
algunas regiones, y conviene no dejar la instancia parada meses.

```bash
# En la máquina, una vez instalado Docker:
git clone https://github.com/lpradaa/SpotterAI.git && cd SpotterAI

cp .env.example .env
# Editar .env — como mínimo:
#   JWT_SECRET      cadena larga y aleatoria, NUNCA la del ejemplo
#   DB_PASSWORD     contraseña propia
#   EMBEDDINGS_URL  vacío (sin servicio de modelo en producción)
#   ADMIN_EMAILS    tu correo, si quieres ver /admin/reportes

docker compose up -d
```

Abrir el puerto 80 en la lista de seguridad de Oracle **y** en el cortafuegos de
la propia máquina (Oracle Linux trae `iptables` cerrado por defecto, y es el
motivo más común de «el contenedor arranca pero no responde»).

### Alternativas

Cualquier proveedor que ejecute Docker Compose en una máquina sirve: Hetzner
(~4 €/mes), una Raspberry Pi propia, un VPS que ya tengas. Render, Railway y Fly
también valen, pero despliegan servicios sueltos: hay que sustituir el
`proxy_pass http://backend:8080` de `frontend/nginx.conf` por la URL interna que
dé la plataforma, y buscar una base MySQL/MariaDB gratuita — que es lo escaso,
porque casi todas las capas gratuitas ofrecen PostgreSQL.

## Variables que hay que poner sí o sí

| Variable | Por qué |
|---|---|
| `JWT_SECRET` | Firma las sesiones. Con el valor del ejemplo, cualquiera que lea el repositorio puede fabricarse una sesión válida. |
| `DB_PASSWORD` | Contraseña de la base. |
| `COOKIE_SEGURA=true` | En `false` la galleta de sesión viaja legible por cualquier red por la que pase. Requiere HTTPS delante. |
| `CORREO_URL_BASE` | Si se queda en `localhost`, los enlaces de recuperar contraseña no le sirven a nadie. |

La siembra no hay que activarla: `SPRING_PROFILES` ya vale `demo` por defecto en
el compose. Conviene revisar además `ADMIN_EMAILS` (quién ve `/admin/reportes`;
vacío significa que nadie) y `CORREO_ACTIVO` (en `false` el enlace de recuperar
contraseña se escribe en el registro en vez de enviarse, que es lo correcto sin
servidor de correo).

## Comprobar que ha salido bien

```bash
curl -s http://TU_IP/api/gimnasios | head -c 200          # el backend responde
docker compose logs backend | grep -i "repaso de vectores" # 0 sin vector: bien
```

Entrar con `demo@spotterai.test` / `Demo1234`, abrir la ficha de Marta Ibáñez y
desplegar «Ver de dónde sale este %». Si aparece **«Lo que contáis de vosotros»**
con puntuación, los vectores sembrados han entrado y el factor semántico está
vivo.

## Cuando se cambie una biografía de demostración

Los vectores del fichero dejan de corresponder al texto. Hay una prueba que lo
detecta, y regenerarlos es:

```bash
cd embeddings && uvicorn servidor:app --port 8000    # levantar el modelo
# arrancar el backend con EMBEDDINGS_URL=http://localhost:8000 y dejar
# que el repaso de arranque recalcule, y después volcar:
mysql -u root spotterai_db -N -B -e \
  "SELECT email, REPLACE(TO_BASE64(biografia_vector),'\n','') \
   FROM usuario WHERE biografia_vector IS NOT NULL ORDER BY email;" \
  > backend/src/main/resources/demo/vectores-biografia.tsv
```
