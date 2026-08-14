# -*- coding: utf-8 -*-
"""
Servicio de embeddings de SpotterAI.

Convierte una biografia en un vector de 384 numeros. Es lo unico de este
proyecto que ejecuta un modelo, y esta aparte del backend a proposito:

  - Un transformer multilingue son ~500 MB de dependencia y de memoria. Meterlo
    dentro del Spring Boot obliga a dimensionar el backend entero por el modelo,
    cuando el backend no lo necesita para nada mas.

  - El embedding se calcula al *guardar* un perfil, no al emparejar. Emparejar
    es un producto escalar sobre vectores ya guardados. Asi que si este servicio
    esta caido, el motor sigue funcionando entero y solo se queda sin recalcular
    las biografias que cambien mientras tanto — un caso que la calculadora ya
    sabe tratar, porque es el mismo "no tenemos ese dato" que ya maneja para el
    gimnasio o el nivel.

  - Y es como se sirve un modelo de verdad: un servicio con su propio ciclo de
    vida, su propia escala y su propio despliegue.

El modelo es multilingue porque la aplicacion lo es: las biografias estan en
español, la interfaz puede estar en ingles, y un modelo entrenado solo en ingles
trataria "me da respeto el peso libre" como ruido.
"""

import hashlib
import os

from fastapi import FastAPI
from pydantic import BaseModel, Field

from modelo_ligero import DIMENSIONES, ModeloLigero

# Multilingue y pequeño. Hay modelos mejores, y todos son mas grandes: este cabe
# en las capas gratuitas donde va a vivir, que es la restriccion real.
NOMBRE_DEL_MODELO = os.getenv("MODELO", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")

# La variante cuantizada a 8 bits. Los pesos ocupan 113 MB en vez de 470 y, junto
# con la inferencia sin PyTorch de modelo_ligero, dejan el servicio en 461 MB —
# medido— frente a los 740 de la primera version.
#
# Cambiar esto obliga a regenerar los vectores sembrados y a volver a pasar la
# calibracion: dos variantes distintas proyectan el mismo texto a sitios
# distintos, y mezclarlas en la misma columna haria que las similitudes no
# significaran nada. Para ARM: onnx/model_qint8_arm64.onnx.
VARIANTE = os.getenv("VARIANTE_MODELO", "onnx/model_qint8_avx512_vnni.onnx")

app = FastAPI(title="SpotterAI · embeddings")

# Se carga una vez al arrancar, no por peticion. Cargarlo por peticion son ocho
# segundos cada vez y el mismo modelo en memoria varias veces.
modelo = ModeloLigero(NOMBRE_DEL_MODELO, VARIANTE)


class Peticion(BaseModel):
    texto: str = Field(min_length=1, max_length=2000)


class Respuesta(BaseModel):
    vector: list[float]
    """El vector, ya normalizado a longitud 1."""

    huella: str
    """
    De que texto salio. El backend la guarda junto al vector para saber si el
    vector que tiene corresponde a la biografia actual o a una anterior.
    """

    dimensiones: int


@app.get("/salud")
def salud() -> dict:
    """Para que el backend —y la plataforma— sepan si el modelo esta cargado."""
    return {"estado": "listo", "modelo": NOMBRE_DEL_MODELO,
            "variante": VARIANTE, "dimensiones": DIMENSIONES}


@app.post("/vector", response_model=Respuesta)
def vector(peticion: Peticion) -> Respuesta:
    texto = peticion.texto.strip()

    # Ya viene normalizado de modelo_ligero: la similitud del coseno entre dos
    # vectores de longitud 1 es su producto escalar, y eso deja el lado Java
    # como una suma de productos sin raices cuadradas ni divisiones.
    embedding = modelo.vector(texto)

    return Respuesta(
        vector=embedding.tolist(),
        huella=hashlib.sha256(texto.encode("utf-8")).hexdigest()[:32],
        dimensiones=len(embedding),
    )
