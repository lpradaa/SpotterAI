# -*- coding: utf-8 -*-
"""
Dónde poner el umbral por debajo del cual un texto «no habla» de un eje.

El primero se puso a ojo en 0,15 y con las trece biografías reales deja el factor
evaluable en **9 de 78 parejas: un 11,5 %**. Es peor cobertura que la fuerza, que
ya era el peor caso del motor. El factor acertaría casi siempre y opinaría casi
nunca.

Bajarlo sin más tampoco vale: un umbral de cero convierte cualquier temblor del
modelo en una postura, y entonces el factor vuelve a opinar de todo — que es
exactamente el problema del que se venía.

Así que hay que mirar las dos cosas a la vez, porque van en direcciones
contrarias:

    cobertura  cuántas parejas reales tienen algún eje en común
    acierto    si las biografías compatibles siguen puntuando por encima de las
               opuestas (el banco de `evaluar_afinidad.py`)

    python calibracion/calibrar_umbral.py
"""
from __future__ import annotations

import itertools
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from intenciones import EJES, LectorDeIntenciones  # noqa: E402
from calibracion.evaluar_afinidad import REAL_COMPATIBLE, REAL_INCOMPATIBLE  # noqa: E402

# Las trece de la base, que son las únicas escritas pensando en personas.
BIOGRAFIAS_REALES = [
    "Llevo dos años entrenando en serio. Busco a alguien constante para los días de fuerza.",
    "Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.",
    "Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.",
    "Empecé en enero. Todavía me da respeto la zona de peso libre.",
    "Empuje/tirón/pierna. Entreno temprano casi siempre.",
    "Corro y hago algo de fuerza. Me amoldo a lo que haga falta.",
    "Un grupo por día, sin prisa. Pecho los lunes, como todo el mundo.",
    "En Chamberí, casi siempre por la tarde. Me muevo si hace falta.",
    "Recién mudado al barrio, buscando con quién ir.",
    "Compito en -63 kg. Entreno de mañana.",
    "Entreno a mediodía, que es cuando puedo escaparme del trabajo.",
    "Vuelve después de una lesión de hombro. Voy con calma en empuje.",
    "Acabo de empezar, todavía rellenando el perfil.",
]

UMBRALES = [0.0, 0.02, 0.05, 0.08, 0.10, 0.15, 0.20, 0.30]


def posiciones_crudas(lector: LectorDeIntenciones, textos: list[str]) -> list[dict]:
    """Las posiciones SIN umbral, para no repetir la inferencia ocho veces."""
    return [{eje: lector.posicion(t, hip) for eje, hip in EJES.items()} for t in textos]


def con_umbral(crudas: dict, umbral: float) -> dict:
    return {e: (None if abs(v) < umbral else v) for e, v in crudas.items()}


def encaje(a: dict, b: dict):
    comunes = [1.0 - abs(a[e] - b[e]) / 2.0
               for e in EJES if a[e] is not None and b[e] is not None]
    return sum(comunes) / len(comunes) if comunes else None


def main() -> None:
    print("Cargando el modelo...")
    lector = LectorDeIntenciones()

    reales = posiciones_crudas(lector, BIOGRAFIAS_REALES)
    compat = [(posiciones_crudas(lector, [a])[0], posiciones_crudas(lector, [b])[0])
              for a, b in REAL_COMPATIBLE]
    incomp = [(posiciones_crudas(lector, [a])[0], posiciones_crudas(lector, [b])[0])
              for a, b in REAL_INCOMPATIBLE]

    print()
    print("=" * 72)
    print("  Dónde poner el umbral: cobertura contra acierto")
    print("=" * 72)
    print()
    print(f"  {'umbral':>7}{'cobertura':>12}{'compatibles':>14}{'opuestas':>11}{'acierta':>10}")
    print("  " + "-" * 54)

    for umbral in UMBRALES:
        # Cobertura sobre las parejas reales.
        pares = list(itertools.combinations(reales, 2))
        con_eje = sum(1 for a, b in pares
                      if encaje(con_umbral(a, umbral), con_umbral(b, umbral)) is not None)
        cobertura = 100 * con_eje / len(pares)

        # Acierto sobre el banco.
        def media(grupo):
            vals = [encaje(con_umbral(a, umbral), con_umbral(b, umbral)) for a, b in grupo]
            vals = [v for v in vals if v is not None]
            return sum(vals) / len(vals) if vals else None

        mc, mi = media(compat), media(incomp)

        if mc is None or mi is None:
            veredicto, mc_txt, mi_txt = "sin datos", "  —", "  —"
        else:
            veredicto = "sí" if mc > mi else "NO"
            mc_txt, mi_txt = f"{mc:.3f}", f"{mi:.3f}"

        print(f"  {umbral:>7.2f}{cobertura:>11.1f}%{mc_txt:>14}{mi_txt:>11}{veredicto:>10}")

    print()
    print("  cobertura   parejas de las 13 biografías reales con algún eje en común")
    print("  acierta     si las compatibles puntúan por encima de las opuestas")
    print()


if __name__ == "__main__":
    main()
