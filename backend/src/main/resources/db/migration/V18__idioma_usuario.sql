-- En que idioma se le escribe a cada persona.
--
-- Todo lo demas que traduce el backend sale de la cabecera Accept-Language que
-- manda el frontend en cada peticion. Los correos no pueden: los manda un
-- barrido que corre cada minuto por su cuenta, sin nadie preguntando y sin
-- ninguna peticion de la que sacar el idioma. Habia que guardarlo.
--
-- Se rellena solo: al registrarse se guarda el idioma con el que se llego, y
-- despues cada vez que alguien pulsa el selector. No hay ninguna pantalla donde
-- se elija esto por separado, y no deberia haberla — dos sitios para decidir lo
-- mismo acaban diciendo cosas distintas.
--
-- 'es' por defecto, y para todas las cuentas que ya existen: es el idioma en el
-- que esta escrita la aplicacion y el que usa el catalogo cuando falta una
-- traduccion. Quien tenga la aplicacion en ingles se corrige solo en cuanto
-- vuelva a entrar.
ALTER TABLE Usuario
    ADD COLUMN idioma VARCHAR(5) NOT NULL DEFAULT 'es'
        COMMENT 'Idioma en el que se le mandan los correos. Lo pone el selector.';
