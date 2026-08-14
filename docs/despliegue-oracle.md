# Desplegar SpotterAI en Oracle Cloud, paso a paso

Guía de ejecución. El **qué** y el **por qué** están en [despliegue.md](despliegue.md);
esto es el **cómo**, con los botones concretos y los sitios donde la gente se
atasca.

Cuenta con **1-2 horas** si todo va bien, y con que el registro y la capacidad
ARM son las dos partes que pueden dar guerra. El resto es mecánico.

---

## Fase 0 · Antes de tocar nada

Cinco minutos aquí ahorran una tarde. El registro de Oracle tiene un antifraude
agresivo y **casi todos los «error al crear la cuenta» salen de esta lista**:

| Requisito | Por qué |
|---|---|
| **Correo nuevo, no usado antes en Oracle** | Es la causa número uno. Un intento fallido deja la dirección medio registrada y todos los reintentos con ella fallan igual, sin decir por qué. Usa una dirección distinta de verdad — un alias `+algo` de Gmail **no** sirve, Oracle los normaliza. |
| **Tarjeta física de banco, débito o crédito** | Las virtuales y las de prepago (Revolut desechable, N26 virtual, Wise) se rechazan mucho. Oracle hace una retención de ~1 € y la libera. |
| **Nombre y dirección exactamente como en el banco** | Un desajuste en el titular o el código postal tumba la verificación sin explicarlo. |
| **VPN apagada** | Una IP de VPN o de datacenter dispara el antifraude directamente. |
| **Ventana de incógnito, sin extensiones** | Las cookies de intentos anteriores arrastran estado a medias. Los bloqueadores rompen su formulario de pago. |

**Decide también tu región base**, porque es **permanente** y no se cambia
después. Desde España: *Spain Central (Madrid)* o *Germany Central (Frankfurt)*.
Frankfurt es más grande y suele tener más capacidad ARM libre — si te da igual la
latencia, es la apuesta más segura.

---

## Fase 1 · La cuenta

1. Ventana de incógnito → `cloud.oracle.com` → **Crea una cuenta**.
2. Rellena con los datos del banco, tal cual.
3. Elige la región base. **Piénsalo, es para siempre.**
4. Verificación por SMS, luego la tarjeta.

### Si vuelve a fallar

No reintentes con lo mismo esperando otro resultado. Cambia **una** cosa cada vez, en este orden:

1. **Correo distinto** (el que más resuelve, con diferencia)
2. **Tarjeta distinta** — otro banco, física
3. **Red distinta** — datos del móvil en vez de wifi, o al revés
4. **Navegador distinto**, incógnito

Si tras eso sigue igual, el antifraude te ha marcado y no se desbloquea solo:
escribe a su chat de soporte pidiendo revisión manual del registro. Suelen
contestar en 24-48 h.

> **Salida de emergencia**: si Oracle no cede, un VPS de Hetzner (~4 €/mes) corre
> el mismo `docker compose up` sin cambiar una línea, y el registro son cinco
> minutos. Todo lo de las fases 3 a 8 vale igual. Que no te bloquee el proyecto.

**Comprobación**: entras a la consola y ves el panel con tu región arriba a la derecha.

---

## Fase 2 · La máquina

**Compute → Instances → Create instance**

| Campo | Valor | Por qué |
|---|---|---|
| Image | **Ubuntu 22.04** | Docker se instala sin pelea. Oracle Linux también vale pero da más pasos. |
| Shape | **Ampere · VM.Standard.A1.Flex** | Es el ARM del Always Free. Las `E2.1.Micro` de AMD tienen 1 GB y **no te valen**. |
| OCPUs / RAM | **2 / 12 GB** | Ver abajo. |
| Boot volume | 50 GB | De sobra; el cupo gratuito llega a 200. |
| SSH keys | **Generate a key pair** → **descarga la privada** | Si no la descargas, no entras. No hay segunda oportunidad. |

### El problema de la capacidad ARM

Vas a ver `Out of host capacity`. **No es culpa tuya**: el cupo gratuito de
Ampere está muy solicitado. Por orden de efectividad:

1. **Pide menos.** Con **1 OCPU / 6 GB** encuentras hueco mucho antes, y a esta
   app le sobra: en marcha consume unos 500 MB entre las tres piezas. Lo único
   que sufre es la primera compilación, que pasa de ~12 a ~25 minutos. Se
   amplía después sin recrear la máquina.
2. **Cambia de Availability Domain** (AD-1, AD-2, AD-3) si tu región tiene más
   de uno. Madrid tiene uno solo; Frankfurt, tres.
3. **Prueba a otras horas.** De madrugada europea se libera capacidad.
4. **Pasa la cuenta a Pay As You Go.** Las cuentas de pago tienen prioridad de
   capacidad, y **los recursos Always Free siguen siendo gratis** dentro de sus
   límites. Es el truco que más funciona, pero con cuidado: si te pasas de 4
   OCPU ARM, 24 GB o 200 GB de disco, **eso sí se cobra**. Si lo haces, no
   toques nada fuera de lo que dice esta guía.

**Comprobación**: la instancia en estado **Running** y apuntada su **IP pública**.

---

## Fase 3 · Entrar por SSH

Desde tu portátil (PowerShell ya trae `ssh`):

```bash
ssh -i C:\ruta\a\tu-clave.key ubuntu@TU_IP_PUBLICA
```

### Si Windows se queja de los permisos de la clave

Es lo normal. Windows la deja legible por todo el mundo y `ssh` se niega:

```bash
icacls "C:\ruta\a\tu-clave.key" /inheritance:r /grant:r "%USERNAME%:R"
```

### Si se queda colgado sin responder

No es la clave: es la Security List de la fase siguiente, que aún no permite ni
el 22. Oracle suele abrirlo solo al crear la instancia; si no, añade la regla del
puerto 22 igual que las de la fase 4.

**Comprobación**: ves el prompt `ubuntu@...:~$`.

---

## Fase 4 · Los dos cortafuegos

**Aquí se atasca casi todo el mundo.** Oracle tiene **dos capas independientes** y
hay que abrir las dos. Si solo abres una, el contenedor arranca perfectamente y
la web no responde — y parece un fallo de la aplicación cuando no lo es.

### 4a · La red virtual (en la consola web)

**Networking → Virtual Cloud Networks → tu VCN → Subnets → la subnet →
Security Lists → Default Security List → Add Ingress Rules**

Dos reglas:

| Source CIDR | IP Protocol | Destination Port Range |
|---|---|---|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |

### 4b · El cortafuegos de la máquina (por SSH)

Las imágenes de Oracle traen `iptables` cerrado **aunque hayas abierto la
Security List**:

```bash
sudo iptables -I INPUT 1 -p tcp --dport 80 -j ACCEPT
```

```bash
sudo iptables -I INPUT 1 -p tcp --dport 443 -j ACCEPT
```

> Se inserta en la **posición 1** a propósito. La receta que circula por ahí usa
> la 6, que es donde suelen acabar las reglas de conexiones ya establecidas —
> pero la posición exacta cambia según la imagen, y si el `REJECT` final queda
> por delante, la regla no sirve de nada y el síntoma es idéntico a no haberla
> puesto. En la 1 va antes que todo, siempre.

Y que sobrevivan al reinicio:

```bash
sudo apt-get update && sudo apt-get install -y iptables-persistent
```

```bash
sudo netfilter-persistent save
```

**Comprobación**: `sudo iptables -L INPUT -n --line-numbers | head` y ver los
`ACCEPT` de 80 y 443 **arriba del todo**, antes de cualquier `REJECT`.

---

## Fase 5 · Docker

```bash
sudo apt-get update && sudo apt-get install -y ca-certificates curl git
```

```bash
curl -fsSL https://get.docker.com | sudo sh
```

```bash
sudo usermod -aG docker ubuntu && newgrp docker
```

**Comprobación**: `docker compose version` responde sin `sudo`. Si pide permisos,
sal de la sesión SSH y vuelve a entrar.

---

## Fase 6 · Desplegar

```bash
git clone https://github.com/lpradaa/SpotterAI.git && cd SpotterAI
```

Crea el `.env` con un secreto de sesión de verdad:

```bash
cp .env.example .env && echo "JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')" >> .env
```

Edita el resto con `nano .env`:

```
DB_PASSWORD=una-contraseña-larga-tuya
ADMIN_EMAILS=tu@correo.com
EMBEDDINGS_URL=
CORREO_URL_BASE=http://TU_IP_PUBLICA
COOKIE_SEGURA=false
```

> `EMBEDDINGS_URL` **vacío** es lo correcto: el servicio de modelo ocupa 756 MB y
> no se despliega. La gente de demostración lleva sus vectores ya calculados, así
> que el factor semántico funciona igual. El detalle, en [despliegue.md](despliegue.md).
>
> `COOKIE_SEGURA=false` **de momento**, porque todavía no hay HTTPS. Se cambia en
> la fase 8, junto con `CORREO_URL_BASE`.

Arranca. **La primera vez tarda 12-25 minutos** según los núcleos: compila
Angular y Maven dentro de la máquina.

```bash
docker compose up -d --build
```

Para mirar cómo va:

```bash
docker compose logs -f
```

---

## Fase 7 · Comprobar que funciona

```bash
docker compose ps
```

Las tres piezas en `running` y `base` en `healthy`.

```bash
docker compose logs backend | grep -i "repaso de vectores"
```

Si no dice nada, perfecto: significa que **ninguna biografía se quedó sin
vector**, o sea que los sembrados entraron.

```bash
curl -s http://localhost/api/gimnasios | head -c 200
```

Y ahora desde tu navegador, `http://TU_IP_PUBLICA`:

1. Entra con **`demo@spotterai.test`** / **`Demo1234`**
2. Abre la ficha de **Marta Ibáñez**
3. Despliega **«Ver de dónde sale este %»**

**Si aparece «Lo que contáis de vosotros» con puntuación, el factor semántico
está vivo en producción.** Ese es el momento en que esto deja de ser un proyecto
local.

### Si la web no carga desde fuera pero sí desde dentro

Es la fase 4, siempre. Vuelve y comprueba **las dos** capas.

---

## Fase 8 · HTTPS

Sin esto, la galleta de sesión viaja legible por cualquier red. Hace falta un
dominio: **DuckDNS es gratis** (`tuapp.duckdns.org` apuntando a tu IP) y sirve
perfectamente.

Con el dominio listo, **Caddy** saca y renueva el certificado solo:

```bash
export DOMINIO=tuapp.duckdns.org
```

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

Y cambia en `.env`, **las dos a la vez**:

```
COOKIE_SEGURA=true
CORREO_URL_BASE=https://tuapp.duckdns.org
```

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

**Comprobación**: `https://tuapp.duckdns.org` con candado, y volver a entrar en
la aplicación. Si la sesión no se mantiene, `COOKIE_SEGURA` está en `true` sin
HTTPS delante de verdad.

---

## Mantenimiento

```bash
git pull && docker compose up -d --build      # desplegar cambios
docker compose logs -f backend                 # ver el registro
docker compose down                            # parar (los datos sobreviven)
```

Los datos viven en volúmenes de Docker, así que `down` y `up` no borran nada.
Lo que sí borra es `docker compose down -v`.

> **Un aviso sobre el Always Free**: Oracle se reserva reclamar instancias
> gratuitas que lleven mucho tiempo inactivas. Entrar de vez en cuando, o tener
> la aplicación recibiendo visitas, basta para que no la toquen.
