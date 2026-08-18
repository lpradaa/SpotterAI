# ¿El motor ordena como ordena la gente?

Del motor está medido cuánto mueve cada peso y en qué se apoya. Lo que **no**
está medido es si acierta, y eso no se puede saber contando: hace falta que
alguien de fuera diga a cuál de dos candidatos elegiría.

Crear más usuarios en la base no sirve para esto. El motor ya se puntúa sobre
1.770 parejas sintéticas; más parejas son más preguntas, y lo que falta son
**respuestas**. Si las genera quien escribió el motor, la medición compara el
motor consigo mismo.

## Cómo se usa

**1. Generar el cuadernillo** (necesita la base con la demo sembrada):

```bash
cd backend && DB_HOST=localhost ./mvnw test -Dtest=GeneradorDeComparaciones
```

Escribe `comparaciones.html` aquí al lado: un fichero autocontenido que se abre
con doble clic, sin servidor y sin conexión. Se puede mandar por correo o por
WhatsApp tal cual.

**2. Repartirlo.** Cinco minutos por persona, sin registrarse ni instalar nada.
Al terminar, cada uno descarga un CSV y lo devuelve.

Con **ocho o diez personas** hay bastante. Y es importante que **varias
contesten el mismo cuadernillo**: sin eso no se puede saber cuánto se ponen de
acuerdo entre ellas, que es el techo de todo lo demás.

**3. Analizar:**

```bash
cd docs/juicios && python analizar.py respuestas-*.csv
```

## Lo que se mide, y en qué orden importa

1. **El acuerdo entre personas.** Va primero porque es el techo. Si dos
   anotadores eligen distinto en la mitad de los pares, exigirle al motor un
   90 % no tiene sentido: la pregunta no tiene respuesta clara.
2. **La coincidencia del motor con la mayoría humana**, comparada contra ese
   techo y no contra el 100 %.
3. **Dónde discrepa**, que es lo único accionable.

## Dos decisiones del diseño

**Comparaciones, no notas.** «Puntúa del 0 al 100 la compatibilidad» produce
ruido: cada persona calibra distinto. «¿Con cuál entrenarías?» produce una señal
ordinal limpia, y el motor lo que genera es un orden.

**Sin fotos, con avatar de iniciales.** Una foto haría que se juzgara también
por el aspecto — y el motor no ve fotos. Las discrepancias que salieran de ahí
no dirían nada sobre los pesos, que es lo que se quiere medir.

## Lo que enseñó la primera prueba

Se hizo un cuadernillo con un solo anotador —el autor— para depurar el
instrumento antes de repartirlo. No vale como validación y sirvió para lo que
tenía que servir: **encontró dos defectos**.

**Los empates contaban como fallo del motor.** Dos candidatos a 86 puntos: el
motor no tiene preferencia ahí, pero un `>=` lo hacía «elegir» al primero de la
lista, y cuando la persona elegía al otro aparecía como un error que el motor no
había cometido. Ahora los empates se apartan y se cuentan aparte.

**Y el peor, que era de diseño.** Los pares se elegían por diferencia de puntos,
y resultó que **8 de 14 tenían a los dos candidatos iguales en tres o más de los
cinco campos duros** — en dos de ellos, iguales en los cuatro primeros. Tiene
sentido mirándolo: dos candidatos empatan en puntuación sobre todo cuando se
parecen en todo.

Eso rompe el experimento sin que se note. Si los factores duros no pueden
desempatar, quien contesta decide con lo único que queda —la biografía— y la
medición ya no dice si el motor ordena bien, dice que ahí no tenía nada que
decir. El anotador lo notó solo: *«me he guiado más por las biografías que por
todo lo demás»*.

Ahora un par «cercano» solo entra si los candidatos difieren de verdad en algo
declarado.

## Lo que esto NO mide

Que el motor sea bueno para el producto. Compara su orden con el criterio de
unas cuantas personas mirando fichas, que no es lo mismo que quién acaba
entrenando con quién — para eso está el embudo, y ese necesita uso real.
