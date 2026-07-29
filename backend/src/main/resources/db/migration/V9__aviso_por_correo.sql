-- Cuando se avisó por correo de esto, si se avisó.
--
-- Es la marca que impide mandar dos veces el mismo aviso. El barrido que los
-- manda corre cada minuto y tiene que poder reiniciarse, caerse a medias o
-- ejecutarse en dos instancias sin que a nadie le lleguen cuatro correos de la
-- misma solicitud: lo que decide si ya se aviso no es una variable en memoria,
-- es esta columna.
--
-- NULL es "todavia no". Todo lo que ya existe entra asi a proposito: son
-- solicitudes y propuestas de hace dias, y avisar hoy de algo que lleva una
-- semana esperando no es un aviso, es ruido. El barrido las descarta por su
-- propio limite de antiguedad.
ALTER TABLE Solicitud
    ADD COLUMN avisado_en DATETIME NULL
        COMMENT 'Cuando se mando el aviso por correo. NULL = no se ha mandado.';

ALTER TABLE Sesion
    ADD COLUMN avisado_en DATETIME NULL
        COMMENT 'Cuando se mando el aviso por correo. NULL = no se ha mandado.';

-- El barrido busca por estado y antiguedad, y lo hace cada minuto para siempre.
CREATE INDEX idx_solicitud_por_avisar ON Solicitud (estado, avisado_en);
CREATE INDEX idx_sesion_por_avisar ON Sesion (estado, avisado_en);
