-- =============================================================================
--  El vector de la biografia
--  ---------------------------------------------------------------------------
--  El motor puntuaba ocho campos estructurados —horario, nivel, fuerza,
--  constancia, rutina, objetivo, gimnasio, edad— e ignoraba por completo lo
--  unico que la persona escribe sobre si misma. Y ahi esta lo que ninguna
--  casilla recoge: "todavia me da respeto la zona de peso libre", "me amoldo a
--  lo que haga falta", "necesito a alguien que pueda ayudarme en banca pesada".
--
--  Se guarda el vector, no el texto vuelto a procesar en cada calculo. El
--  embedding se computa una vez, al guardar el perfil; emparejar es entonces un
--  producto escalar en memoria y no cuesta ni una llamada de red. Es lo que
--  permite que el noveno factor no toque los 44 ms que costo bajar la consulta.
-- =============================================================================

ALTER TABLE usuario
    -- 384 numeros en coma flotante de 32 bits, en crudo y en big-endian: 1536
    -- bytes. En JSON serian unos 4 KB por persona y habria que parsearlos en
    -- cada comparacion. El formato esta documentado en VectorDeTexto, que es lo
    -- unico que lo escribe y lo lee.
    ADD COLUMN biografia_vector BLOB DEFAULT NULL,

    -- De que texto salio el vector. Sin esto no hay forma de saber si el vector
    -- guardado corresponde a la biografia actual o a una anterior: alguien
    -- edita su bio, el servicio de embeddings esta caido, y el vector viejo se
    -- queda describiendo a quien esa persona ya no dice ser. Guardando la huella
    -- del texto, un vector desfasado se detecta y se recalcula.
    ADD COLUMN biografia_vector_de VARCHAR(64) DEFAULT NULL;
