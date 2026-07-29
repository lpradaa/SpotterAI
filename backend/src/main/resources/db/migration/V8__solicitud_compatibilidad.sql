-- La compatibilidad que habia cuando se mando la solicitud.
--
-- Se congela al crearla y no se vuelve a tocar. No es cache: es que el numero
-- de hoy no sirve para saber que viste tu aquel dia. La constancia lo mueve
-- sola cada semana —depende de cuanto ha ido cada uno en los ultimos 28 dias—
-- y cada vez que se reajustan los pesos del motor los historicos dejan de ser
-- comparables entre si. Sin congelarlo, la pregunta "¿las solicitudes con mas
-- de 80 % acaban en mas entrenamientos?" no se puede responder hacia atras.
--
-- NULL a proposito para lo que ya estaba: rellenarlo con la puntuacion de hoy
-- seria inventarse el dato, y un dato inventado es peor que uno ausente porque
-- el que lee no sabe que lo es. El embudo las descarta y dice cuantas descarto.
ALTER TABLE Solicitud
    ADD COLUMN compatibilidad INT NULL
        COMMENT 'Puntuacion 0-100 en el momento de enviarla. NULL = anterior a que se guardara.';
