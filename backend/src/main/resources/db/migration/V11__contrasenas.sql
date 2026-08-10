-- Recuperar la contraseña, cambiarla, y poder echar a las sesiones abiertas.
--
-- Hasta ahora quien olvidaba su contraseña se quedaba fuera para siempre y sin
-- ningun recurso: no habia forma de recuperarla ni de cambiarla.

-- Desde cuando valen los tokens de esta persona.
--
-- Un JWT no se puede retirar: esta firmado y vale hasta que caduca, lo tenga
-- quien lo tenga. Esta marca es la forma barata de invalidarlos igualmente —el
-- filtro rechaza los emitidos antes— y hace falta justo donde mas importa: al
-- cambiar la contraseña. Si alguien te habia robado la sesion, cambiarla sin
-- esto no lo echa; sigue dentro 24 horas.
--
-- NULL es "nunca se ha revocado nada", que es el caso de todo el mundo hasta
-- que cambie su contraseña por primera vez.
ALTER TABLE Usuario
    ADD COLUMN sesiones_validas_desde DATETIME NULL
        COMMENT 'Los tokens emitidos antes de esta fecha se rechazan.';

-- El token para restablecer, GUARDADO EN HASH.
--
-- Aqui si y en la llave de baja no, y la diferencia no es capricho: con la
-- llave de baja, quien lea la base puede dejar a alguien sin correos. Con esta,
-- puede entrar en su cuenta. Un token de restablecimiento es una credencial en
-- toda regla mientras vive, asi que en la base se guarda su huella y el valor
-- de verdad solo existe dentro del correo que se manda.
ALTER TABLE Usuario
    ADD COLUMN token_reset CHAR(64) NULL,
    ADD COLUMN token_reset_expira DATETIME NULL,
    ADD CONSTRAINT uk_usuario_token_reset UNIQUE (token_reset);
