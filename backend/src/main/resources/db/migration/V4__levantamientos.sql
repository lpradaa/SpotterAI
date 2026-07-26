-- Los levantamientos principales de cada persona, como dato y no como prosa.
--
-- Ya existia la tabla `hito`, que es texto libre con foto: sirve para contar
-- algo. Esto es otra cosa: la ficha tecnica que el motor puede cruzar para
-- saber si dos personas pueden cubrirse con la barra cargada. Un spotter que no
-- puede con tu peso no te sirve, y eso es seguridad, no preferencia.

CREATE TABLE levantamiento (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    -- Lista cerrada en el enum Ejercicio: "banca", "press banca" y "press de
    -- banca" escritos a mano son tres ejercicios distintos para una maquina, y
    -- la gracia de este dato es justamente poder compararlo.
    ejercicio     VARCHAR(30) NOT NULL,
    -- Kilos de la barra completa. Sin normalizar por peso corporal: para hacer
    -- de spotter importa cuanto pesa lo que hay que sujetar.
    peso          DOUBLE      NOT NULL,
    -- Un maximo a una repeticion y un peso de trabajo a diez son cosas muy
    -- distintas; sin esto no se sabria cual de las dos se esta mirando.
    repeticiones  INT         NOT NULL,
    usuario_id    BIGINT      NOT NULL,
    PRIMARY KEY (id),
    -- Un ejercicio, una marca. Dos filas de sentadilla para la misma persona no
    -- significan nada: o es la buena o esta desactualizada.
    CONSTRAINT uk_levantamiento_usuario_ejercicio UNIQUE (usuario_id, ejercicio),
    CONSTRAINT fk_levantamiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
