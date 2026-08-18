# -*- coding: utf-8 -*-
"""
¿El factor de afinidad acierta, o solo se mueve?

Del noveno factor se sabe cuánto mueve —un punto de media, una decisión de cada
veintidós, medido en `docs/medir-el-motor.md`— y no se sabe **si acierta**. Son
dos preguntas distintas: un factor puede mover decisiones y moverlas mal.

No hace falta gente para responderla. Basta con pares cuya relación se conoce
por construcción, y compararlos entre sí:

    PARÁFRASIS   la misma intención con otras palabras   ->  debe salir ALTO
    CONTRASTE    intenciones incompatibles               ->  debe salir BAJO
    INVERTIDO    casi el mismo texto, intención opuesta  ->  debe salir BAJO
    FONDO        dos textos del dominio sin relación     ->  el suelo común

El grupo que decide es **INVERTIDO**, y es el único que no es obvio. «Busco a
alguien que compita conmigo» y «no busco competir con nadie» comparten casi todo
el vocabulario y significan lo contrario. Los modelos de frases suelen puntuar
alto ahí, porque miden tema antes que intención — y una biografía de gimnasio es
justo el sitio donde alguien escribe lo que **no** quiere.

Si INVERTIDO puntúa como PARÁFRASIS, el factor está premiando lo contrario de lo
que debería en el peor caso posible, y eso hay que saberlo antes de defender el
número que enseña la pantalla.

## Lo que esto no es

Los pares están escritos aquí, no etiquetados por terceros. «Por construcción»
significa que la relación es evidente para cualquiera que lea español, no que
haya pasado por anotadores. Para validar el motor entero —el orden que produce,
no este factor— hacen falta juicios humanos, y eso es otro experimento.

    python calibracion/evaluar_afinidad.py
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from modelo_ligero import ModeloLigero  # noqa: E402

# El primero es el que sirve el servidor hoy. El segundo esta aqui para
# contestar la pregunta obvia —¿se arregla cambiando de modelo?— con un numero
# en vez de con una opinion.
#
# La comparacion es justa: misma familia de cuantizacion (qint8 avx512), mismo
# runtime y mismo banco. Lo unico distinto es el modelo.
#
# El prefijo no es un detalle. Los E5 se entrenan con «query: » delante y sin el
# rinden bastante peor; medir sin ponerlo seria construirle un fallo al rival.
MODELOS = [
    ("MiniLM-paraphrase (el de hoy)",
     "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
     "onnx/model_qint8_avx512_vnni.onnx",
     ""),
    ("multilingual-e5-small",
     "intfloat/multilingual-e5-small",
     "onnx/model_qint8_avx512_vnni.onnx",
     "query: "),
]

# Los umbrales del factor, copiados de CalculadoraCompatibilidad. Son los que
# traducen una similitud a puntos, así que sin ellos el coseno es un número sin
# consecuencia.
AFINIDAD_MINIMA = 0.15
AFINIDAD_DE_SOBRA = 0.55
PESO_AFINIDAD = 6


# --------------------------------------------------------------------- el banco

PARAFRASIS = [
    ("Busco a alguien constante para los días de fuerza.",
     "Necesito un compañero que no falle a las sesiones de pesado."),
    ("Prefiero entrenar acompañada en los básicos.",
     "Me gusta tener a alguien al lado en sentadilla y banca."),
    ("Empecé en enero y todavía me da respeto la zona de peso libre.",
     "Soy nueva en esto y las barras aún me imponen."),
    ("Entreno de mañana temprano, antes de trabajar.",
     "Voy al gimnasio a primera hora, antes de la oficina."),
    ("Vengo de powerlifting y necesito quien me ayude en banca pesada.",
     "Hago fuerza y busco a alguien que me asegure en press con mucho peso."),
    ("Me amoldo a lo que haga falta, no soy exigente con la rutina.",
     "Me da igual el reparto, me adapto a lo que entrene el otro."),
]

CONTRASTE = [
    ("Compito en powerlifting, entreno pesado y con descansos largos.",
     "Vengo a desconectar, hago cardio suave y alguna clase dirigida."),
    ("Busco a alguien exigente que me apriete en cada serie.",
     "Voy a mi ritmo y sin presión, esto es mi rato tranquilo."),
    ("Entreno de madrugada, sobre las seis de la mañana.",
     "Solo puedo por la noche, salgo tarde de trabajar."),
    ("Quiero ganar masa y estoy en superávit, todo fuerza.",
     "Estoy en definición y hago mucho cardio para bajar peso."),
    ("Llevo diez años entrenando y compito federado.",
     "Es mi primera semana en un gimnasio, no sé usar las máquinas."),
    ("Prefiero entrenar solo y con cascos, sin hablar.",
     "Lo que busco es gente, charla entre series y ambiente."),
]

# El grupo que decide. Mismo léxico, intención opuesta.
INVERTIDO = [
    ("Busco a alguien que compita conmigo.",
     "No busco competir con nadie."),
    ("Me gusta entrenar acompañado.",
     "No me gusta entrenar acompañado."),
    ("Quiero a alguien que me apriete y me exija.",
     "No quiero que nadie me apriete ni me exija."),
    ("Puedo entrenar por las mañanas.",
     "No puedo entrenar por las mañanas."),
    ("Me interesa el powerlifting y la fuerza máxima.",
     "No me interesa el powerlifting ni la fuerza máxima."),
    ("Estoy dispuesto a cambiar de gimnasio si hace falta.",
     "No estoy dispuesto a cambiar de gimnasio."),
]

# El control que separa dos explicaciones distintas.
#
# Si INVERTIDO sale alto, puede ser porque el modelo ignore la negación o
# simplemente porque dos textos casi iguales le parecen casi iguales, negación o
# no. Estos cambian UNA palabra que da la vuelta al significado sin usar «no»:
# si estos salen bajos y los invertidos altos, el punto ciego es la negación en
# concreto. Si salen igual de altos, lo que falla es la sensibilidad a cambios
# pequeños, que es otro problema.
CAMBIO_MINIMO = [
    ("Puedo entrenar por las mañanas.",
     "Puedo entrenar por las tardes."),
    ("Me interesa el powerlifting y la fuerza máxima.",
     "Me interesa el cardio y la resistencia."),
    ("Quiero a alguien que me apriete y me exija.",
     "Quiero a alguien que me acompañe y me escuche."),
    ("Estoy dispuesto a cambiar de gimnasio si hace falta.",
     "Estoy dispuesto a cambiar de rutina si hace falta."),
    ("Me gusta entrenar acompañado.",
     "Me gusta entrenar solo."),
    ("Busco a alguien que compita conmigo.",
     "Busco a alguien que descanse conmigo."),
]

# Lo mismo, con biografias de longitud real.
#
# Los grupos de arriba son frases de laboratorio de seis palabras, donde cambiar
# una pesa mucho. Una biografia de verdad tiene veinte o treinta, asi que el
# efecto podria diluirse. Estos pares tienen la longitud de las que hay en la
# base, y cada uno enfrenta las dos cosas a la vez:
#
#   COMPATIBLE   quieren lo mismo, escrito de formas distintas  -> debe salir ALTO
#   INCOMPATIBLE quieren cosas opuestas, escrito parecido       -> debe salir BAJO
#
# Si el modelo mide significado, COMPATIBLE gana. Si mide redaccion, gana
# INCOMPATIBLE — y eso es lo que le pasa al producto, porque son biografias que
# alguien podria escribir tal cual.
REAL_COMPATIBLE = [
    ("Llevo tres años entrenando fuerza y busco a alguien serio, que venga a "
     "trabajar y no a mirar el móvil entre series.",
     "Vengo a entrenar de verdad. Me da rabia perder el tiempo, prefiero "
     "compañía que se lo tome en serio aunque sea más floja que yo."),

    ("Estoy empezando y agradecería que alguien me corrigiera la técnica sin "
     "hacerme sentir mal por preguntar.",
     "Soy novata, todavía me lío con las máquinas. Busco paciencia más que "
     "nivel, alguien que no se ría si pregunto una tontería."),

    ("Entreno de seis a ocho de la mañana porque luego trabajo todo el día y "
     "por la tarde ya no me queda nada.",
     "Solo puedo a primera hora. Salgo del gimnasio y me voy directo a la "
     "oficina; por la noche prefiero descansar."),
]

REAL_INCOMPATIBLE = [
    ("Busco a alguien que me exija, que me obligue a sacar la última serie "
     "aunque no me apetezca, y que no acepte excusas.",
     "Busco a alguien que no me exija, que respete si un día no me apetece "
     "sacar la última serie, y que acepte que hay semanas malas."),

    ("Entreno fuerza pesada, series de tres a cinco repeticiones, y descanso "
     "tres minutos entre series porque lo necesito.",
     "Entreno resistencia ligera, series de veinte a treinta repeticiones, y "
     "descanso treinta segundos entre series porque me gusta el ritmo."),

    ("Voy al gimnasio de lunes a viernes por la mañana temprano y los fines de "
     "semana descanso, que es sagrado para mí.",
     "Voy al gimnasio los fines de semana por la tarde y entre semana descanso, "
     "que es sagrado para mí."),
]

FONDO = [
    ("Entreno cuatro días por semana desde hace dos años.",
     "Mi gimnasio está al lado de casa y me pilla de camino."),
    ("Hago torso pierna de lunes a jueves.",
     "Me apunté para quitarme el dolor de espalda de la oficina."),
    ("Los fines de semana descanso siempre.",
     "Uso mancuernas más que máquinas, por costumbre."),
    ("Suelo ir después de comer, sobre las tres.",
     "Escucho podcasts mientras hago cardio."),
]

GRUPOS = {
    "PARAFRASIS": (PARAFRASIS, "alto"),
    "CONTRASTE": (CONTRASTE, "bajo"),
    "INVERTIDO": (INVERTIDO, "bajo"),
    "CAMBIO_MIN": (CAMBIO_MINIMO, "bajo"),
    "REAL_COMPAT": (REAL_COMPATIBLE, "alto"),
    "REAL_INCOMP": (REAL_INCOMPATIBLE, "bajo"),
    "FONDO": (FONDO, "el suelo"),
}


# ----------------------------------------------------------------------- medir

def puntos_del_factor(similitud: float) -> float:
    """Los puntos que ese coseno le daría a una pareja, de los 6 que valen."""
    sobre_el_suelo = (similitud - AFINIDAD_MINIMA) / (AFINIDAD_DE_SOBRA - AFINIDAD_MINIMA)
    return max(0.0, min(1.0, sobre_el_suelo)) * PESO_AFINIDAD


def frase_del_factor(similitud: float) -> str:
    """Lo que la pantalla diría, que es la decisión de verdad."""
    ratio = max(0.0, min(1.0, (similitud - AFINIDAD_MINIMA) / (AFINIDAD_DE_SOBRA - AFINIDAD_MINIMA)))
    if ratio >= 0.75:
        return "mucha"
    if ratio >= 0.4:
        return "algo"
    return "poca"


def medir(nombre: str, repo: str, variante: str, prefijo: str) -> dict:
    """Las similitudes de cada grupo con un modelo."""
    print(f"  cargando {nombre}...")
    modelo = ModeloLigero(repo, variante)

    medidas = {}
    for grupo, (pares, esperado) in GRUPOS.items():
        sims = []
        for a, b in pares:
            # Los vectores salen normalizados, asi que el producto escalar ES el
            # coseno. Igual que en VectorDeTexto.similitudCon.
            va = modelo.vector(prefijo + a)
            vb = modelo.vector(prefijo + b)
            sims.append(float(va @ vb))
        medidas[grupo] = sims
    return medidas


def media(xs) -> float:
    return sum(xs) / len(xs)


def main() -> None:
    print()
    print("Midiendo. La primera vez descarga cada modelo (~120 MB).")
    print()

    resultados = {nombre: medir(nombre, repo, var, pre)
                  for nombre, repo, var, pre in MODELOS}

    # ------------------------------------------------------------- la tabla
    print()
    print("=" * 78)
    print("  ¿Ordena por compatibilidad, o por parecido de redacción?")
    print("=" * 78)
    print()

    anchos = [max(len(n), 12) for n in resultados]
    cabecera = "  " + f"{'grupo':<13}{'debería':<10}"
    for n, a in zip(resultados, anchos):
        cabecera += f"{n:>{a + 2}}"
    print(cabecera)
    print("  " + "-" * (len(cabecera) - 2))

    for grupo, (_, esperado) in GRUPOS.items():
        fila = "  " + f"{grupo:<13}{esperado:<10}"
        for n, a in zip(resultados, anchos):
            fila += f"{media(resultados[n][grupo]):>{a + 2}.3f}"
        print(fila)

    print()

    # -------------------------------------------------- las dos comparaciones
    # que deciden. No son "cuanto se parecen": son si el ORDEN es el correcto.
    print("=" * 78)
    print("  Las dos preguntas que importan")
    print("=" * 78)

    for nombre, medidas in resultados.items():
        compat = media(medidas["REAL_COMPAT"])
        incomp = media(medidas["REAL_INCOMP"])
        paraf = media(medidas["PARAFRASIS"])
        cambio = media(medidas["CAMBIO_MIN"])

        print()
        print(f"  {nombre}")
        print(f"    biografías reales: compatibles {compat:.3f} vs opuestas {incomp:.3f}")
        if compat > incomp:
            print("      [OK]  ordena por lo que dice ordenar")
        else:
            print(f"      [MAL] premia a las opuestas por {incomp - compat:+.3f}")

        print(f"    frases sueltas:    paráfrasis {paraf:.3f} vs una palabra cambiada {cambio:.3f}")
        if paraf > cambio:
            print("      [OK]  la intención pesa más que la forma")
        else:
            print(f"      [MAL] la forma pesa más que la intención por {cambio - paraf:+.3f}")

        # En bruto los dos modelos no se pueden comparar: cada uno vive en su
        # banda. E5 mete todo entre 0,85 y 0,97 —dos textos sin relacion ya dan
        # 0,85— asi que un +0,07 suyo puede ser mas grave que un +0,34 del otro.
        # Lo comparable es que fraccion de su propio rango ocupa el error.
        todas = [media(v) for v in medidas.values()]
        rango = max(todas) - min(todas)
        porcentaje = 100 * (incomp - compat) / rango
        print(f"    su rango entero es {rango:.3f}, del fondo a lo más parecido, así que")
        print(f"      ese error vale el {porcentaje:.0f} % de todo lo que este modelo distingue")

    print()

    # ------------------------------------------------ traduccion a la pantalla
    print("=" * 78)
    print("  Y en puntos del motor (umbrales de hoy: %.2f a %.2f, %d puntos)"
          % (AFINIDAD_MINIMA, AFINIDAD_DE_SOBRA, PESO_AFINIDAD))
    print("=" * 78)
    print()
    for nombre, medidas in resultados.items():
        compat = media(medidas["REAL_COMPAT"])
        incomp = media(medidas["REAL_INCOMP"])
        print(f"  {nombre}")
        print(f"    compatibles  {puntos_del_factor(compat):.1f}/6  «{frase_del_factor(compat)}»")
        print(f"    opuestas     {puntos_del_factor(incomp):.1f}/6  «{frase_del_factor(incomp)}»")
        print()

    print("  Los umbrales son los del modelo de hoy. Un modelo distinto tiene otro")
    print("  fondo común y habría que recalibrarlos: aquí sirven para leer la tabla")
    print("  de la izquierda, no para juzgar al de la derecha.")
    print()


if __name__ == "__main__":
    main()
