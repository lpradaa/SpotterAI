-- Poder dejar de recibir los avisos por correo.
--
-- Hasta ahora la unica forma de apagarlos era una propiedad del servidor, o sea
-- para todo el mundo a la vez. Eso vale mientras el unico usuario seas tu; en
-- cuanto se registra alguien real, mandarle correo que no puede parar deja de
-- ser aceptable.
--
-- TRUE por defecto, tambien para los que ya existen: quien se registra en una
-- aplicacion de emparejar gente espera que le avisen cuando alguien le escribe.
-- Lo que no puede faltar es la salida, y la salida es esta columna.
ALTER TABLE Usuario
    ADD COLUMN avisos_por_correo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Si quiere recibir avisos por correo.';

-- La llave para darse de baja desde el propio correo, sin iniciar sesion.
--
-- Aleatoria y unica: es lo unico que acompana al enlace, asi que tiene que ser
-- imposible de adivinar y no puede derivarse del id, o cualquiera podria dar de
-- baja a cualquiera probando numeros. No sirve para nada mas que para esto: no
-- autentica, no abre sesion y no deja leer nada.
--
-- NULL hasta que hace falta. Se genera la primera vez que se le manda un aviso
-- a esa persona, que es el primer momento en que el enlace tiene que existir.
ALTER TABLE Usuario
    ADD COLUMN token_avisos VARCHAR(64) NULL,
    ADD CONSTRAINT uk_usuario_token_avisos UNIQUE (token_avisos);
