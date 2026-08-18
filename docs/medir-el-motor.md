# Medir el motor

El motor reparte 100 puntos entre nueve factores y cada peso está razonado en
`CalculadoraCompatibilidad`: por qué el horario vale 40 y la edad 5. Razonar no
es medir. Esto es lo segundo.

## Qué es esto y qué no es

**No valida que el motor acierte.** Validar sería comprobar que la gente con
puntuación alta acaba entrenando junta, y para eso está el embudo de `/embudo`,
que hoy dice —correctamente— que hacen falta 20 solicitudes por tramo y todavía
no hay. Inventar usuarios y llamar a eso validación sería el mismo teatro que se
evitó al no poner un botón de denunciar sin nadie detrás.

**Mide el motor contra sí mismo**: cuánto mueve cada factor las decisiones, en
qué se apoya de verdad y si algún peso está puesto donde no cambia nada. Eso no
necesita usuarios reales; necesita una población variada y bien construida.

La métrica no son los puntos sino las **decisiones cambiadas**: cuántas parejas
cambian de tramo —«muy compatibles» frente a «buena compatibilidad»— y cuántas
cambian la frase que se enseña debajo del número. La diferencia entre un 68 y un
71 no la ve nadie; la que hay entre dos tramos cambia lo que la pantalla dice.

Es la misma metodología con la que se calibró la cuantización del modelo: lo que
importa no es cuánto se mueve un vector, es cuánto se mueve lo que la persona
lee.

## Cómo reproducirlo

```bash
cd backend
./mvnw test -Dtest=SensibilidadDelMotorTest   # qué mueve cada peso
./mvnw test -Dtest=AportaLaAfinidadTest       # qué aporta el noveno factor
./mvnw test -Dtest=LasDosViasDelGimnasioTest  # de dónde sale la influencia del gimnasio
```

Y la evaluación del noveno factor, que no es un test de Java sino un script
contra el modelo real:

```bash
cd embeddings && .venv/Scripts/python calibracion/evaluar_afinidad.py
```

`SensibilidadDelMotorTest` imprime dos tablas: la de siempre y la misma con la
gente entrenando a las horas de verdad. Compararlas es lo que dice si un número
depende del supuesto de horarios o no.

Los dos imprimen su informe. La población es determinista —semilla `20260817` en
`BancoDePerfiles`— porque unos números que cambian en cada ejecución no se pueden
citar en ningún sitio.

## Antes de los resultados: dos veces que el instrumento estuvo mal

Las dos salieron a la luz al mirar los números, no al leer el código, y las dos
quedan como prueba para que no vuelvan.

**1. La población tenía gente que no existe.** La primera versión generaba
perfiles sin horarios, sin gimnasio, sin nivel y sin edad «para ejercitar la
redistribución de pesos», y el análisis salió con un 41 % de parejas sin horario
que cruzar. Pero `PerfilMinimo` exige esos seis campos y el guard no deja pasar a
Explorar sin ellos: **nadie llega a ser puntuado sin los seis**. Se estaba
midiendo un motor que atiende a una población que la aplicación no permite.

Lo que sí falta de verdad son los tres que `PerfilMinimo` deja opcionales a
propósito: las marcas, la constancia y la biografía. Y eso resultó ser justo el
resultado interesante.

**2. Las dos poblaciones dejaron de ser la misma gente.** Al hacer la biografía
opcional, el sorteo quedó dentro de un cortocircuito:

```java
if (conBiografia && azar.nextInt(5) != 0) { … }   // mal
```

Con `conBiografia = false` la llamada no se ejecuta, la secuencia de números
aleatorios se desplaza y las dos poblaciones pasan a ser dos grupos distintos.
La medición dio entonces que **un factor de 6 puntos movía 54 y cambiaba el 77 %
de las frases** — imposible, y por eso se vio.

El arreglo es sortear siempre y decidir después. Y de ahí sale la prueba que más
vale de todo esto: `elExperimentoSeSostiene` comprueba que quitar un factor de 6
puntos no mueve la puntuación más de 6. **Valida el instrumento, no el motor.**

## Resultado 1: en qué se apoya el motor

60 perfiles, 1.770 parejas. Se anula un factor cada vez y se cuenta cuántas
parejas cambian de tramo.

| factor | peso | mueve | sin datos | rinde |
|---|---|---|---|---|
| horario | 40 | 56,5 % | 0,0 % | 1,41 |
| objetivo | 12 | 14,7 % | 0,0 % | 1,23 |
| nivel | 10 | 11,2 % | 0,0 % | 1,12 |
| gimnasio | 8 | 11,1 % | 0,0 % | 1,38 |
| **constancia** | **10** | **6,6 %** | **30,8 %** | **0,66** |
| **fuerza** | **10** | **6,3 %** | **77,5 %** | **0,63** |
| edad | 5 | 4,6 % | 0,0 % | 0,93 |
| rutina | 5 | 4,4 % | 0,0 % | 0,88 |

- **mueve**: parejas que cambian de tramo si se anula el factor.
- **sin datos**: parejas en las que el factor no se puede evaluar.
- **rinde**: cuánto mueve por cada punto de peso.

**El reparto hace lo que dice.** El horario decide más de la mitad de los tramos,
que es lo que corresponde a un factor que no es una preferencia sino una
restricción: si dos personas no coinciden, no entrenan juntas y ningún otro
factor arregla eso.

**El gimnasio rinde muy por encima de su peso** (1,38 con 8 puntos, casi como el
horario). Por qué, tiene sección propia más abajo: la primera explicación que se
le dio a ese número era razonable y resultó ser falsa.

## Resultado 2: los dos factores que peor rinden son los dos que no se exigen

Esta es la conclusión del análisis, y no se ve en ninguna parte del código.

Seis de los ocho factores tienen **0 % sin datos**: son los que `PerfilMinimo`
exige, así que están siempre. Los dos que faltan alguna vez son la constancia
(30,8 %) y la fuerza (77,5 %) — y son exactamente **los dos que peor rinden**
(0,66 y 0,63, la mitad que el resto).

No es casualidad ni un reparto mal calibrado: **es el precio de no obligar a
rellenarlos**, y ese precio ahora tiene un número.

La fuerza es el caso extremo, y por un motivo estructural. Los demás factores
solo necesitan que las dos personas hayan rellenado un campo; este necesita algo
más fuerte: que hayan apuntado marcas **del mismo ejercicio**. Dos personas con
tres marcas cada una pueden no compartir ninguna.

**Y no se arregla subiéndole el peso.** Subir el peso de algo que no se puede
evaluar no lo hace más influyente: cuando falta, sus puntos se reparten entre los
demás igualmente. Lo que lo arregla es que haya más coincidencia — y eso tenía
dos caminos, así que se midieron los dos antes de elegir.

Lo que **no** hay que hacer es exigir las marcas en el perfil mínimo.
`PerfilMinimo` explica por qué: exigirlas produciría números inventados por quien
no los sabe, y entrarían derechos al factor del que depende el nombre del
producto. Un dato ausente al menos sabe que lo está.

> **El 77,5 % depende de esta población**, donde los ejercicios se reparten al
> azar entre seis. En la realidad la gente apunta los básicos y coincidirá más.
> Lo que no depende de la población es el mecanismo, y eso es lo que fija
> `laFuerzaEsElQueMenosDatosTiene`.

## Decisión: qué se hizo con eso

Dos caminos para subir la cobertura, medidos con el mismo instrumento
(`SubirLaCoberturaDeLaFuerzaTest`):

| | cobertura | qué cuesta |
|---|---|---|
| hoy | 22,5 % | — |
| **A. sugerir los básicos** | **30,5 %** | nada: sigue comparando el mismo ejercicio |
| B. comparar por patrón | 35,2 % | **cambia el veredicto en el 34,3 %** |

**B sube más cobertura y por eso está descartada.** Agrupar por patrón —banca
con militar, sentadilla con peso muerto— permite comparar a más parejas, pero
compara números que no son comparables: en peso muerto se levanta bastante más
que en sentadilla, así que el ratio sale bajo aunque las dos personas sean
igual de fuertes.

Medido donde se puede saber la verdad —las 399 parejas que comparten ejercicio
exacto— la versión por patrón **diría lo contrario en una de cada tres**. Para
un factor cuya salida es «podéis cubriros», que es el nombre del producto, eso
no es ganar cobertura: es mentir más a menudo.

**A está implementada.** `Ejercicio` marca los tres básicos, el backend los
sirve primero y el formulario propone el siguiente que falte al añadir una
marca. Se sugiere el ejercicio y **nunca el peso**: rellenar un número por
alguien sería inventarle un dato al factor, que es justo lo que `PerfilMinimo`
evita al no exigir las marcas. Y se puede cambiar — quien entrene otra cosa la
apunta igual.

## Resultado 3: qué aporta el noveno factor

La afinidad de lo escrito es el único factor que necesita un modelo levantado,
un servicio aparte y 475 MB de memoria. Eso obliga a hacerle una pregunta que a
los otros ocho no: **¿cuántas decisiones cambia?**

Se mide con la misma población dos veces, con biografía y sin ella. Sin
biografía el factor se queda sin datos y sus 6 puntos se reparten entre los
demás, que es exactamente lo que pasaría si se quitara.

| | |
|---|---|
| diferencia media | 1,31 puntos |
| diferencia máxima | 6 puntos |
| cambian de tramo | 4,6 % |
| cambian de frase | 3,6 % |

**Hace exactamente lo que pretendía hacer.** El código dice que seis puntos
bastan «para mover una decisión en el margen» y no para mandar sobre datos duros:
mueve un punto de media y cambia la decisión en una de cada veintidós parejas. Ni
es un adorno —si no cambiara nada, el servicio de embeddings no se pagaría solo—
ni se le va de las manos.

La diferencia máxima es 6, que es su peso exacto. Ese número es también la
comprobación de que el experimento está bien montado.

Y cuando decide, **sale en la explicación**: hay parejas en las que la afinidad
es el factor dominante, así que el número movido tiene una frase que lo cuenta.
Un factor que moviera la puntuación sin aparecer nunca en el texto sería
exactamente lo contrario de la tesis del producto.

## Resultado 4: el gimnasio influye por dos caminos, y no por el que se dijo

Aquí arriba, en la primera versión de esta página, ponía esto:

> El gimnasio rinde muy por encima de su peso. No es un error de la tabla: además
> de sus puntos, compartir gimnasio cuadruplica lo que cuenta el solape horario.

Es una explicación razonable, encaja con el número y **es falsa**. Se quedó
escrita porque nadie la midió: el análisis de sensibilidad anula factores bajando
su peso, y el descuento del horario no es un peso, son constantes de
`CalculadoraCompatibilidad`. Así que el 11,1 % de la tabla era una sola de las dos
vías, y la frase que lo explicaba hablaba de la otra.

Las dos vías son:

- **A, los puntos.** Compartir sala da 8 de 100.
- **B, el descuento del horario.** Con salas distintas el solape cuenta una cuarta
  parte —0,60 si alguien se desplaza— porque coincidir a las seis en dos
  edificios distintos no es coincidir.

**Se separan sin tocar el motor.** No hace falta hacer configurable el descuento:
con el peso del gimnasio ya a cero, poner a toda la población en la misma sala
deja el multiplicador en 1 y no cambia nada más. Es el mismo truco que se usó con
la afinidad — quitarle los datos al factor en vez de quitarlo del código.

| vía | tramos | frases | dif. media |
|---|---|---|---|
| A · los 8 puntos | **11,1 %** | 15,4 % | 3,19 |
| B · el descuento del horario | **2,5 %** | 4,6 % | 0,67 |
| A+B · el gimnasio entero | 13,6 % | 20,0 % | 3,85 |

**Sale al revés de lo que decía la explicación**, y el motivo es que una base
grande no sirve de nada si casi siempre vale cero:

| | |
|---|---|
| parejas que coinciden en algún horario | 16,9 % |
| …y además en salas distintas, que es donde B puede actuar | **10,1 %** |

Multiplicar cero por 0,25 sigue dando cero. En las nueve de cada diez parejas
restantes, compartir sala o no **da exactamente igual por esta vía**.

Visto de otra forma, B no es débil: donde puede actuar cambia una decisión de
cada cuatro. Lo que pasa es que casi nunca puede — **y ahí estaba la trampa**.

### Segunda vuelta: ese 16,9 % era el problema

«Casi nunca puede» es una afirmación sobre esta población, no sobre el producto.
Y en cuanto se mira, el 16,9 % no se sostiene: el banco reparte las horas de
entrenamiento **uniformemente** entre las 7 y las 19, y nadie entrena a horas
repartidas.

La única referencia que existe son los quince perfiles de la demostración,
escritos a mano pensando en personas concretas. Diecisiete de sus treinta y una
franjas empiezan a las 18:00, y los días se apilan en lunes y miércoles:

| | franjas por persona | parejas que coinciden |
|---|---|---|
| banco, horas uniformes | 2,5 | 16,9 % |
| **demostración, a mano** | **2,2** | **50,5 %** |

No es que el banco tenga poca disponibilidad: **tiene incluso más franjas por
persona**. Es que las pone donde no va nadie.

Ese 50,5 % sale de la base con la demostración sembrada, y se puede recomprobar:

```sql
SELECT COUNT(DISTINCT CONCAT(a.usuario_id,'-',b.usuario_id)) AS coinciden
FROM disponibilidad a JOIN disponibilidad b
  ON a.usuario_id < b.usuario_id
 AND a.dia_semana = b.dia_semana
 AND a.hora_inicio < b.hora_fin AND b.hora_inicio < a.hora_fin;
-- 46 de las 91 parejas que forman los 14 perfiles con horario
```

Repitiendo la medición entera sobre una población apilada en las horas de
siempre —44,4 % de coincidencia, cerca del 50,5 % de referencia—:

| vía | horas uniformes | horas punta |
|---|---|---|
| A · los 8 puntos | 11,1 % | 13,1 % |
| **B · el descuento del horario** | **2,5 %** | **8,3 %** |
| A+B · el gimnasio entero | 13,6 % | **21,2 %** |

B se multiplica por 3,3 mientras A apenas se mueve. Y en **frases** —el texto que
se enseña debajo del número— B pasa a 13,5 % y A a 11,4 %: en una población con
horarios realistas, **el descuento decide más texto que los puntos**.

Así que la explicación original no era falsa: era falsa *en el banco*. Y el banco
tenía un sesgo que nadie había mirado porque nada dependía de él… hasta que algo
dependió.

### Lo que hay que sacar de aquí

Que la lección no es «mide antes de explicar». Esa ya estaba. Es la siguiente:
**una medición hereda entera la población sobre la que se hizo**, y hay que saber
de qué supuestos depende cada número antes de citarlo.

Por eso la tabla completa se mide ahora en los dos repartos, y la respuesta es
tranquilizadora: el orden no cambia y las conclusiones aguantan.

| factor | uniforme | horas punta | sin datos |
|---|---|---|---|
| horario | 56,5 % | 49,3 % | 0 % → 0 % |
| objetivo | 14,7 % | 15,4 % | 0 % → 0 % |
| nivel | 11,2 % | 12,9 % | 0 % → 0 % |
| gimnasio | 11,1 % | 13,1 % | 0 % → 0 % |
| constancia | 6,6 % | 8,3 % | 30,8 % → 30,8 % |
| fuerza | 6,3 % | 7,8 % | **77,5 % → 77,5 %** |
| edad | 4,6 % | 5,9 % | 0 % → 0 % |
| rutina | 4,4 % | 5,0 % | 0 % → 0 % |

El horario sigue mandando de largo, la fuerza sigue siendo la de menos cobertura
—y su 77,5 % **no se mueve ni una décima**, porque depende de qué ejercicios
apunta la gente y no de a qué hora entrena— y ningún factor cambia de sitio. El
único resultado sensible al supuesto era el de la vía B, que es justo el que lo
tenía dentro.

**El reparto uniforme se queda como el de por defecto.** No porque sea el bueno
—quince perfiles a mano no son una muestra y no autorizan a decir cuál lo es—
sino porque es el conservador: mide el motor en un mundo donde la gente casi no
coincide, y un motor que se sostiene ahí se sostiene con horarios apretados. Lo
que cambia es que ahora está escrito de qué depende cada número.

### Y de paso, lo que cuesta preguntar «¿puedes desplazarte?»

La misma medición contesta a algo que llevaba tiempo sin comprobarse. Esa casilla
del perfil sube el descuento de 0,25 a 0,60, y es una pregunta más en un
formulario que ya es largo:

| | tramos | frases | dif. media |
|---|---|---|---|
| que nadie pudiera marcarla | 0,3 % | 0,5 % | 0,10 |

Cinco parejas de 1.770. Cuelga de la vía B, así que hereda su problema —solo
cuenta donde hay solape y salas distintas— y encima se aplica al 24,4 % de las
parejas que están en ese caso.

**No se quita**, y la razón no es el número: la casilla existe porque la
alternativa era aplicar una media a todo el mundo, y quien está dispuesto a coger
el metro puntuaba igual que quien no piensa moverse. Eso sigue siendo verdad para
esas cinco parejas, y para ellas la decisión cambia entera. Lo que este número
dice es otra cosa: **que no se espere de ahí ninguna mejora agregada**, y que si
algún día hay que acortar el formulario, esta es la primera candidata y ya se
sabe lo que cuesta.

### Qué se fija como prueba y qué no

`LasDosViasDelGimnasioTest` deja fijado que las dos vías hacen algo y que quitar
las dos mueve al menos tanto como quitar una. **No fija que A mueva más que B**,
y eso resultó ser una decisión afortunada: se escribió diciendo «con horarios más
apretados podría darse la vuelta», y media hora después se dio.

Es la diferencia entre una propiedad y un resultado. Una propiedad aguanta un
cambio de población; un resultado, no. Ponerlo como prueba habría dejado la suite
en rojo al medir el reparto realista, y con razón.

Y valida el instrumento por partida doble, que es la lección de este documento:
que las tres poblaciones son la misma gente con lo único distinto que debe serlo,
y que anular un factor de 8 puntos no mueve la puntuación más de 8.

## Resultado 5: el noveno factor no ordena por lo que dice ordenar

Del factor semántico se sabía cuánto mueve —un punto de media, una decisión de
cada veintidós— y **no se sabía si acierta**. Son dos preguntas distintas: un
factor puede mover decisiones y moverlas mal.

No hacen falta usuarios para responderla. Bastan pares cuya relación se conoce
por construcción, pasados por el modelo real que sirve la aplicación
(`embeddings/calibracion/evaluar_afinidad.py`):

| grupo | qué es | debería |
|---|---|---|
| PARÁFRASIS | la misma intención con otras palabras | alto |
| CONTRASTE | intenciones incompatibles | bajo |
| INVERTIDO | casi el mismo texto, intención opuesta | bajo |
| CAMBIO_MIN | una palabra cambiada, sin negar | bajo |
| REAL_COMPAT | dos biografías largas compatibles, escritas distinto | alto |
| REAL_INCOMP | dos biografías largas opuestas, escritas parecido | bajo |
| FONDO | dos textos del dominio sin relación | el suelo |

### El resultado

| grupo | coseno | puntos de 6 |
|---|---|---|
| PARÁFRASIS | 0,508 | 5,4 |
| CONTRASTE | 0,360 | 3,2 |
| INVERTIDO | 0,525 | 5,6 |
| **CAMBIO_MIN** | **0,743** | **6,0** |
| REAL_COMPAT | 0,499 | 5,2 |
| **REAL_INCOMP** | **0,843** | **6,0** |
| FONDO | 0,227 | 1,2 |

**El factor no mide compatibilidad, mide parecido de redacción.** Dos personas
que quieren lo contrario dicho con la misma estructura sacan 0,84; dos que
quieren lo mismo dicho con sus palabras sacan 0,50. El orden está invertido
respecto a lo que el producto necesita, y las dos salen en pantalla como «mucha
afinidad».

### El control que cambió la conclusión

La primera pasada solo tenía INVERTIDO, y decía lo que todo el mundo espera de
estos modelos: **que ignoran la negación**. Es falso aquí. Cambiar una palabra
*sin* negar —`mañanas` por `tardes`— da **0,743**, más alto todavía que negar
(0,525). La negación no es el punto ciego; el punto ciego es que la similitud de
superficie domina sobre el significado, y el «no» al menos añade un token que
mueve algo.

Sin ese control, en esta página habría quedado escrita una explicación plausible
y equivocada. Es la tercera vez que pasa lo mismo en este documento.

### Y el segundo hallazgo, que estaba escondido

Los umbrales del factor (0,15 a 0,55) se calibraron con las trece biografías de
la demostración, que son de quince palabras. Con veinticinco, **casi todo
satura**: 0,499 y 0,843 dan 5,2 y 6,0 puntos, y la misma frase en pantalla. El
rango se queda corto justo donde el texto se parece a lo que la gente escribe.

### ¿Se arregla cambiando de modelo?

No. Es la pregunta obvia y se midió antes de descartarla, con
`multilingual-e5-small` — misma cuantización, mismo runtime, mismo banco, y con
el prefijo `query:` que ese modelo necesita, porque medirlo sin él sería
construirle un fallo al rival.

| | premia a las opuestas por | su rango entero | el error vale |
|---|---|---|---|
| MiniLM-paraphrase (hoy) | +0,344 | 0,617 | **56 %** |
| multilingual-e5-small | +0,073 | 0,115 | **63 %** |

En bruto parece que E5 mejora cinco veces. **No se pueden comparar en bruto**:
cada modelo vive en su banda, y E5 mete todo entre 0,85 y 0,97 — dos textos sin
ninguna relación ya dan 0,851. Lo comparable es qué fracción de su propio rango
ocupa el error, y ahí E5 es **peor**.

La razón es estructural y no se arregla con otro modelo de la misma clase. Un
bi-encoder proyecta cada texto por separado: cuando codifica «busco a alguien
que me exija» no hay nada en ese vector que sepa que va a compararse con su
negación. **La oposición entre dos frases no es una propiedad de ninguna de las
dos, es de la pareja.**

Lo que sí lo resolvería —un cross-encoder, o un modelo de inferencia textual que
sepa decir «contradicción»— rompe la arquitectura: no habría vector que guardar y
habría que pasar el modelo por **cada pareja**, justo lo que el diseño evita para
que el emparejamiento sea instantáneo y para que el servicio pueda caerse sin que
el motor se entere.

### Lo que esto no invalida

El daño está acotado **por diseño**. El factor vale 6 de 100 y cambia una
decisión de cada veintidós, y eso es exactamente porque en su día se decidió que
un modelo no mandara sobre datos duros. Esa decisión aguanta y es la que impide
que un fallo así se lleve el producto por delante.

Lo que no aguanta es la calidad de la señal dentro de esos seis puntos.

### Lo honesto sobre este experimento

Los pares están escritos aquí, no etiquetados por terceros: «por construcción»
significa que la relación es evidente para cualquiera que lea español, no que
haya pasado por anotadores. Y los pares incompatibles son **adversariales a
propósito** —misma estructura, palabra cambiada—, cosa que dos personas
cualesquiera no hacen al escribir.

Lo que no es adversarial es la otra mitad: el 0,499 de dos biografías
genuinamente compatibles escritas con voz propia es realista, y queda por debajo
de casi todo lo demás.

Validar el motor **entero** —el orden que produce, no este factor— necesita otra
cosa: juicios humanos por comparación de pares, con acuerdo entre anotadores
medido para saber cuál es el techo. Eso sigue pendiente.

## Resultado 6: el arreglo, y lo que costó encontrarlo

Sabiendo que comparar dos biografías con un coseno mide redacción y no
compatibilidad, el rediseño evidente es dejar de comparar textos y comparar
**intenciones**: sacar de cada biografía su posición en unos pocos ejes y cruzar
posiciones. Los ejes salen de las trece biografías reales, no de la imaginación
—leídas seguidas, la mayoría hablan de horario y rutina, que ya son campos del
perfil— y lo que el texto libre aporta y no cabe en ningún desplegable es esto:

    qué busca del otro    que le exija  <->  compañía
    ambición              competir      <->  mantenerse
    flexibilidad          me amoldo     <->  tengo mi plan

### Intento 1: proyectar contra anclas. No funciona

La primera versión define cada polo con cuatro frases prototípicas y mira a cuál
se acerca más la biografía. Falla, y falla exactamente igual:

    A: «Busco a alguien que me exija, que me obligue…»       qué busca: +1,00
    B: «Busco a alguien que NO me exija, que respete…»       qué busca: +1,00

Idéntico. Proyectar contra un ancla **sigue siendo un coseno del mismo modelo**:
se cambió qué se compara, no cómo, y el problema estaba en el cómo. De paso
apareció un segundo defecto: el par que solo habla de horarios sacaba −1,00 en un
eje del que no dice nada, o sea que el umbral le inventaba una posición a quien
no había hablado del tema.

### Intento 2: preguntarle a un modelo que entiende la negación

Un modelo de inferencia textual recibe **dos** textos y decide si el primero
implica al segundo, lo contradice, o ninguna cosa. Eso es justo la pregunta:

    premisa:    «Busco a alguien que no me exija, que respete si un día…»
    hipótesis:  «Busco que me exijan.»
    respuesta:  contradicción

**Y esto sí cabe en la arquitectura**, en contra de lo que se dijo un párrafo
antes al descartar los cross-encoders. Aquel argumento —«habría que ejecutarlo
por cada pareja»— es cierto para comparar dos biografías y **falso para leer
una**: aquí el modelo corre una vez por persona al guardar el perfil, igual que
el embedding de hoy, y lo que se guarda son tres números en vez de 384.
Comparar a dos personas sigue siendo aritmética sobre datos ya calculados.

### Lo que de verdad costó: la plantilla, no el modelo

La primera tanda de hipótesis dio neutral en todo. El modelo no estaba roto —en
casos de libro acierta— sino que no hace la correferencia entre «busco» y «esta
persona busca»:

| hipótesis | afirma | niega | separa |
|---|---|---|---|
| «Esta persona busca un compañero que le exija.» | +0,11 | +0,07 | **0,046** |
| **«Busco que me exijan.»** | **+0,62** | **−0,51** | **1,128** |

**Veinticuatro veces más separación por reescribir la frase en primera persona.**
Ninguna mejora de esta página vino de cambiar de modelo; todas vinieron de
cambiar la pregunta. Con NLI además sobra el segundo polo: una hipótesis por eje,
y el signo dice el lado.

### El resultado

| | compatibles vs opuestas | paráfrasis vs palabra cambiada |
|---|---|---|
| coseno entre biografías | −0,344 ✗ | −0,234 ✗ |
| ejes por anclas | −0,199 ✗ | −0,122 ✗ |
| **ejes por NLI** | **+0,283 ✓** | **+0,115 ✓** |

Por primera vez el orden es el correcto en las dos preguntas. Y el grupo FONDO
—textos del dominio sin relación entre sí— sale **4 de 4 sin señal**: deja de
llevarse 1,2 puntos por compartir vocabulario de gimnasio.

### Lo que cuesta

| | RSS |
|---|---|
| base (python + onnxruntime + tokenizers) | 18,6 MB |
| el modelo de hoy | 483,9 MB |
| **el NLI, sustituyéndolo** | **610,9 MB** |

**+127 MB, un 26 %.** Desactivar la arena de ONNX Runtime solo devuelve 7, así
que no hay palanca fácil. Eso rompe el objetivo de 512 MB que motivó toda la
optimización de PyTorch — aunque conviene recordar de dónde salía ese número: era
**portabilidad**, caber en cualquier capa gratuita. El despliegue que documenta
`despliegue-oracle.md` es ARM Always Free con 24 GB, donde 611 MB no es nada.

El otro precio es la cobertura: muchas biografías no dicen nada de ninguno de los
tres ejes, así que el factor **opinará menos veces**. Es el mismo caso que la
fuerza, y es preferible a opinar mal — pero es una pérdida y hay que contarla.

### Qué queda decidido y qué no

Queda medido que el rediseño funciona y lo que cuesta. Queda **sin decidir** si se
integra, y esa es una decisión de producto: cambiar de modelo obliga a recalcular
todos los vectores guardados, a recalibrar los umbrales del factor y a rehacer la
historia de memoria del README. Con seis puntos de cien en juego.

## Qué protege esto para el futuro

Los informes son informes: sus números cambiarán si cambia el reparto. Lo que
queda fijado como prueba son las propiedades que no deberían cambiar sin que
alguien lo decida:

- el horario es lo que más decide;
- la edad desempata, no decide (menos del 15 % de los tramos);
- ninguno de los ocho es decorativo — todos mueven alguna decisión;
- la fuerza es el factor con menos cobertura;
- la afinidad mueve algo, no manda, y cuando decide se explica;
- el gimnasio influye por dos caminos y los dos hacen algo;
- y el experimento se sostiene: quitar un factor no mueve más de lo que vale, y
  las poblaciones que se comparan son la misma gente.

Y hay una que no está en la lista a propósito. Ninguna prueba fija que la vía A
del gimnasio mueva más que la B, ni que el reparto de hoy sea el bueno: eso son
resultados de esta población, y ya se ha visto en esta página lo que pasa cuando
una explicación plausible se queda escrita sin que nadie la mida.

Si mañana alguien reparte los pesos de otra forma, esas pruebas dicen si el
motor sigue apoyándose donde dice apoyarse.
