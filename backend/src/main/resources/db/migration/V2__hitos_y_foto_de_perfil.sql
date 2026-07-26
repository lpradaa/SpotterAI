-- Contenido escrito por las personas, no calculado por el motor.
--
-- Hasta ahora todo lo que se leia de un perfil ajeno lo producia la calculadora
-- de compatibilidad: un porcentaje y una frase generada. Un hito es lo primero
-- que escribe alguien para que lo lean los demas.

CREATE TABLE hito (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    titulo      VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) DEFAULT NULL,
    fecha       DATE         NOT NULL,
    -- Ruta servida por la aplicacion, no el binario: meter ficheros en la base
    -- la hincha y complica cualquier copia de seguridad.
    medio_url   VARCHAR(255) DEFAULT NULL,
    -- "imagen" o "video". Se guarda en vez de deducirlo de la extension, que
    -- puede mentir.
    medio_tipo  VARCHAR(20)  DEFAULT NULL,
    usuario_id  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_hito_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- El perfil se pinta ordenado por fecha y filtrado por persona.
CREATE INDEX ix_hito_usuario_fecha ON hito (usuario_id, fecha DESC);

-- Foto de perfil. Es opcional: sin ella se siguen usando las iniciales sobre
-- color, que identifican igual de bien y no necesitan que nadie suba nada.
ALTER TABLE usuario ADD COLUMN foto_url VARCHAR(255) DEFAULT NULL;
