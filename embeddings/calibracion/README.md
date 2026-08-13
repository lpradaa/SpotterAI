# Calibración del factor de afinidad

`medir_afinidad.py` pasa las biografías reales por el modelo e imprime la
similitud de todas las parejas. Sirve para una cosa concreta: **fijar el suelo y
el techo del factor con datos, no a ojo.**

La primera versión del factor llevaba 0,30 y 0,75, elegidos por intuición. Al
medir, el rango real entre las 21 parejas resultó ser **0,085 – 0,544**: con
aquellos umbrales, la mejor pareja real se habría quedado en la mitad del factor
y la mayoría en cero. Un factor prácticamente inerte. Los valores que hay ahora
en `CalculadoraCompatibilidad` —0,15 y 0,55— salen de esta medición.

## Cuándo hay que volver a ejecutarlo

- **Al cambiar de modelo.** Los umbrales son de *este* modelo. Otro produce otra
  escala y los números dejan de significar lo mismo.
- Al cambiar el idioma o el estilo de las biografías de referencia.

```bash
uvicorn servidor:app --port 8000          # en el directorio de arriba
python calibracion/medir_afinidad.py
```
