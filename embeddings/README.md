# Servicio de embeddings

Convierte una biografía en un vector de 384 números. Es lo único del proyecto
que ejecuta un modelo.

## Por qué está aparte del backend

El embedding se calcula **al guardar un perfil**, no al emparejar. Emparejar es
un producto escalar sobre vectores ya guardados, así que este servicio no está
en el camino crítico de nada: si se cae, el motor sigue calculando
compatibilidades a la misma velocidad y solo deja de recalcular las biografías
que cambien mientras tanto. La calculadora trata esa ausencia como cualquier
otro dato que falta —igual que un perfil sin gimnasio— y reparte el peso del
factor entre los demás.

Meter el modelo dentro del Spring Boot habría obligado a dimensionar el backend
entero por el transformer, para una operación que ocurre cuando alguien edita su
perfil.

## Por qué no usa sentence-transformers

Porque arrastra PyTorch, y PyTorch pesa más que el modelo. Medido en este
proyecto, **antes de cargar ningún peso**:

| | RSS |
|---|---|
| python + numpy + onnxruntime + tokenizers | **51 MB** |
| … + torch | 219 MB |
| … + transformers | 284 MB |
| … + sentence-transformers | **405 MB** |

Con el modelo cargado, el servicio se plantaba en **740 MB**.

Cuantizar a int8 por sí solo no lo arreglaba: bajaba los pesos de 470 MB a 113,
pero el proceso subía a **805 MB**, porque cargaba las dos pilas a la vez. Y
quitar PyTorch por sí solo tampoco: con el modelo en float32 sobre ONNX Runtime
son **789 MB**, porque reserva memoria con más holgura.

Solo funciona la combinación: la inferencia escrita a mano en `modelo_ligero.py`
sobre el modelo int8 deja el servicio en **475 MB**, un 36 % menos, y quita ~2 GB
de dependencias de la imagen.

`verificar_ligero.py` comprueba que la inferencia manual reproduce
`sentence-transformers` con **0.000000 de desviación** sobre el mismo modelo. Lo
que cambia los números es la cuantización, no la implementación — y cuánto,
lo mide `calibracion/comparar_cuantizado.py`.

## Arrancarlo

```bash
python -m venv .venv && .venv/Scripts/activate   # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
uvicorn servidor:app --port 8000
```

La primera arrancada descarga el modelo (~470 MB) y tarda. Las siguientes leen
la caché de `~/.cache/huggingface`.

## Probarlo

```bash
curl -s localhost:8000/salud
curl -s localhost:8000/vector -H "Content-Type: application/json" \
  -d '{"texto":"Todavía me da respeto la zona de peso libre"}'
```

## Qué espera el backend

`EMBEDDINGS_URL` apuntando aquí (por defecto `http://localhost:8000`). Si la
variable está vacía el backend no llama a nadie y el factor semántico queda
permanentemente sin datos — que es el comportamiento correcto en un entorno
donde este servicio no está desplegado.


---

## Y después se pagaron 127 MB de vuelta

Todo lo de arriba sigue siendo verdad y ya no es toda la historia. El factor para
el que se optimizó este servicio —comparar dos biografías con la similitud del
coseno— resultó **medir parecido de redacción y no compatibilidad**: dos personas
que quieren lo contrario dicho con la misma estructura sacaban 0,843 y dos que
quieren lo mismo dicho con sus palabras, 0,499.

No se arreglaba con otro modelo de la misma clase. Se probó `multilingual-e5-small`
y, medido como fracción de su propio rango, sale **peor**: el fallo es
estructural, porque un bi-encoder proyecta cada texto por separado y la oposición
entre dos frases no es propiedad de ninguna de las dos.

Lo que sí lo arregla es dejar de comparar textos y **leer cada biografía por
separado** con un modelo de inferencia textual, preguntándole tres cosas
concretas. Eso cabe aquí porque el modelo lee a una persona, no compara a dos:
sigue corriendo una vez al guardar el perfil y fuera del camino crítico.

| | RSS |
|---|---|
| base (python + onnxruntime + tokenizers) | 18,6 MB |
| el de embeddings | 483,9 MB |
| **el de intenciones (mDeBERTa-xnli int8)** | **610,9 MB** |

**El servicio pasa de 484 a 611 MB.** Desactivar la arena de ONNX Runtime solo
devuelve 7, así que no hay palanca fácil, y esos 611 rompen el objetivo de 512 que
motivó quitar PyTorch.

Se paga a sabiendas y por dos razones. La primera es que aquellos 512 eran de
**portabilidad** —caber en cualquier capa gratuita— y el despliegue que documenta
`docs/despliegue-oracle.md` es ARM Always Free con 24 GB. La segunda es más
simple: un factor que ordena al revés de lo que dice ordenar no vale 127 MB menos,
vale cero.

Los dos modelos se cargan **de forma perezosa** y por eso no se suman: desde que
el factor se calcula por ejes nadie llama a `/vector`, y cargar los dos a la vez
sería pagar 1,1 GB para siempre por atender el último repaso de una migración.

El recorrido entero —las tres cosas que se probaron y las dos que fallaron— está
en [`docs/medir-el-motor.md`](../docs/medir-el-motor.md).
