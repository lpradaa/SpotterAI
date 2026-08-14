# -*- coding: utf-8 -*-
"""
¿Cuantizar el modelo cambia las decisiones del motor?

Cuantizar a int8 pierde precisión. La pregunta no es cuánta —eso siempre se
pierde algo— sino si esa pérdida llega a cambiar lo que el producto hace: los
puntos que reparte el factor y la frase que enseña.

Por eso esto no compara vectores, compara **decisiones**. Pasa las mismas
biografías reales que fijaron los umbrales del factor por los dos modelos y mira
si alguna pareja cambia de tramo.

    python calibracion/comparar_cuantizado.py
"""
import itertools
from pathlib import Path

from sentence_transformers import SentenceTransformer

# Las biografías reales de la base, que son las que calibraron los umbrales.
BIOS = {
    "Alex":   "Llevo dos años entrenando en serio. Busco a alguien constante para los días de fuerza.",
    "Marta":  "Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.",
    "Javi":   "Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.",
    "Lucia":  "Empecé en enero. Todavía me da respeto la zona de peso libre.",
    "Diego":  "Empuje/tirón/pierna. Entreno temprano casi siempre.",
    "Noa":    "Corro y hago algo de fuerza. Me amoldo a lo que haga falta.",
    "Hugo":   "Un grupo por día, sin prisa. Pecho los lunes, como todo el mundo.",
    "Carmen": "En Chamberí, casi siempre por la tarde. Me muevo si hace falta.",
}

# Los mismos que CalculadoraCompatibilidad. Si allí cambian, aquí también.
AFINIDAD_MINIMA = 0.15
AFINIDAD_DE_SOBRA = 0.55
PESO_AFINIDAD = 6


def tramo(similitud: float) -> tuple[int, str]:
    """Los puntos y la frase que le tocarían a esta pareja."""
    ratio = min(max((similitud - AFINIDAD_MINIMA) / (AFINIDAD_DE_SOBRA - AFINIDAD_MINIMA), 0.0), 1.0)
    puntos = round(ratio * PESO_AFINIDAD)

    if ratio >= 0.75:
        return puntos, "mucha"
    if ratio >= 0.4:
        return puntos, "algo"
    return puntos, "poca"


def vectores(modelo: SentenceTransformer) -> dict[str, list[float]]:
    return {n: modelo.encode(t, normalize_embeddings=True) for n, t in BIOS.items()}


def main() -> None:
    aqui = Path(__file__).parent.parent
    nombre = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"

    # La variante decide la VELOCIDAD, no el resultado: los tres ficheros int8
    # dan los mismos numeros, cada uno optimizado para un juego de instrucciones.
    # avx512_vnni para Intel/AMD modernos; arm64 si se despliega en ARM.
    import os
    VARIANTE = os.getenv("VARIANTE_INT8", "onnx/model_qint8_avx512_vnni.onnx")

    print("Cargando el modelo normal (float32)…")
    normal = SentenceTransformer(nombre)

    print("Cargando el cuantizado (int8)…")
    # Del propio repositorio del modelo, que publica las variantes cuantizadas
    # ya hechas. Exportarlas nosotros daba exactamente el mismo fichero de 113 MB,
    # asi que no habia razon para mantener un paso de export propio.
    cuantizado = SentenceTransformer(
        nombre,
        backend="onnx",
        model_kwargs={"file_name": VARIANTE},
    )

    v_normal, v_int8 = vectores(normal), vectores(cuantizado)
    sim = lambda v, a, b: float(sum(x * y for x, y in zip(v[a], v[b])))

    print(f"\n{'pareja':22} {'float32':>9} {'int8':>9} {'dif':>7}   decisión")
    print("-" * 72)

    cambios, desviaciones = 0, []
    for a, b in itertools.combinations(BIOS, 2):
        s1, s2 = sim(v_normal, a, b), sim(v_int8, a, b)
        desviaciones.append(abs(s1 - s2))

        p1, f1 = tramo(s1)
        p2, f2 = tramo(s2)

        if (p1, f1) == (p2, f2):
            veredicto = f"= {p1}/6 {f1}"
        else:
            veredicto = f"CAMBIA  {p1}/6 {f1} -> {p2}/6 {f2}"
            cambios += 1

        print(f"{a + ' + ' + b:22} {s1:9.3f} {s2:9.3f} {s2 - s1:+7.3f}   {veredicto}")

    print("-" * 72)
    print(f"Desviación máxima en la similitud: {max(desviaciones):.4f}")
    print(f"Desviación media:                  {sum(desviaciones) / len(desviaciones):.4f}")
    print(f"Parejas que cambian de decisión:   {cambios} de {len(desviaciones)}")

    # Lo que de verdad importa: el orden. El factor no usa la similitud en bruto,
    # la usa para repartir puntos — si el orden entre parejas se conserva, el
    # producto se comporta igual aunque los decimales bailen.
    orden_normal = sorted(itertools.combinations(BIOS, 2), key=lambda p: sim(v_normal, *p))
    orden_int8 = sorted(itertools.combinations(BIOS, 2), key=lambda p: sim(v_int8, *p))
    print(f"El orden de las parejas se conserva: {'sí' if orden_normal == orden_int8 else 'NO'}")

    print("\n" + ("APTO: la cuantización no cambia ninguna decisión del motor."
                  if cambios == 0 else
                  f"OJO: {cambios} parejas cambian de tramo. Revisar antes de adoptarlo."))


if __name__ == "__main__":
    main()
