-- Poder quitarte a alguien de encima.
--
-- No habia forma. "Deshacer relacion" solo borraba la fila de la solicitud, asi
-- que la otra persona podia mandarte otra al segundo siguiente y te seguia
-- viendo en Explorar. En una aplicacion que le enseña a un desconocido en que
-- gimnasio entrenas y a que horas exactas, eso no es un hueco de comodidad.
--
-- Se elige bloquear y no denunciar. Una denuncia necesita a alguien que la lea
-- y aqui no hay nadie; un boton de denunciar sin moderacion detras es teatro, y
-- encima uno peligroso, porque quien lo pulsa cree haber hecho algo. Bloquear
-- funciona con cero personas al otro lado y lo aplica la propia aplicacion.
CREATE TABLE bloqueo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Quien bloquea y a quien. Es dirigido: que tu bloquees a alguien no
    -- significa que esa persona te haya bloqueado a ti, y desbloquear tiene que
    -- deshacer solo lo tuyo.
    bloqueador_id BIGINT NOT NULL,
    bloqueado_id  BIGINT NOT NULL,

    creado_en DATETIME NOT NULL,

    CONSTRAINT uk_bloqueo_par UNIQUE (bloqueador_id, bloqueado_id),
    CONSTRAINT fk_bloqueo_bloqueador FOREIGN KEY (bloqueador_id) REFERENCES Usuario(id),
    CONSTRAINT fk_bloqueo_bloqueado  FOREIGN KEY (bloqueado_id)  REFERENCES Usuario(id)
) ENGINE=InnoDB;

-- Los dos sentidos se consultan constantemente: al listar gente, al abrir una
-- ficha y al mandar una solicitud. La comprobacion es "hay bloqueo entre estos
-- dos, en cualquier direccion", asi que hace falta un indice por cada lado.
CREATE INDEX idx_bloqueo_bloqueador ON bloqueo (bloqueador_id);
CREATE INDEX idx_bloqueo_bloqueado  ON bloqueo (bloqueado_id);
