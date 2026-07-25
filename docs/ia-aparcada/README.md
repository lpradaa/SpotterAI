# IA aparcada

Aquí está, tal cual estaba, el código que llamaba a la API de Claude para
redactar las explicaciones de compatibilidad. Se retira del proyecto por
decisión de producto: hay bastante que arreglar antes, y la llamada real nunca
llegó a ejecutarse porque nunca hubo clave configurada.

**Nada de esto se nota en la aplicación.** Lo que se veía en pantalla ya era el
texto determinista de respaldo, que sigue funcionando exactamente igual desde
`ExplicadorCompatibilidad`. Lo único que desaparece es una llamada de red que
nunca se hacía y una dependencia de 2 MB que nunca se usaba.

## Qué había aquí

| Fichero | Qué es |
|---|---|
| `ExplicadorCompatibilidad.java` | La clase completa: cliente perezoso, instrucciones del sistema, salida estructurada y el respaldo determinista |
| `ExplicadorCompatibilidadTest.java` | Sus pruebas, incluidas las dos de `construirResumen` que ya no tienen consumidor |
| `ExplicacionMatch.java` | El record **con** las anotaciones `@JsonPropertyDescription`, que son el esquema que viajaba al modelo. El que sigue vivo en el proyecto es el mismo record sin ellas, porque ya no describen nada |

## La decisión que conviene no perder

El modelo **no puntuaba**. Recibía el desglose ya calculado por
`CalculadoraCompatibilidad` y solo lo redactaba.

Esa separación es lo importante y hay que mantenerla si esto vuelve: una
puntuación que decide un orden tiene que ser instantánea, gratis, idéntica
entre ejecuciones y testeable. Un modelo puntuando da un producto que no se
puede depurar ni defender.

## Cómo devolverlo

1. Copiar los dos ficheros a sus paquetes originales:
   - `backend/src/main/java/com/spotterai/backend/matching/`
   - `backend/src/test/java/com/spotterai/backend/matching/`
2. Devolver la dependencia al `backend/pom.xml`:

   ```xml
   <!-- SDK oficial de Anthropic: redacta las explicaciones de compatibilidad -->
   <dependency>
       <groupId>com.anthropic</groupId>
       <artifactId>anthropic-java</artifactId>
       <version>2.34.0</version>
   </dependency>
   ```
3. Exportar `ANTHROPIC_API_KEY` antes de arrancar. Sin ella la clase cae sola
   al respaldo, así que el paso 3 es opcional para que compile y arranque.

No hace falta tocar nada más: el endpoint, el servicio y el frontend no
cambiaron al retirarla, porque la firma pública de `explicar(...)` es la misma.

## Lo que quedó pendiente de comprobar

**La llamada real nunca se ha ejecutado.** El código está escrito contra el SDK
2.34.0 con salida estructurada (`.outputConfig(ExplicacionMatch.class)`) y el
modelo `claude-opus-5`, pero sin clave nunca pasó del respaldo. Si vuelve, lo
primero es verificar esa llamada de verdad, no darla por buena.

## Dónde estaba el valor de verdad

Redactar una puntuación ya calculada es lo de menos: se puede hacer con
plantillas y queda casi igual. El uso que sí cambiaría el producto es el
**registro conversacional** — convertir *"suelo ir por las tardes después del
curro, sobre las 7, menos los viernes"* en franjas estructuradas. Eso es lo que
un modelo hace y el código no, y ataca el problema real, que es que el motor de
compatibilidad está sin datos porque nadie rellena formularios.

Si esto se retoma algún día, empezar por ahí y no por la redacción.
