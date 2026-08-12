-- Marcar un reporte como visto.
--
-- El panel solo acumulaba: a los diez reportes deja de ser legible, y quien
-- modera no tiene forma de saber que ha mirado ya. Eso convierte una lista que
-- crece en una lista que se ignora, que es peor que no tenerla — porque la
-- gente que reporta sigue creyendo que alguien lo lee.
--
-- No es "resuelto" ni "sancionado": es "visto". Lo que se haga despues pasa
-- fuera de la aplicacion, y fingir aqui un flujo de sanciones que no existe
-- seria el mismo teatro que se evito al no poner un boton de denunciar sin
-- nadie detras.
ALTER TABLE reporte
    ADD COLUMN revisado_en DATETIME NULL,
    ADD COLUMN revisado_por VARCHAR(255) NULL;
