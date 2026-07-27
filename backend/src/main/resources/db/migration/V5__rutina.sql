-- Como reparte cada uno los grupos musculares a lo largo de la semana.
--
-- El objetivo (hipertrofia, fuerza...) dice que busca alguien; la rutina dice
-- que toca el martes. Coincidir en horario con quien ese dia hace pierna
-- mientras tu haces pecho es coincidir en el gimnasio, no entrenar juntos.
--
-- Valores del enum Rutina: FULL_BODY, TORSO_PIERNA, EMPUJE_TIRON_PIERNA,
-- WEIDER, LIBRE. Nulo significa que la persona todavia no lo ha dicho, que no
-- es lo mismo que LIBRE: eso ultimo es una respuesta.
ALTER TABLE usuario ADD COLUMN rutina VARCHAR(30) DEFAULT NULL;
