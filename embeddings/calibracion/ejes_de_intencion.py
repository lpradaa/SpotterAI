# -*- coding: utf-8 -*-
"""
Comparar intenciones en vez de textos.

`evaluar_afinidad.py` deja medido que comparar dos biografías con un coseno mide
parecido de redacción y no compatibilidad: dos personas que quieren lo contrario
dicho con la misma estructura sacan 0,843 y dos que quieren lo mismo dicho con
sus palabras sacan 0,499. Y que no se arregla cambiando de modelo, porque la
oposición entre dos frases no es propiedad de ninguna de las dos.

Esto prueba otra cosa. En vez de preguntar «¿se parecen estos dos textos?»,
pregunta «¿dónde cae cada uno en los tres ejes que importan?» y compara las
posiciones.

## Por qué esto puede funcionar donde lo otro falla

Porque cambia lo que se compara con lo que. Antes: biografía contra biografía,
dos textos largos que comparten dominio, estructura y vocabulario. Ahora:
biografía contra **anclas** —frases cortas y prototípicas de cada polo— y lo que
decide no es la similitud absoluta sino **cuál de los dos polos queda más cerca**.

Es una comparación relativa, y ahí el fondo común se cancela: si la biografía se
parece un 0,6 a un polo y un 0,3 al otro, da igual que los dos números estén
inflados por el vocabulario de gimnasio, porque lo que se lee es la diferencia.

## De dónde salen los ejes

De las trece biografías que hay en la base, no de la imaginación. Leídas
seguidas, la mayoría hablan de **horario y rutina** — que ya son campos del
perfil y el motor ya los cruza. Lo que el texto libre aporta y no cabe en ningún
desplegable es esto:

    qué busca del otro    que le exija y le empuje  <->  compañía y buen rato
    ambición              competir, mover peso      <->  mantenerse, salud
    flexibilidad          me amoldo a lo que sea    <->  tengo mi plan

Tres ejes, no diez: cada uno tiene que estar de verdad en lo que la gente
escribe, y con trece biografías reales delante no hay más que se repita.

## Lo que un eje puede decir, y lo que no

Si un texto queda a la misma distancia de los dos polos, **ese eje no dice nada
de esa persona** y no se evalúa. Es el mismo trato que da el motor a un campo
vacío, y es lo que evita inventarse una posición para quien no ha hablado del
tema: la mitad de las biografías de la base no dicen nada de ambición.

    python calibracion/ejes_de_intencion.py
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import numpy as np  # noqa: E402

from modelo_ligero import ModeloLigero  # noqa: E402

REPO = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
VARIANTE = "onnx/model_qint8_avx512_vnni.onnx"

# Por debajo de esta diferencia entre los dos polos, se considera que el texto
# no habla del eje. No sale de ninguna tabla: es el orden de magnitud en el que
# las trece biografías reales dejan de estar claramente de un lado.
UMBRAL_DE_SEÑAL = 0.04


EJES = {
    "qué busca del otro": {
        "exigencia": [
            "Busco a alguien que me exija y no me deje abandonar la última serie.",
            "Quiero un compañero que me empuje cuando se me acaben las ganas.",
            "Necesito a alguien constante, que no falle a las sesiones duras.",
            "Me viene bien que alguien me controle la técnica y me corrija.",
        ],
        "compañía": [
            "Busco compañía para no venir solo al gimnasio.",
            "Me gusta entrenar acompañada, charlar entre series y pasarlo bien.",
            "Quiero conocer gente del barrio con quien coincidir en el gimnasio.",
            "Prefiero un ambiente tranquilo y sin presión, cada uno a su ritmo.",
        ],
    },
    "ambición": {
        "competir": [
            "Compito en powerlifting y entreno para subir mis marcas.",
            "Voy a por el peso máximo, series pesadas y descansos largos.",
            "Me tomo el entrenamiento en serio, quiero progresar de verdad.",
            "Entreno para competir en mi categoría de peso.",
        ],
        "mantenerse": [
            "Vengo a mantenerme y a quitarme el dolor de espalda.",
            "Hago ejercicio por salud, sin ninguna meta de rendimiento.",
            "Me lo tomo con calma, esto es mi rato para desconectar.",
            "Estoy empezando y voy despacio, sin prisa por levantar más.",
        ],
    },
    "flexibilidad": {
        "me amoldo": [
            "Me amoldo a lo que haga falta, no soy exigente con la rutina.",
            "Me da igual el reparto, me adapto a lo que entrene el otro.",
            "Puedo cambiar de horario o de gimnasio si hace falta.",
            "Soy flexible con los días y con lo que toque entrenar.",
        ],
        "tengo mi plan": [
            "Sigo mi rutina de torso pierna y no la cambio.",
            "Tengo mis días y mis horas fijas, no me muevo de ahí.",
            "Llevo mi programación escrita y la sigo al detalle.",
            "Entreno siempre lo mismo el mismo día de la semana.",
        ],
    },
}


class LectorDeIntenciones:
    """Coloca un texto en cada eje, o dice que ese eje no aplica."""

    def __init__(self, modelo: ModeloLigero) -> None:
        self.modelo = modelo
        # El vector de un polo es la media de sus anclas, normalizada: así el
        # polo es "la idea" y no una frase concreta con sus manías.
        self.polos = {}
        for eje, extremos in EJES.items():
            self.polos[eje] = {}
            for nombre, frases in extremos.items():
                vectores = np.array([modelo.vector(f) for f in frases])
                medio = vectores.mean(axis=0)
                self.polos[eje][nombre] = medio / np.linalg.norm(medio)

    def leer(self, texto: str) -> dict:
        """Para cada eje: la posición de -1 a 1, o None si el texto no habla."""
        v = self.modelo.vector(texto)
        posiciones = {}

        for eje, extremos in self.polos.items():
            (nombre_a, polo_a), (nombre_b, polo_b) = list(extremos.items())
            sim_a = float(v @ polo_a)
            sim_b = float(v @ polo_b)
            diferencia = sim_a - sim_b

            if abs(diferencia) < UMBRAL_DE_SEÑAL:
                posiciones[eje] = None      # de esto no dice nada
            else:
                # Se escala para que la diferencia sea legible; el signo es lo
                # que manda, la magnitud solo modula.
                posiciones[eje] = max(-1.0, min(1.0, diferencia * 5))

        return posiciones


def compatibilidad(a: dict, b: dict) -> tuple[float | None, list[str]]:
    """
    Cuánto encajan dos lecturas, y por qué.

    Los tres ejes son de coincidencia: dos personas que quieren lo mismo encajan.
    La flexibilidad podría tratarse como complemento —un flexible con un rígido
    funciona— pero eso hay que medirlo antes de afirmarlo, así que aquí no se
    hace.

    Devuelve None cuando ningún eje se puede evaluar, que es el mismo trato que
    da el motor a un factor sin datos.
    """
    evaluados = []
    explicacion = []

    for eje in EJES:
        if a.get(eje) is None or b.get(eje) is None:
            continue

        # Distancia entre las dos posiciones, de 0 (idénticos) a 2 (opuestos).
        distancia = abs(a[eje] - b[eje])
        encaje = 1.0 - distancia / 2.0
        evaluados.append(encaje)

        lado = lambda p, e: list(EJES[e])[0] if p > 0 else list(EJES[e])[1]
        if distancia < 0.5:
            explicacion.append(f"{eje}: los dos hacia «{lado(a[eje], eje)}»")
        elif distancia > 1.2:
            explicacion.append(
                f"{eje}: uno hacia «{lado(a[eje], eje)}» y otro hacia «{lado(b[eje], eje)}»")

    if not evaluados:
        return None, ["ninguno de los tres ejes sale en los dos textos"]

    return sum(evaluados) / len(evaluados), explicacion
