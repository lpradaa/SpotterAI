-- La direccion deja de ser obligatoria.
--
-- Hasta ahora los gimnasios solo los sembraba la demo, con direccion inventada.
-- Desde que una persona puede añadir el suyo desde el perfil escribiendo el
-- nombre, la columna obligaba a rellenarla con algo: cadena vacia o un texto
-- falso. Las dos opciones son peores que decir que no se sabe, porque cualquier
-- pantalla que enseñe la direccion tendria que adivinar cual de los dos casos
-- significa "no consta".
ALTER TABLE gimnasio MODIFY COLUMN direccion VARCHAR(255) DEFAULT NULL;
