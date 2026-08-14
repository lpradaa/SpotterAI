# -*- coding: utf-8 -*-
"""
¿Da el modelo ligero los mismos números que sentence-transformers?

Tiene que darlos. En la base hay vectores calculados con la pila antigua, y dos
formas distintas de proyectar el mismo texto harían que las similitudes entre
unos y otros dejaran de significar nada.

Se comparan las dos cosas que importan: el vector en sí, y —lo que de verdad
decide— si alguna pareja cambiaría de tramo en el motor.
"""
import itertools, sys
import numpy as np

REPO = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
BIOS = {
 "Alex":"Llevo dos años entrenando en serio. Busco a alguien constante para los días de fuerza.",
 "Marta":"Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.",
 "Javi":"Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.",
 "Lucia":"Empecé en enero. Todavía me da respeto la zona de peso libre.",
 "Diego":"Empuje/tirón/pierna. Entreno temprano casi siempre.",
 "Noa":"Corro y hago algo de fuerza. Me amoldo a lo que haga falta.",
 "Hugo":"Un grupo por día, sin prisa. Pecho los lunes, como todo el mundo.",
 "Carmen":"En Chamberí, casi siempre por la tarde. Me muevo si hace falta.",
}
VARIANTE = sys.argv[1] if len(sys.argv) > 1 else "onnx/model.onnx"

from modelo_ligero import ModeloLigero
ligero = ModeloLigero(REPO, VARIANTE)
v_lig = {n: ligero.vector(t) for n, t in BIOS.items()}

from sentence_transformers import SentenceTransformer
st = SentenceTransformer(REPO)
v_st = {n: st.encode(t, normalize_embeddings=True) for n, t in BIOS.items()}

print(f"\nVariante: {VARIANTE}")
print(f"{'texto':10} {'|dif| max por dimension':>24}")
for n in BIOS:
    print(f"{n:10} {np.abs(v_lig[n] - v_st[n]).max():>24.6f}")

sim = lambda v,a,b: float(np.dot(v[a], v[b]))
peor = max(abs(sim(v_lig,a,b) - sim(v_st,a,b)) for a,b in itertools.combinations(BIOS,2))
print(f"\nPeor desviacion en la similitud entre parejas: {peor:.6f}")

def tramo(s):
    r = min(max((s-0.15)/0.40, 0.0), 1.0)
    return round(r*6), ("mucha" if r>=0.75 else "algo" if r>=0.4 else "poca")

cambios = [f"{a}+{b}" for a,b in itertools.combinations(BIOS,2)
           if tramo(sim(v_lig,a,b)) != tramo(sim(v_st,a,b))]
print(f"Parejas que cambiarian de tramo: {len(cambios)}" + (f"  ({', '.join(cambios)})" if cambios else ""))
print("\nIDENTICO: el ligero puede sustituir a la pila antigua." if not cambios
      else "\nDIFIERE: revisar antes de sustituir.")
