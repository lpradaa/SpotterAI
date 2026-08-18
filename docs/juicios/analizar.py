# -*- coding: utf-8 -*-
"""
Qué dicen las respuestas: ¿el motor ordena como ordena la gente?

    python analizar.py respuestas-*.csv

## Lo que mide, y en qué orden importa

**1. Cuánto se ponen de acuerdo las personas entre sí.** Va primero a propósito,
porque es el techo de todo lo demás. Si dos anotadores eligen distinto en la
mitad de los pares, exigirle al motor un 90 % de coincidencia no tiene sentido:
la pregunta no tiene una respuesta clara y el experimento no puede concluir nada.
Un resultado de «los humanos coinciden el 55 % del tiempo» es un hallazgo, no un
fracaso — significa que en esos pares no hay mejor ni peor.

**2. Cuánto coincide el motor con la mayoría humana.** Y se compara contra ese
techo, no contra el 100 %.

**3. Dónde discrepa.** Es lo único accionable: si el motor pierde sistemáticamente
en los pares de «factores enfrentados», el reparto de pesos tiene un problema
concreto y localizable.

## Lo que NO mide

Que el motor sea bueno para el producto. Esto compara el orden del motor con el
criterio de unas cuantas personas mirando fichas, que no es lo mismo que quién
acaba entrenando con quién — para eso está el embudo, que necesita uso real.
"""
from __future__ import annotations

import csv
import sys
from collections import Counter, defaultdict
from math import sqrt
from pathlib import Path


def leer(rutas: list[Path]) -> list[dict]:
    filas = []
    for i, ruta in enumerate(rutas):
        with ruta.open(encoding="utf-8-sig", newline="") as f:
            for fila in csv.DictReader(f):
                # El anotador es el fichero: nadie pone su nombre, y pedirlo
                # solo conseguiría que alguien se lo pensara antes de contestar.
                fila["anotador"] = ruta.stem
                fila["_n"] = i
                filas.append(fila)
    return filas


def clave(fila: dict) -> tuple:
    """Una comparación concreta, para cruzar a quien la contestó dos veces."""
    return (fila["juez"], fila["a"], fila["b"])


def eleccion_del_motor(fila: dict) -> str | None:
    """
    A cuál prefiere el motor, o None si le da igual.

    El empate no es una opinión. Salió en el primer cuadernillo —dos candidatos
    a 86— y con un `>=` se contaba como si el motor hubiera elegido el primero,
    que es la posición en una lista y no una preferencia. Cuando la persona
    elegía el otro, aquello aparecía como un fallo del motor que el motor no
    había cometido.

    Los empates se apartan y se cuentan aparte: son informativos por su cuenta
    —si hay muchos, el motor no discrimina donde debería— pero no son aciertos
    ni errores.
    """
    a, b = int(fila["puntosA"]), int(fila["puntosB"])
    if a == b:
        return None
    return "a" if a > b else "b"


def main() -> None:
    rutas = [Path(a) for a in sys.argv[1:]]
    if not rutas:
        print(__doc__)
        sys.exit(1)

    filas = leer(rutas)
    anotadores = sorted({f["anotador"] for f in filas})

    print()
    print("=" * 66)
    print(f"  {len(filas)} respuestas de {len(anotadores)} anotadores")
    print("=" * 66)
    print()

    # --- 1. El techo: acuerdo entre personas -----------------------------
    porComparacion = defaultdict(list)
    for f in filas:
        porComparacion[clave(f)].append(f)

    compartidas = {k: v for k, v in porComparacion.items() if len(v) > 1}

    if not compartidas:
        print("  Ningún par ha sido contestado por más de una persona, así que no")
        print("  se puede saber cuánto se ponen de acuerdo entre ellas — y sin eso")
        print("  no hay con qué comparar al motor. Haría falta que varios usaran")
        print("  el mismo cuadernillo.")
        print()
        techo = None
    else:
        deAcuerdo = 0
        for respuestas in compartidas.values():
            elecciones = Counter(r["elegido"] for r in respuestas)
            # Cuántos coinciden con la mayoría, sobre el total.
            deAcuerdo += elecciones.most_common(1)[0][1] / len(respuestas)

        techo = 100 * deAcuerdo / len(compartidas)
        print(f"  Acuerdo entre personas   {techo:.1f} %   "
              f"({len(compartidas)} comparaciones contestadas por varios)")
        print("    Este es el techo. El motor no puede aspirar a más que esto.")
        print()

    # --- 2. El motor contra la mayoría humana ----------------------------
    aciertos = totales = empates = 0
    porMotivo = defaultdict(lambda: [0, 0])

    for k, respuestas in porComparacion.items():
        humanas = Counter(r["elegido"] for r in respuestas if r["elegido"] != "ninguna")
        if not humanas:
            continue        # todos dijeron «no sabría decidir»

        mayoria = humanas.most_common(1)[0][0]
        motor = eleccion_del_motor(respuestas[0])
        motivo = respuestas[0]["motivo"]

        if motor is None:
            empates += 1
            continue

        totales += 1
        porMotivo[motivo][1] += 1
        if mayoria == motor:
            aciertos += 1
            porMotivo[motivo][0] += 1

    if not totales:
        print("  Nadie ha elegido en ninguna comparación: solo hay «no sabría decidir».")
        return

    coincidencia = 100 * aciertos / totales
    print(f"  El motor coincide con la mayoría   {coincidencia:.1f} %   "
          f"({aciertos} de {totales})")
    if empates:
        print(f"    Y en {empates} el motor empataba, así que no tenía preferencia "
              f"que comparar.")

    # Con pocas comparaciones el porcentaje de arriba no significa gran cosa, y
    # decirlo importa más que el propio numero: es la diferencia entre un dato y
    # una anecdota con decimales.
    p = aciertos / totales
    err = 1.96 * sqrt(p * (1 - p) / totales)
    print(f"    Intervalo de confianza del 95 %: "
          f"{100 * max(0, p - err):.0f} % a {100 * min(1, p + err):.0f} %.")
    if totales < 40:
        print(f"    Con {totales} comparaciones ese intervalo es enorme y no permite "
              f"concluir nada;")
        print("    hacen falta varias personas contestando el mismo cuadernillo.")
    if techo:
        print(f"    Sobre un techo del {techo:.1f} %, o sea el "
              f"{100 * coincidencia / techo:.0f} % de lo alcanzable.")
    print()

    # --- 3. Dónde discrepa, que es lo accionable -------------------------
    print("  Por tipo de comparación:")
    for motivo, (bien, total) in sorted(porMotivo.items()):
        print(f"    {motivo:<22} {100 * bien / total:5.1f} %   ({bien} de {total})")
    print()

    # Sesgo de posición: si la gente elige la izquierda mucho más que la
    # derecha, están respondiendo por sitio y no por criterio, y todo lo de
    # arriba vale menos.
    izquierda = sum(1 for f in filas
                    if f["elegido"] != "ninguna" and f["elegido"] == f["izquierdaFue"])
    decididas = sum(1 for f in filas if f["elegido"] != "ninguna")
    if decididas:
        sesgo = 100 * izquierda / decididas
        print(f"  Eligieron la ficha de la izquierda el {sesgo:.0f} % de las veces.")
        if abs(sesgo - 50) > 15:
            print("    Eso se aleja bastante del 50 %: puede que se esté respondiendo")
            print("    por posición y no por criterio, y entonces lo de arriba vale menos.")
        print()

    dudas = sum(1 for f in filas if f["elegido"] == "ninguna")
    print(f"  «No sabría decidir» en el {100 * dudas / len(filas):.0f} % de las respuestas.")
    if dudas / len(filas) > 0.3:
        print("    Con tanta duda, puede que las fichas no den bastante información")
        print("    para decidir — eso es un problema del cuadernillo, no del motor.")
    print()


if __name__ == "__main__":
    main()
