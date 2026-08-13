import { agruparEnCasos, Reporte } from './casos-de-moderacion';

/**
 * La regla de la pantalla de moderación.
 *
 * Lo que se fija aquí no es que agrupe, es *qué separa*: la diferencia entre
 * una persona que se ha quejado tres veces y tres personas que se han quejado
 * una vez. La primera puede ser un conflicto entre dos, la segunda es un
 * patrón, y son las dos cosas que quien modera necesita distinguir antes de
 * hacer nada. Si esa cuenta se hiciera por nombre en vez de por correo, o si un
 * caso ya revisado adelantara a uno pendiente, la pantalla seguiría
 * "funcionando" y dejaría de decir lo único que tiene que decir.
 */
describe('agruparEnCasos', () => {

  let siguiente = 0;

  function reporte(parcial: Partial<Reporte> & { reportadoId: number }): Reporte {
    siguiente += 1;
    return {
      id: siguiente,
      reportadorNombre: 'Quien sea',
      reportadorEmail: `alguien${siguiente}@ejemplo.test`,
      reportadoNombre: 'Persona ' + parcial.reportadoId,
      reportadoEmail: `persona${parcial.reportadoId}@ejemplo.test`,
      motivo: 'OTRO',
      detalle: '',
      creadoEn: '2026-08-01T10:00:00',
      revisado: false,
      ...parcial,
    };
  }

  /** Lo que dice el servidor, sin marcados de esta sesión. */
  const comoVino = (r: Reporte) => r.revisado;

  beforeEach(() => { siguiente = 0; });

  // ===================== Agrupar =====================

  it('junta en un caso los reportes de la misma persona', () => {
    const casos = agruparEnCasos([
      reporte({ reportadoId: 7 }),
      reporte({ reportadoId: 9 }),
      reporte({ reportadoId: 7 }),
    ], comoVino);

    expect(casos.length).toBe(2);
    expect(casos.find(c => c.id === 7)!.reportes.length).toBe(2);
  });

  it('dentro de un caso, el reporte más reciente va primero', () => {
    const casos = agruparEnCasos([
      reporte({ reportadoId: 7, creadoEn: '2026-08-01T10:00:00', motivo: 'VIEJO' }),
      reporte({ reportadoId: 7, creadoEn: '2026-08-09T10:00:00', motivo: 'NUEVO' }),
    ], comoVino);

    expect(casos[0].reportes[0].motivo).toBe('NUEVO');
  });

  // ===================== Un conflicto no es un patrón =====================

  it('tres quejas de tres personas son tres denunciantes', () => {
    const casos = agruparEnCasos([
      reporte({ reportadoId: 7, reportadorEmail: 'a@x.test' }),
      reporte({ reportadoId: 7, reportadorEmail: 'b@x.test' }),
      reporte({ reportadoId: 7, reportadorEmail: 'c@x.test' }),
    ], comoVino);

    expect(casos[0].denunciantes).toBe(3);
    expect(casos[0].reportes.length).toBe(3);
  });

  it('tres quejas de la misma persona son un solo denunciante', () => {
    // Esto es lo que separa un conflicto entre dos de un problema con alguien.
    // Contarlo como tres seria convertir una discusion en un patron.
    const casos = agruparEnCasos([
      reporte({ reportadoId: 7, reportadorEmail: 'a@x.test' }),
      reporte({ reportadoId: 7, reportadorEmail: 'a@x.test' }),
      reporte({ reportadoId: 7, reportadorEmail: 'a@x.test' }),
    ], comoVino);

    expect(casos[0].denunciantes).toBe(1);
    expect(casos[0].reportes.length).toBe(3);
  });

  it('dos personas que se llaman igual siguen siendo dos', () => {
    // Se cuenta por correo justamente por esto: por nombre, estas dos se
    // fundirian en una y el patron desapareceria.
    const casos = agruparEnCasos([
      reporte({ reportadoId: 7, reportadorNombre: 'Ana García', reportadorEmail: 'ana1@x.test' }),
      reporte({ reportadoId: 7, reportadorNombre: 'Ana García', reportadorEmail: 'ana2@x.test' }),
    ], comoVino);

    expect(casos[0].denunciantes).toBe(2);
  });

  // ===================== El orden =====================

  it('lo que tiene algo sin revisar va antes que lo ya mirado', () => {
    const casos = agruparEnCasos([
      // Ya revisado, de hoy y con dos denunciantes: gana en todo lo demas.
      reporte({ reportadoId: 1, creadoEn: '2026-08-13T10:00:00', revisado: true,
                reportadorEmail: 'a@x.test' }),
      reporte({ reportadoId: 1, creadoEn: '2026-08-13T09:00:00', revisado: true,
                reportadorEmail: 'b@x.test' }),
      // Uno solo, viejo, pero sin revisar.
      reporte({ reportadoId: 2, creadoEn: '2026-01-01T10:00:00', revisado: false }),
    ], comoVino);

    expect(casos[0].id).toBe(2);
    expect(casos[1].id).toBe(1);
  });

  it('entre pendientes, manda cuánta gente distinta lo ha reportado', () => {
    const casos = agruparEnCasos([
      // Mas reciente, pero de una sola persona.
      reporte({ reportadoId: 1, creadoEn: '2026-08-13T10:00:00', reportadorEmail: 'a@x.test' }),
      // Mas antiguo y de dos personas distintas: esto es lo que hay que mirar.
      reporte({ reportadoId: 2, creadoEn: '2026-08-01T10:00:00', reportadorEmail: 'b@x.test' }),
      reporte({ reportadoId: 2, creadoEn: '2026-08-02T10:00:00', reportadorEmail: 'c@x.test' }),
    ], comoVino);

    expect(casos[0].id).toBe(2);
  });

  it('un caso revisado del todo se queda, no se esconde', () => {
    // Un reporte mirado sigue siendo informacion, sobre todo si esa persona
    // acumula varios. Esconderlo convertiria esto en una bandeja de tareas.
    const casos = agruparEnCasos([
      reporte({ reportadoId: 1, revisado: true }),
    ], comoVino);

    expect(casos.length).toBe(1);
    expect(casos[0].sinRevisar).toBe(0);
  });

  // ===================== Lo marcado en esta sesión =====================

  it('cuenta como revisado lo que se acaba de marcar, sin recargar', () => {
    const recien = reporte({ reportadoId: 1, revisado: false });
    const otro = reporte({ reportadoId: 1, revisado: false });

    const casos = agruparEnCasos([recien, otro], r => r.revisado || r.id === recien.id);

    expect(casos[0].sinRevisar).toBe(1);
  });

  it('sin reportes no hay casos', () => {
    expect(agruparEnCasos([], comoVino)).toEqual([]);
  });
});
