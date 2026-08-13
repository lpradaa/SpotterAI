# -*- coding: utf-8 -*-
import json, urllib.request, itertools

BIOS = {
 "Alex":   "Llevo dos años entrenando en serio. Busco a alguien constante para los días de fuerza.",
 "Marta":  "Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.",
 "Javi":   "Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.",
 "Lucia":  "Empecé en enero. Todavía me da respeto la zona de peso libre.",
 "Diego":  "Empuje/tirón/pierna. Entreno temprano casi siempre.",
 "Noa":    "Corro y hago algo de fuerza. Me amoldo a lo que haga falta.",
 "Hugo":   "Un grupo por día, sin prisa. Pecho los lunes, como todo el mundo.",
}

def vec(t):
    r = urllib.request.urlopen(urllib.request.Request(
        "http://localhost:8000/vector", method="POST",
        data=json.dumps({"texto": t}).encode(),
        headers={"Content-Type": "application/json"}), timeout=60)
    return json.load(r)["vector"]

V = {n: vec(t) for n, t in BIOS.items()}
sim = lambda a, b: sum(x*y for x, y in zip(V[a], V[b]))

pares = sorted(((sim(a,b), a, b) for a, b in itertools.combinations(BIOS, 2)), reverse=True)
print("=== MAS PARECIDOS ===")
for s, a, b in pares[:4]:
    print(f"  {s:.3f}  {a} + {b}")
print("=== MENOS PARECIDOS ===")
for s, a, b in pares[-4:]:
    print(f"  {s:.3f}  {a} + {b}")

print("\n=== ¿distingue lo que los campos no ven? ===")
print(f"  Javi(pide ayuda en banca) + Lucia(le da respeto el peso libre): {sim('Javi','Lucia'):.3f}")
print(f"  Javi(powerlifting)        + Alex(dias de fuerza)              : {sim('Javi','Alex'):.3f}")

print("\n=== ¿cruza idiomas? (la app es bilingue) ===")
en = vec("I'm a beginner and the free weights area still intimidates me.")
print(f"  Lucia(es) vs la misma frase en ingles: {sum(x*y for x,y in zip(V['Lucia'], en)):.3f}")
print(f"  Hugo(es)  vs esa frase inglesa       : {sum(x*y for x,y in zip(V['Hugo'], en)):.3f}")
