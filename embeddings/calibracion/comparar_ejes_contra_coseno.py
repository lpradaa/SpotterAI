# -*- coding: utf-8 -*-
"""
¿Los ejes de intención arreglan lo que el coseno hacía mal?

Mismo banco que `evaluar_afinidad.py`, mismo modelo, misma cuantización. Lo
único que cambia es cómo se compara: antes coseno entre las dos biografías,
ahora posición de cada una en tres ejes y encaje entre posiciones.

La pregunta es una sola y no admite matices:

    ¿Salen las biografías COMPATIBLES por encima de las OPUESTAS?

El coseno decía que no —0,499 contra 0,843, con las opuestas ganando por 34
centésimas— y esa es la razón de que exista este experimento.

    python calibracion/comparar_ejes_contra_coseno.py
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from modelo_ligero import ModeloLigero  # noqa: E402
from calibracion.ejes_de_intencion import (  # noqa: E402
    EJES, LectorDeIntenciones, compatibilidad,
)
from calibracion.evaluar_afinidad import (  # noqa: E402
    REAL_COMPATIBLE, REAL_INCOMPATIBLE, PARAFRASIS, CAMBIO_MINIMO, FONDO,
)
from calibracion.ejes_de_intencion import REPO, VARIANTE  # noqa: E402

GRUPOS = [
    ("REAL_COMPAT", REAL_COMPATIBLE, "alto"),
    ("REAL_INCOMP", REAL_INCOMPATIBLE, "bajo"),
    ("PARAFRASIS", PARAFRASIS, "alto"),
    ("CAMBIO_MIN", CAMBIO_MINIMO, "bajo"),
    ("FONDO", FONDO, "—"),
]


def main() -> None:
    print("Cargando el modelo...")
    modelo = ModeloLigero(REPO, VARIANTE)
    lector = LectorDeIntenciones(modelo)

    print()
    print("=" * 76)
    print("  Coseno entre biografías  vs  encaje de intenciones")
    print("=" * 76)
    print()
    print(f"  {'grupo':<13}{'debería':<9}{'coseno':>9}{'ejes':>9}{'sin señal':>12}")
    print("  " + "-" * 54)

    resumen = {}
    for nombre, pares, esperado in GRUPOS:
        cosenos, encajes, mudos = [], [], 0

        for a, b in pares:
            va, vb = modelo.vector(a), modelo.vector(b)
            cosenos.append(float(va @ vb))

            encaje, _ = compatibilidad(lector.leer(a), lector.leer(b))
            if encaje is None:
                mudos += 1
            else:
                encajes.append(encaje)

        media_cos = sum(cosenos) / len(cosenos)
        media_eje = sum(encajes) / len(encajes) if encajes else float("nan")
        resumen[nombre] = (media_cos, media_eje)

        eje_txt = f"{media_eje:>9.3f}" if encajes else f"{'—':>9}"
        print(f"  {nombre:<13}{esperado:<9}{media_cos:>9.3f}{eje_txt}"
              f"{mudos:>7}/{len(pares)}")

    print()
    print("  sin señal = pares en los que ningún eje sale en los DOS textos")
    print()

    # ------------------------------------------------------- la única pregunta
    print("=" * 76)
    print("  ¿Ordena por compatibilidad?")
    print("=" * 76)
    print()

    for etiqueta, indice in (("coseno entre biografías", 0), ("encaje de intenciones", 1)):
        compat = resumen["REAL_COMPAT"][indice]
        incomp = resumen["REAL_INCOMP"][indice]
        signo = "OK " if compat > incomp else "MAL"
        print(f"  [{signo}] {etiqueta:<26} compatibles {compat:.3f}  opuestas {incomp:.3f}"
              f"   ({compat - incomp:+.3f})")

    print()

    # ------------------------------------------- y por qué, par a par, en detalle
    print("-" * 76)
    print("  Las biografías opuestas, leídas por ejes")
    print("-" * 76)
    print()
    for a, b in REAL_INCOMPATIBLE:
        encaje, porques = compatibilidad(lector.leer(a), lector.leer(b))
        cos = float(modelo.vector(a) @ modelo.vector(b))
        print(f"  coseno {cos:.3f}  ->  ejes {encaje if encaje is None else f'{encaje:.3f}'}")
        print(f"    A: {a[:68]}...")
        print(f"    B: {b[:68]}...")
        for p in porques:
            print(f"    · {p}")
        print()


if __name__ == "__main__":
    main()
