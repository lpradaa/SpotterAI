-- Lo que dice una biografia sobre como quiere entrenar quien la escribio.
--
-- Sustituye a `biografia_vector`, que guardaba 384 numeros opacos. Aquel factor
-- comparaba dos biografias con la similitud del coseno y resulto medir parecido
-- de redaccion en vez de compatibilidad: dos personas que querian lo contrario
-- dicho con la misma estructura puntuaban mas alto que dos que querian lo mismo
-- dicho con sus palabras. Esta medido en docs/medir-el-motor.md.
--
-- Son DOUBLE y no un JSON porque son tres numeros con nombre, no una estructura:
-- asi se pueden consultar, promediar y mirar a ojo en la base cuando algo no
-- cuadre, que es justo lo que no se podia hacer con el blob anterior.
--
-- NULL no es cero: es «esta persona no ha dicho nada de esto». La mitad de las
-- biografias reales no hablan de la mitad de los ejes, y el motor trata ese
-- hueco como cualquier otro dato que falta.
ALTER TABLE Usuario
    ADD COLUMN intencion_exigencia DOUBLE NULL
        COMMENT 'De -1 a 1. Positivo: busca que le exijan. Negativo: busca compania.',
    ADD COLUMN intencion_ambicion DOUBLE NULL
        COMMENT 'De -1 a 1. Positivo: entrena para competir. Negativo: para mantenerse.',
    ADD COLUMN intencion_flexibilidad DOUBLE NULL
        COMMENT 'De -1 a 1. Positivo: se adapta al otro. Negativo: tiene su plan.',
    ADD COLUMN intenciones_de VARCHAR(64) NULL
        COMMENT 'Huella del texto del que salieron, para saber si siguen al dia.';

-- La columna del vector se queda de momento, a proposito. Borrarla en la misma
-- migracion que crea las nuevas dejaria sin datos al factor entre el arranque y
-- el primer repaso, que es cuando se recalculan las intenciones de todos.
