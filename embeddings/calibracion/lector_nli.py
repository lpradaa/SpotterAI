# -*- coding: utf-8 -*-
"""
Leer una biografía con un modelo que entiende la negación.

## Qué problema resuelve

Comparar dos biografías con un coseno mide parecido de redacción y no
compatibilidad — medido en `evaluar_afinidad.py`—, y proyectarlas contra anclas
hereda el fallo entero: «busco a alguien que me exija» y «busco a alguien que no
me exija» caen en el mismo polo con la misma intensidad, porque comparten todo
el vocabulario.

Los dos fallan por lo mismo: un bi-encoder proyecta cada texto por separado, y
la negación no cambia mucho un vector construido a base de significados sueltos.

Un modelo de inferencia textual no funciona así. Recibe **dos** textos a la vez
—una premisa y una hipótesis— y decide si la primera implica la segunda, la
contradice, o ninguna cosa. Eso es exactamente la pregunta que hay que hacerle a
una biografía:

    premisa:    «Busco a alguien que no me exija, que respete si un día…»
    hipótesis:  «Esta persona busca un compañero que le exija.»
    respuesta:  contradicción

## Por qué esto sí cabe en la arquitectura

Antes descarté los modelos que ven dos textos juntos porque habría que
ejecutarlos por cada pareja de usuarios, y eso pone un modelo en el camino
crítico del emparejamiento. Es cierto para comparar dos biografías — y falso
para esto.

Aquí el modelo se ejecuta **una vez por persona, al guardar el perfil**, igual
que el embedding de hoy: una biografía contra las hipótesis de los ejes. Lo que
se guarda son tres números en vez de 384, y comparar a dos personas sigue siendo
aritmética sobre datos ya calculados.

## Lo que hay que medir antes de creerse nada

1. ¿Distingue la negación? Es para lo que está entrenado, pero hay que verlo.
2. ¿Cuánta memoria cuesta? El servicio entero ocupa hoy 475 MB tras un trabajo
   que costó bajar de 740, y estos pesos son casi tres veces los de ahora.
"""
from __future__ import annotations

import numpy as np
import onnxruntime as ort
from huggingface_hub import hf_hub_download
from tokenizers import Tokenizer

# El multilingüe entrenado en XNLI, en su variante int8: 317 MB de pesos frente
# a los 1.116 del original. El "pequeño" de la familia (MiniLMv2-L6) solo existe
# sin cuantizar y pesa 428, así que el grande cuantizado sale más barato.
REPO_NLI = "Xenova/mDeBERTa-v3-base-xnli-multilingual-nli-2mil7"
VARIANTE_NLI = "onnx/model_int8.onnx"

# El orden de las etiquetas es del modelo, no una convención: mDeBERTa-xnli
# saca (entailment, neutral, contradiction) y leerlas en otro orden daría
# resultados invertidos sin que nada fallara.
ETIQUETAS = ("implica", "neutral", "contradice")


class LectorNLI:
    """Pregunta cosas sobre un texto y contesta con probabilidades."""

    def __init__(self, max_tokens: int = 256) -> None:
        ruta_onnx = hf_hub_download(REPO_NLI, VARIANTE_NLI)
        ruta_tok = hf_hub_download(REPO_NLI, "tokenizer.json")

        self.tokenizador = Tokenizer.from_file(ruta_tok)
        self.tokenizador.enable_truncation(max_length=max_tokens)

        opciones = ort.SessionOptions()
        # Mismo criterio que el servicio de embeddings: una biografía cada vez
        # que alguien guarda su perfil, no un flujo continuo.
        opciones.intra_op_num_threads = 1
        opciones.inter_op_num_threads = 1

        self.sesion = ort.InferenceSession(
            ruta_onnx, opciones, providers=["CPUExecutionProvider"])
        self.entradas = {e.name for e in self.sesion.get_inputs()}

    def juzgar(self, premisa: str, hipotesis: str) -> dict[str, float]:
        """Qué relación hay entre lo que alguien escribió y una afirmación."""
        codificado = self.tokenizador.encode(premisa, hipotesis)

        ids = np.array([codificado.ids], dtype=np.int64)
        mascara = np.array([codificado.attention_mask], dtype=np.int64)

        entrada = {"input_ids": ids, "attention_mask": mascara}
        if "token_type_ids" in self.entradas:
            entrada["token_type_ids"] = np.array([codificado.type_ids], dtype=np.int64)
        # Este exportado NO los declara —sus entradas son input_ids y
        # attention_mask— y aun asi separa bien las dos frases: DeBERTa-v3 usa
        # posiciones relativas y el [SEP] que ya va dentro de los ids. Se
        # comprueba en vez de suponerse porque otros exportados del mismo
        # modelo si los piden, y pasar una entrada que el grafo no declara es
        # un error, no un aviso.

        logits = self.sesion.run(None, entrada)[0][0]
        exp = np.exp(logits - logits.max())
        probabilidades = exp / exp.sum()

        return dict(zip(ETIQUETAS, (float(p) for p in probabilidades)))

    def posicion_en_eje(self, texto: str, hipotesis: str) -> float:
        """
        De -1 a 1: cuánto sostiene el texto esa afirmación.

        Se resta la contradicción en vez de mirar solo la implicación porque las
        dos cosas importan y no son la misma: un texto que no habla del tema
        sale neutral en las dos y aquí da cero, que es lo correcto —no dice nada
        de este eje— mientras que mirando solo «implica» un texto neutro y uno
        que niega darían lo mismo.
        """
        r = self.juzgar(texto, hipotesis)
        return r["implica"] - r["contradice"]


# ---------------------------------------------------------------- los tres ejes

# Una hipótesis por eje, y el signo dice el lado: con NLI no hacen falta dos
# polos como con las anclas, porque «contradice» ya es el otro extremo.
#
# Están escritas en PRIMERA PERSONA y a propósito. Medido:
#
#   «Esta persona busca un compañero que le exija»   separa 0,046
#   «Busco que me exijan»                            separa 1,128
#
# El modelo no hace la correferencia entre «busco» y «esta persona busca», así
# que en tercera persona contesta neutral a todo y el eje muere. Veinticuatro
# veces de diferencia por reescribir la frase, y ninguna por cambiar de modelo.
EJES_NLI = {
    "qué busca del otro": "Busco que me exijan.",
    "ambición": "Entreno para competir.",
    "flexibilidad": "Me adapto a lo que entrene el otro.",
}

# Por debajo de esto, el texto no habla del eje. Es más alto que el de las
# anclas porque aquí el neutral es una respuesta explícita del modelo y no una
# distancia parecida entre dos polos.
UMBRAL_DE_SEÑAL = 0.15


def leer_ejes(lector: "LectorNLI", texto: str) -> dict:
    """La posición en cada eje, o None si el texto no dice nada de eso."""
    posiciones = {}
    for eje, hipotesis in EJES_NLI.items():
        p = lector.posicion_en_eje(texto, hipotesis)
        posiciones[eje] = None if abs(p) < UMBRAL_DE_SEÑAL else p
    return posiciones


def encaje(a: dict, b: dict):
    """Cuánto encajan dos lecturas. None si ningún eje sale en los dos."""
    evaluados = [1.0 - abs(a[e] - b[e]) / 2.0
                 for e in EJES_NLI
                 if a.get(e) is not None and b.get(e) is not None]
    return sum(evaluados) / len(evaluados) if evaluados else None
