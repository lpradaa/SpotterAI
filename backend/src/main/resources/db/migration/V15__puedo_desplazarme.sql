-- Si esta persona se desplazaria a otro gimnasio para entrenar acompañada.
--
-- Es el unico dato del emparejamiento que el sistema no puede deducir de nada:
-- ni del horario, ni del gimnasio, ni del historial. Solo lo sabe quien lo
-- decide. Hasta ahora, entrenar en gimnasios distintos multiplicaba por 0,25 el
-- solape de horario —el factor que mas pesa— sin ninguna salida posible, asi que
-- alguien dispuesto a coger el metro tres paradas puntuaba igual que alguien que
-- no piensa moverse.
--
-- Por defecto FALSE y no TRUE: darlo por hecho seria suponer que todo el mundo
-- se desplaza, que es justo lo contrario de lo que pasa.
ALTER TABLE usuario
    ADD COLUMN puedo_desplazarme BOOLEAN NOT NULL DEFAULT FALSE;
