-- Una franja repetida contaba dos veces.
--
-- Se descubrio al montar el perfil publico: las franjas en comun salian
-- duplicadas en la respuesta. El origen estaba en la base, con la misma franja
-- guardada varias veces para la misma persona, y la consecuencia no era
-- cosmetica: CalculadoraSolape suma los minutos de cada par, asi que un
-- duplicado inflaba directamente el numero que la aplicacion vende. Once
-- usuarios enseñaban el doble de horas en comun de las que tenian.
--
-- Nadie lo habria notado leyendo el codigo, porque el calculo es correcto: lo
-- que estaba mal era darle dos veces el mismo dato.

-- 1. Quitar las copias. Se conserva la de menor id; se comprobo antes que
--    ningun grupo duplicado difiere en habitual, o sea que las copias son
--    equivalentes y da igual cual se quede.
DELETE d FROM disponibilidad d
JOIN (
    SELECT MIN(id) AS conservar, usuario_id, dia_semana, hora_inicio, hora_fin
    FROM disponibilidad
    GROUP BY usuario_id, dia_semana, hora_inicio, hora_fin
) k
  ON  k.usuario_id  = d.usuario_id
  AND k.dia_semana  = d.dia_semana
  -- <=> y no =: las horas admiten null y con = una fila nula nunca casaria
  -- consigo misma, dejando duplicados sin borrar.
  AND k.hora_inicio <=> d.hora_inicio
  AND k.hora_fin    <=> d.hora_fin
WHERE d.id > k.conservar;

-- 2. Que no vuelva a pasar. Guardar dos veces la misma franja no significa
--    nada: o puedes ir a esa hora o no puedes.
ALTER TABLE disponibilidad
    ADD CONSTRAINT uk_disponibilidad_franja
    UNIQUE (usuario_id, dia_semana, hora_inicio, hora_fin);
