-- Esquema base de SpotterAI.
--
-- Refleja el estado al que se habia llegado con ddl-auto=update, que es lo que
-- se sustituye a partir de aqui. Los nombres de las claves ajenas son propios y
-- no los que generaba Hibernate (FK9ccq89yl97tpcvgek8bguks7r y compañia), que
-- no se pueden buscar ni leer en un error.
--
-- En una base que ya existia, Flyway la marca en esta version y no la ejecuta.
-- En una vacia, la crea entera.

CREATE TABLE gimnasio (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(255) NOT NULL,
    direccion     VARCHAR(255) NOT NULL,
    ciudad        VARCHAR(255) DEFAULT NULL,
    codigo_postal VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE usuario (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password     VARCHAR(255) DEFAULT NULL,
    edad         INT          DEFAULT NULL,
    genero       VARCHAR(255) DEFAULT NULL,
    peso         FLOAT        DEFAULT NULL,
    nivel        VARCHAR(255) DEFAULT NULL,
    objetivos    VARCHAR(255) DEFAULT NULL,
    avatar       VARCHAR(255) DEFAULT NULL,
    -- Texto libre del perfil. 280 aqui y 280 en el servidor al guardar.
    biografia    VARCHAR(280) DEFAULT NULL,
    -- Entrenamientos que se propone hacer a la semana.
    meta_semanal INT          DEFAULT NULL,
    gimnasio_id  BIGINT       DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_gimnasio FOREIGN KEY (gimnasio_id) REFERENCES gimnasio (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE disponibilidad (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    dia_semana  VARCHAR(255) NOT NULL,
    hora_inicio TIME         DEFAULT NULL,
    hora_fin    TIME         DEFAULT NULL,
    -- Si va seguro ("voy siempre") o solo podria ir. Es lo que separa
    -- compromiso de disponibilidad en el calculo de compatibilidad.
    habitual    BIT(1)       NOT NULL,
    usuario_id  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_disponibilidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE entrenamiento (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    fecha            DATE         NOT NULL,
    tipo             VARCHAR(255) NOT NULL,
    duracion_minutos INT          DEFAULT NULL,
    lugaronotas      VARCHAR(255) DEFAULT NULL,
    usuario_id       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_entrenamiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE solicitud (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    estado          VARCHAR(255) NOT NULL,
    fecha_solicitud DATETIME(6)  DEFAULT NULL,
    emisor_id       BIGINT       DEFAULT NULL,
    receptor_id     BIGINT       DEFAULT NULL,
    PRIMARY KEY (id),
    -- Hubo duplicados y se parchearon en Java. Esto los hace imposibles.
    -- Cubre el mismo par en el mismo sentido; el inverso (A pide a B y B pide a
    -- A) se comprueba en el servicio, porque expresarlo aqui necesita columnas
    -- generadas con LEAST/GREATEST.
    CONSTRAINT uk_solicitud_emisor_receptor UNIQUE (emisor_id, receptor_id),
    CONSTRAINT fk_solicitud_emisor FOREIGN KEY (emisor_id) REFERENCES usuario (id),
    CONSTRAINT fk_solicitud_receptor FOREIGN KEY (receptor_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE mensajes (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    contenido   TEXT        NOT NULL,
    fecha_envio DATETIME(6) DEFAULT NULL,
    -- Si el receptor ya abrio la conversacion despues de recibirlo.
    leido       BIT(1)      NOT NULL,
    emisor_id   BIGINT      NOT NULL,
    receptor_id BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mensaje_emisor FOREIGN KEY (emisor_id) REFERENCES usuario (id),
    CONSTRAINT fk_mensaje_receptor FOREIGN KEY (receptor_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- Indices para las consultas que se hacen en cada carga de pantalla.
CREATE INDEX ix_disponibilidad_usuario ON disponibilidad (usuario_id);
CREATE INDEX ix_entrenamiento_usuario ON entrenamiento (usuario_id);
CREATE INDEX ix_solicitud_receptor ON solicitud (receptor_id);
CREATE INDEX ix_mensajes_emisor ON mensajes (emisor_id);
-- El de receptor lleva leido detras porque el contador de sin leer filtra por
-- los dos a la vez.
CREATE INDEX ix_mensajes_receptor_leido ON mensajes (receptor_id, leido);
