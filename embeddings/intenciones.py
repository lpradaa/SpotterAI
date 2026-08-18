# -*- coding: utf-8 -*-
"""
Qué dice una biografía sobre cómo quiere entrenar quien la escribió.

## Por qué esto y no un embedding

El factor semántico comparaba dos biografías con la similitud del coseno, y eso
resultó medir **parecido de redacción y no compatibilidad**: dos personas que
quieren lo contrario dicho con la misma estructura sacaban 0,843 y dos que
quieren lo mismo dicho con sus palabras, 0,499. Está medido en
`calibracion/evaluar_afinidad.py` y contado en `docs/medir-el-motor.md`.

No se arregla con otro modelo de la misma clase —se probó `multilingual-e5-small`
y sale peor en proporción a su rango— porque el fallo es estructural: un
bi-encoder proyecta cada texto por separado, y la oposición entre dos frases no
es propiedad de ninguna de las dos, es de la pareja.

## Cómo funciona

Un modelo de inferencia textual recibe **dos** textos y dice si el primero
implica al segundo, lo contradice, o ninguna cosa. Así que en vez de comparar dos
biografías entre sí, a cada una se le hacen tres preguntas:

    premisa:    «Busco a alguien que no me exija, que respete si un día…»
    hipótesis:  «Busco que me exijan.»
    respuesta:  contradicción  ->  posición -0,51 en ese eje

Lo que se guarda son tres números con nombre, no 384 opacos. Y comparar a dos
personas vuelve a ser aritmética: restar posiciones.

## Por qué esto sí cabe donde un cross-encoder no

Porque el modelo lee **una** biografía, no compara dos. Corre una vez por persona
al guardar el perfil —igual que el embedding de antes— y no en el camino crítico
del emparejamiento. Si el servicio se cae, el motor sigue puntuando con los ejes
ya guardados y solo deja de releer las biografías que cambien.
"""
from __future__ import annotations

import numpy as np
import onnxruntime as ort
from huggingface_hub import hf_hub_download
from tokenizers import Tokenizer

# Multilingüe entrenado en XNLI, cuantizado a int8: 317 MB de pesos frente a los
# 1.116 del original. El "pequeño" de la familia (MiniLMv2-L6) solo existe sin
# cuantizar y pesa 428, así que el grande cuantizado sale más barato.
#
# El servicio pasa de 484 MB a 611 con este cambio. Es caro y se paga a
# sabiendas: el factor anterior ordenaba al revés de lo que decía ordenar.
REPO = "Xenova/mDeBERTa-v3-base-xnli-multilingual-nli-2mil7"
VARIANTE = "onnx/model_int8.onnx"

# El orden es del modelo, no una convención. Leerlas al revés daría resultados
# invertidos sin que nada fallara.
ETIQUETAS = ("implica", "neutral", "contradice")

# Una hipótesis por eje. Con inferencia textual no hacen falta dos polos, porque
# «contradice» ya es el otro extremo.
#
# Están en PRIMERA PERSONA a propósito, y es el detalle que hizo funcionar esto:
#
#     «Esta persona busca un compañero que le exija»   separa 0,046
#     «Busco que me exijan»                            separa 1,128
#
# El modelo no hace la correferencia entre «busco» y «esta persona busca», así
# que en tercera persona contesta neutral a todo y el eje muere. Veinticuatro
# veces de diferencia por reescribir la frase — ninguna mejora de este factor ha
# venido de cambiar de modelo, todas de cambiar la pregunta.
#
# Y son tres, no diez. Salen de leer seguidas las biografías que hay en la base:
# casi todas hablan de horario y rutina, que ya son campos del perfil y el motor
# ya cruza. Esto es lo que el texto libre aporta y no cabe en un desplegable.
EJES = {
    "exigencia": "Busco que me exijan.",
    "ambicion": "Entreno para competir.",
    "flexibilidad": "Me adapto a lo que entrene el otro.",
}

# Por debajo de esto, el texto no habla del eje y no se inventa una posición.
# Es el mismo trato que da el motor a un campo vacío: media biografía no dice
# nada de la mitad de los ejes, y eso es un dato, no un cero.
#
# MEDIDO, no elegido a ojo — el primero fue 0,15 a ojo y dejaba el factor
# evaluable en 9 de 78 parejas reales. Con `calibracion/calibrar_umbral.py`:
#
#     umbral   cobertura   separacion   ¿acierta?
#       0,02       89,7 %       0,058      si
#       0,05       62,8 %       0,091      si
#       0,08       23,1 %       0,275      si
#       0,15       11,5 %       0,283      si
#
# Lo importante es que acierta en TODOS: bajar el umbral no invierte el orden,
# solo deja menos separacion. Asi que la eleccion es cuanta cobertura se compra
# con cuanta señal, y 0,05 la pone al nivel de los factores decentes del motor
# —dos de cada tres parejas— sin dejar de ordenar bien.
#
# Un factor de 6 puntos que opina en dos de cada tres parejas con señal moderada
# mueve mas decisiones acertadas que uno que opina en una de cada nueve. Y el
# peso pequeño es justo lo que permite ese cambio sin arriesgar mucho.
UMBRAL_DE_SEÑAL = 0.05


class LectorDeIntenciones:
    """Lee una biografía y la coloca en los tres ejes."""

    def __init__(self, max_tokens: int = 256) -> None:
        ruta_onnx = hf_hub_download(REPO, VARIANTE)
        ruta_tok = hf_hub_download(REPO, "tokenizer.json")

        self.tokenizador = Tokenizer.from_file(ruta_tok)
        self.tokenizador.enable_truncation(max_length=max_tokens)

        opciones = ort.SessionOptions()
        # Mismo criterio que el modelo anterior: esto atiende una biografía cada
        # vez que alguien guarda su perfil, no un flujo continuo.
        opciones.intra_op_num_threads = 1
        opciones.inter_op_num_threads = 1

        self.sesion = ort.InferenceSession(
            ruta_onnx, opciones, providers=["CPUExecutionProvider"])
        self.entradas = {e.name for e in self.sesion.get_inputs()}

    def juzgar(self, premisa: str, hipotesis: str) -> dict[str, float]:
        codificado = self.tokenizador.encode(premisa, hipotesis)

        ids = np.array([codificado.ids], dtype=np.int64)
        entrada = {
            "input_ids": ids,
            "attention_mask": np.array([codificado.attention_mask], dtype=np.int64),
        }
        # Este exportado no declara token_type_ids y aun así separa bien las dos
        # frases: DeBERTa-v3 usa posiciones relativas y el [SEP] que ya va dentro
        # de los ids. Se comprueba en vez de suponerse porque otros exportados
        # del mismo modelo sí los piden, y pasar una entrada que el grafo no
        # declara es un error, no un aviso.
        if "token_type_ids" in self.entradas:
            entrada["token_type_ids"] = np.array([codificado.type_ids], dtype=np.int64)

        logits = self.sesion.run(None, entrada)[0][0]
        exp = np.exp(logits - logits.max())
        return dict(zip(ETIQUETAS, (float(p) for p in exp / exp.sum())))

    def posicion(self, texto: str, hipotesis: str) -> float:
        """
        De -1 a 1: cuánto sostiene el texto esa afirmación.

        Se resta la contradicción en vez de mirar solo la implicación porque las
        dos cosas importan: un texto que no habla del tema sale neutral en las
        dos y aquí da cero, que es lo correcto. Mirando solo «implica», un texto
        neutro y uno que niega darían lo mismo.
        """
        r = self.juzgar(texto, hipotesis)
        return r["implica"] - r["contradice"]

    def leer(self, texto: str) -> dict[str, float | None]:
        """Los tres ejes. None en los que el texto no dice nada."""
        leidos = {}
        for eje, hipotesis in EJES.items():
            p = self.posicion(texto, hipotesis)
            leidos[eje] = None if abs(p) < UMBRAL_DE_SEÑAL else round(p, 4)
        return leidos
