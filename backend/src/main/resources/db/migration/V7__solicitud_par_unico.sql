-- Una sola solicitud por pareja, la mande quien la mande.
--
-- La restriccion sobre (emisor, receptor) ya existia desde V1, pero solo cubre
-- el mismo par en el mismo sentido: A pidiendo a B y B pidiendo a A pasaban las
-- dos. Eso se comprobaba en SolicitudServiceImpl con dos consultas, y una
-- comprobacion en Java no para dos peticiones simultaneas: las dos leen que no
-- hay nada y las dos insertan.
--
-- El comentario de la entidad decia que expresarlo en la base "pide una
-- migracion de verdad, no ddl-auto=update". Ya las hay, asi que se acabo la
-- excusa.

-- 1. Primero hay que dejar la tabla en condiciones de aceptarla. Si ya hay
--    pares invertidos —y los hay, porque nada lo impedia— el indice fallaria al
--    crearse y la migracion dejaria la base a medias.
--
--    De cada pareja duplicada se conserva una sola fila, y no al azar: primero
--    la aceptada, porque es la que refleja una relacion que existe de verdad;
--    entre iguales, la mas antigua, que es la que lleva mas tiempo en pie.
DELETE s FROM solicitud s
  JOIN solicitud otra
    ON otra.emisor_id = s.receptor_id
   AND otra.receptor_id = s.emisor_id
 WHERE (CASE s.estado    WHEN 'ACEPTADA' THEN 0 WHEN 'PENDIENTE' THEN 1 ELSE 2 END,  s.id)
     > (CASE otra.estado WHEN 'ACEPTADA' THEN 0 WHEN 'PENDIENTE' THEN 1 ELSE 2 END, otra.id);

-- 2. Dos columnas calculadas que ordenan el par siempre igual. Con ellas, "A y
--    B" y "B y A" producen exactamente la misma clave, que es lo que permite
--    que un indice unico las vea como lo que son: la misma relacion.
--
--    Generadas y no rellenadas a mano: asi no hay forma de que se desincronicen
--    con las columnas de las que salen, ni de que un INSERT se olvide de ellas.
ALTER TABLE solicitud
  ADD COLUMN usuario_menor BIGINT AS (LEAST(emisor_id, receptor_id)) STORED,
  ADD COLUMN usuario_mayor BIGINT AS (GREATEST(emisor_id, receptor_id)) STORED;

ALTER TABLE solicitud
  ADD CONSTRAINT uk_solicitud_pareja UNIQUE (usuario_menor, usuario_mayor);
