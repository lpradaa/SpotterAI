# -*- coding: utf-8 -*-
"""
El modelo, sin PyTorch.

`sentence-transformers` es cómodo y arrastra PyTorch entero. Medido en este
proyecto, importar la librería —sin cargar todavía ningún peso— cuesta 405 MB, de
los cuales 168 son solo torch:

    python + numpy + onnxruntime + tokenizers ...  51 MB
    + torch ..............................  219 MB
    + transformers .......................  284 MB
    + sentence-transformers ..............  405 MB

Con el modelo cargado, el servicio se plantaba en 740 MB y no cabía en las capas
gratuitas de 512. Cuantizar a int8 no lo arregló —bajó los pesos de 470 MB a 113,
pero el proceso subió a 805, porque cargaba las dos pilas a la vez—.

Lo que sí lo arregla es quitar la pila. Aquí está la inferencia escrita a mano
contra ONNX Runtime: tokenizar, pasar por la red, promediar y normalizar. Son
cuatro pasos y ninguno necesita PyTorch.

Los números que salen de aquí tienen que ser IDÉNTICOS a los de
sentence-transformers, porque en la base de datos hay vectores calculados con
aquella pila y mezclar dos formas de proyectar el mismo texto haría que las
similitudes dejaran de significar nada. Lo comprueba `verificar_ligero.py`.
"""
from __future__ import annotations

import numpy as np
import onnxruntime as ort
from huggingface_hub import hf_hub_download
from tokenizers import Tokenizer

DIMENSIONES = 384


class ModeloLigero:
    """Un transformer de frases sin más dependencias que ONNX Runtime."""

    def __init__(self, repo: str, fichero_onnx: str, max_tokens: int = 128) -> None:
        # Solo los dos ficheros que hacen falta, no el repositorio entero: el
        # .onnx cuantizado y el tokenizador. Los pesos en float32 —que son la
        # mayor parte del peso del repo— no se descargan siquiera.
        ruta_onnx = hf_hub_download(repo, fichero_onnx)
        ruta_tok = hf_hub_download(repo, "tokenizer.json")

        self.tokenizador = Tokenizer.from_file(ruta_tok)
        self.tokenizador.enable_truncation(max_length=max_tokens)

        opciones = ort.SessionOptions()
        # Un hilo. Este servicio atiende una biografía cada vez que alguien
        # guarda su perfil, no un flujo continuo: varios hilos aquí solo suman
        # memoria y contención con el resto del contenedor.
        opciones.intra_op_num_threads = 1
        opciones.inter_op_num_threads = 1

        self.sesion = ort.InferenceSession(ruta_onnx, opciones, providers=["CPUExecutionProvider"])
        self.entradas = {e.name for e in self.sesion.get_inputs()}

    def vector(self, texto: str) -> np.ndarray:
        codificado = self.tokenizador.encode(texto)

        ids = np.array([codificado.ids], dtype=np.int64)
        mascara = np.array([codificado.attention_mask], dtype=np.int64)

        entrada = {"input_ids": ids, "attention_mask": mascara}
        # Algunos exportados piden token_type_ids y otros no. Pasar una entrada
        # que el grafo no declara es un error, asi que se mira lo que pide.
        if "token_type_ids" in self.entradas:
            entrada["token_type_ids"] = np.zeros_like(ids)

        # (1, tokens, 384): un vector por token, no uno por frase.
        por_token = self.sesion.run(None, entrada)[0]

        return _normalizar(_promediar(por_token, mascara))


def _promediar(por_token: np.ndarray, mascara: np.ndarray) -> np.ndarray:
    """
    De un vector por token a uno por frase.

    Promedio ponderado por la máscara, no media a secas: los tokens de relleno
    del final no son parte de la frase y meterlos en el promedio desplazaría el
    vector hacia el relleno — más cuanto más corta sea la biografía, que es
    justamente el caso de esta aplicación.

    Es lo mismo que hace la capa de pooling de sentence-transformers para este
    modelo, y por eso los números coinciden.
    """
    pesos = mascara[..., None].astype(np.float32)
    return (por_token * pesos).sum(axis=1) / np.clip(pesos.sum(axis=1), 1e-9, None)


def _normalizar(vector: np.ndarray) -> np.ndarray:
    """
    A longitud 1.

    Con dos vectores de longitud 1, la similitud del coseno es su producto
    escalar. Es lo que deja el lado Java como una suma de productos, sin raíces
    ni divisiones y sin poder dividir por cero.
    """
    return (vector / np.clip(np.linalg.norm(vector, axis=1, keepdims=True), 1e-9, None))[0]
