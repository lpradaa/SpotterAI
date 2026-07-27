-- Sesiones: el plan concreto para entrenar juntos.
--
-- Hasta aqui el recorrido acababa en un porcentaje y un chat abierto. Esta
-- tabla es la que convierte "encajais al 90 %" en "el lunes a las seis".
--
-- No lleva restriccion unica sobre el par: dos personas pueden quedar muchas
-- veces, y hasta tener una propuesta viva mientras arrastran varias rechazadas.
-- Lo que si esta acotado, y en el servicio, es que no haya dos propuestas
-- pendientes a la vez entre los mismos: eso no es un dato imposible, es una
-- conversacion confusa.
CREATE TABLE sesion (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    proponente_id         BIGINT       NOT NULL,
    invitado_id           BIGINT       NOT NULL,
    fecha                 DATE         NOT NULL,
    hora_inicio           TIME         NOT NULL,
    hora_fin              TIME         NOT NULL,
    gimnasio_id           BIGINT       NULL,
    estado                VARCHAR(30)  NOT NULL DEFAULT 'PROPUESTA',
    nota                  VARCHAR(140) NULL,
    creada_en             DATETIME     NOT NULL,
    respondida_en         DATETIME     NULL,

    -- Una por cabeza: que el otro diga que entrenasteis no prueba que tu fueras.
    confirmada_proponente BOOLEAN      NOT NULL DEFAULT FALSE,
    confirmada_invitado   BOOLEAN      NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),
    CONSTRAINT fk_sesion_proponente FOREIGN KEY (proponente_id) REFERENCES usuario (id),
    CONSTRAINT fk_sesion_invitado   FOREIGN KEY (invitado_id)   REFERENCES usuario (id),
    CONSTRAINT fk_sesion_gimnasio   FOREIGN KEY (gimnasio_id)   REFERENCES gimnasio (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Las dos consultas que se hacen de verdad: "mis sesiones" mira las dos
-- columnas de participante, siempre filtrando por estado.
CREATE INDEX ix_sesion_proponente ON sesion (proponente_id, estado);
CREATE INDEX ix_sesion_invitado   ON sesion (invitado_id, estado);
