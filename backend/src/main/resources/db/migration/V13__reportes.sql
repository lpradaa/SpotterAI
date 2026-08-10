-- Que la aplicacion se entere de que alguien se ha portado mal.
--
-- Bloquear te protege a ti; no hay forma de que la aplicacion sepa que una
-- misma persona se ha portado mal con varias. Esto es esa forma, y es
-- deliberadamente minima: sin estado de "resuelto", sin quien lo revisa, sin
-- moderacion detras todavia. Construir ese flujo entero para un proceso que
-- hoy no existe -cero usuarios, nadie moderando- seria construir la pantalla
-- antes que la necesidad. Lo que hace falta primero es que el hecho quede
-- escrito en algun sitio.
CREATE TABLE reporte (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    reportador_id BIGINT NOT NULL,
    reportado_id  BIGINT NOT NULL,

    -- Una de las categorias de MotivoReporte. Sin restriccion CHECK a
    -- proposito: anadir un motivo nuevo no debe pedir una migracion.
    motivo VARCHAR(40) NOT NULL,
    detalle VARCHAR(500) NULL,

    creado_en DATETIME NOT NULL,

    CONSTRAINT fk_reporte_reportador FOREIGN KEY (reportador_id) REFERENCES Usuario(id),
    CONSTRAINT fk_reporte_reportado  FOREIGN KEY (reportado_id)  REFERENCES Usuario(id)
) ENGINE=InnoDB;

-- Sin restriccion de unicidad: la misma pareja puede tener varios reportes de
-- motivos y fechas distintas, y cada uno es un hecho aparte.
CREATE INDEX idx_reporte_reportado ON reporte (reportado_id);
