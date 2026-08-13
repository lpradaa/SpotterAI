/**
 * Agrupar reportes por la persona reportada.
 *
 * La pantalla de moderación era una tabla plana ordenada por fecha, y una tabla
 * plana no responde la pregunta que se hace quien modera. Esa pregunta no es
 * «qué ha pasado y en qué orden», es «de quién hay un problema».
 *
 * La distinción que manda es entre «una persona se ha quejado tres veces» y
 * «tres personas se han quejado una vez»: la primera puede ser un conflicto
 * entre dos, la segunda es un patrón. Son cosas distintas, y ninguna lista
 * ordenada por fecha las separa.
 *
 * Vive aquí y no dentro del componente porque es una regla de producto —qué se
 * considera un patrón y qué se mira antes— y se prueba como tal, igual que
 * `compatibilidad.ts`.
 */

/** Un reporte tal y como lo manda GET /api/reportes. */
export interface Reporte {
  id: number;
  reportadorNombre: string;
  reportadorEmail: string;
  reportadoId: number;
  reportadoNombre: string;
  reportadoEmail: string;
  motivo: string;
  detalle: string;
  creadoEn: string;
  /** Si alguien con acceso ya lo dio por visto. */
  revisado: boolean;
}

/** Todos los reportes que ha recibido una misma persona. */
export interface CasoDeModeracion {
  /** El id de quien ha sido reportado, para poder ir a verlo. */
  id: number;
  nombre: string;
  email: string;
  /** Sus reportes, del más reciente al más antiguo. */
  reportes: Reporte[];
  sinRevisar: number;
  /** Cuántas personas distintas lo han reportado. */
  denunciantes: number;
}

/**
 * Los reportes agrupados por persona reportada, en el orden en que conviene
 * mirarlos.
 *
 * El orden pone delante lo que pide una decisión: primero los casos con algo
 * sin revisar y, dentro de esos, los que ha reportado más gente distinta. Un
 * caso ya revisado del todo baja al final aunque sea de hoy, porque ya se ha
 * mirado — pero no desaparece: un reporte revisado sigue siendo información,
 * sobre todo si la misma persona acumula varios.
 *
 * @param revisado cómo saber si un reporte ya se ha mirado. Se pasa desde fuera
 *   porque la pantalla cuenta también los que se han marcado en esta sesión y
 *   que el servidor todavía no devuelve como vistos.
 */
export function agruparEnCasos(
  reportes: readonly Reporte[],
  revisado: (r: Reporte) => boolean,
): CasoDeModeracion[] {

  const porPersona = new Map<number, CasoDeModeracion>();

  for (const r of reportes) {
    let caso = porPersona.get(r.reportadoId);
    if (!caso) {
      caso = {
        id: r.reportadoId,
        nombre: r.reportadoNombre,
        email: r.reportadoEmail,
        reportes: [],
        sinRevisar: 0,
        denunciantes: 0,
      };
      porPersona.set(r.reportadoId, caso);
    }
    caso.reportes.push(r);
  }

  for (const caso of porPersona.values()) {
    caso.reportes.sort((a, b) => b.creadoEn.localeCompare(a.creadoEn));
    caso.sinRevisar = caso.reportes.filter(r => !revisado(r)).length;
    // Por correo y no por nombre: dos personas pueden llamarse igual, y contar
    // de más aquí convierte un conflicto entre dos en un patrón.
    caso.denunciantes = new Set(caso.reportes.map(r => r.reportadorEmail)).size;
  }

  return [...porPersona.values()].sort((a, b) => {
    const pendiente = Number(b.sinRevisar > 0) - Number(a.sinRevisar > 0);
    if (pendiente !== 0) return pendiente;

    if (b.denunciantes !== a.denunciantes) return b.denunciantes - a.denunciantes;

    return b.reportes[0].creadoEn.localeCompare(a.reportes[0].creadoEn);
  });
}
