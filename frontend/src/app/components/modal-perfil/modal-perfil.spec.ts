import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ModalPerfilComponent } from './modal-perfil';
import { api } from '../../config/api';

/**
 * Lo que se protege aquí es el viaje de ida y vuelta de cada campo.
 *
 * Es la red para el fallo que más veces ha aparecido en este proyecto: un campo
 * que existe en la base, se guarda bien y no vuelve —o al revés, vuelve y no se
 * manda—. Ha pasado cuatro veces (biografía, meta semanal, foto y rutina) y
 * siempre igual: el mapeo se escribe a mano y añadir una columna no obliga a
 * tocarlo. Aquí se recorre la lista de campos editables en vez de comprobar uno.
 */
describe('ModalPerfilComponent', () => {

  let fixture: ComponentFixture<ModalPerfilComponent>;
  let componente: ModalPerfilComponent;
  let http: HttpTestingController;

  /** Un perfil completo, como lo devuelve /api/usuarios/perfil. */
  const PERFIL = {
    nombre: 'Alex',
    avatar: 'ascua',
    edad: 28,
    genero: 'Mujer',
    peso: 64,
    nivel: 'Intermedio',
    objetivos: 'Hipertrofia',
    rutina: 'TORSO_PIERNA',
    gimnasioId: 3,
    biografia: 'Busco a alguien constante',
    metaSemanal: 5,
    fotoUrl: '/api/medios/abc.png',
    horarios: [{ diaSemana: 'Lunes', horaInicio: '18:00', horaFin: '20:00', habitual: true }],
    levantamientos: [{ ejercicio: 'PRESS_BANCA', peso: 90, repeticiones: 5 }],
    ejerciciosDisponibles: [{ clave: 'PRESS_BANCA', nombre: 'Press de banca' }],
    rutinasDisponibles: [{ clave: 'TORSO_PIERNA', nombre: 'Torso / Pierna' }]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalPerfilComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(ModalPerfilComponent);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('perfil', PERFIL);
    fixture.componentRef.setInput('gimnasios', [{ id: 3, nombre: 'McFit' }]);
    fixture.componentRef.setInput('nombre', 'Alex');
    await fixture.whenStable();

    // Las marcas se piden al construirse; aquí no interesan.
    http.expectOne(api('/api/hitos')).flush([]);
  });

  afterEach(() => http.verify());

  /** Dispara el guardado y devuelve el cuerpo del PUT. */
  function cuerpoDelGuardado(): any {
    componente.guardarPerfil();
    const peticion = http.expectOne(api('/api/usuarios/perfil'));
    expect(peticion.request.method).toBe('PUT');
    const cuerpo = peticion.request.body;
    peticion.flush({});
    return cuerpo;
  }

  it('vuelca en el formulario todo lo que llega del servidor', () => {
    expect(componente.perfilForm.nivel).toBe('Intermedio');
    expect(componente.perfilForm.objetivos).toBe('Hipertrofia');
    expect(componente.perfilForm.rutina).toBe('TORSO_PIERNA');
    expect(componente.perfilForm.biografia).toBe('Busco a alguien constante');
    expect(componente.perfilForm.fotoUrl).toBe('/api/medios/abc.png');
    expect(componente.perfilForm.metaSemanal).toBe(5);
    expect(componente.perfilForm.horarios.length).toBe(1);
    expect(componente.perfilForm.levantamientos.length).toBe(1);
  });

  it('devuelve al servidor todos los campos editables, sin perder ninguno', () => {
    const cuerpo = cuerpoDelGuardado();

    // Si se añade un campo editable y no llega hasta aquí, se puede escribir en
    // la pantalla y no se guarda. Esta lista tiene que crecer con el formulario.
    for (const campo of ['avatar', 'edad', 'genero', 'peso', 'nivel', 'objetivos',
                         'rutina', 'gimnasioId', 'biografia', 'metaSemanal',
                         'fotoUrl', 'horarios', 'levantamientos']) {
      expect(cuerpo[campo], `Falta '${campo}' en el PUT`).toBeDefined();
    }
  });

  it('no cambia los valores por el camino', () => {
    const cuerpo = cuerpoDelGuardado();

    expect(cuerpo.rutina).toBe('TORSO_PIERNA');
    expect(cuerpo.metaSemanal).toBe(5);
    expect(cuerpo.fotoUrl).toBe('/api/medios/abc.png');
    expect(cuerpo.horarios[0].habitual).toBe(true);
    expect(cuerpo.levantamientos[0].ejercicio).toBe('PRESS_BANCA');
  });

  it('los catálogos vienen del servidor y no de una copia local', () => {
    expect(componente.rutinasDisponibles()).toEqual(PERFIL.rutinasDisponibles);
    expect(componente.ejerciciosDisponibles()).toEqual(PERFIL.ejerciciosDisponibles);
  });

  it('edita una copia: cancelar no deja tocado el perfil de fuera', () => {
    componente.perfilForm.horarios[0].habitual = false;
    componente.perfilForm.levantamientos[0].peso = 200;

    // Sin la copia, el tablero de detrás se quedaría con los cambios de un
    // formulario que la persona ha cancelado.
    expect(PERFIL.horarios[0].habitual).toBe(true);
    expect(PERFIL.levantamientos[0].peso).toBe(90);
  });

  it('elegir un color quita la foto', () => {
    // Con foto puesta el color no se ve por ningún lado, así que el selector
    // parecería estropeado.
    componente.seleccionarAvatar('acero');

    expect(componente.perfilForm.avatar).toBe('acero');
    expect(componente.perfilForm.fotoUrl).toBeNull();
  });

  it('no deja marcar más franjas fijas de las permitidas', () => {
    const avisos: string[] = [];
    componente.aviso.subscribe(a => avisos.push(a.texto));

    componente.perfilForm.horarios = [
      { diaSemana: 'Lunes', habitual: true },
      { diaSemana: 'Martes', habitual: true },
      { diaSemana: 'Miércoles', habitual: true },
      { diaSemana: 'Jueves', habitual: false }
    ];

    const cuarta = componente.perfilForm.horarios[3];
    expect(componente.puedeMarcarHabitual(cuarta)).toBe(false);

    componente.alternarHabitual(cuarta);

    expect(cuarta.habitual).toBe(false);
    expect(avisos.length).toBe(1);
  });

  it('desmarcar una fija siempre se puede, aunque el cupo esté lleno', () => {
    componente.perfilForm.horarios = [
      { diaSemana: 'Lunes', habitual: true },
      { diaSemana: 'Martes', habitual: true },
      { diaSemana: 'Miércoles', habitual: true }
    ];

    const primera = componente.perfilForm.horarios[0];
    componente.alternarHabitual(primera);

    expect(primera.habitual).toBe(false);
    expect(componente.habitualesMarcadas()).toBe(2);
  });

  it('cuenta como puestos solo los levantamientos completos', () => {
    componente.perfilForm.levantamientos = [
      { ejercicio: 'PRESS_BANCA', peso: 90, repeticiones: 5 },
      { ejercicio: 'SENTADILLA', peso: null, repeticiones: null },
      { ejercicio: '', peso: 100, repeticiones: 5 }
    ];

    // Los incompletos no llegan al motor, así que decir "3 de 3" mentiría.
    expect(componente.levantamientosPuestos()).toBe(1);
  });

  it('no admite más de tres levantamientos', () => {
    componente.perfilForm.levantamientos = [];
    for (let i = 0; i < 5; i++) componente.anadirLevantamiento();

    expect(componente.perfilForm.levantamientos.length).toBe(3);
  });

  it('avisa y no cierra cuando el guardado falla', () => {
    const avisos: { texto: string, tipo: string }[] = [];
    let guardados = 0;
    componente.aviso.subscribe(a => avisos.push(a));
    componente.guardado.subscribe(() => guardados++);

    componente.guardarPerfil();
    http.expectOne(api('/api/usuarios/perfil')).flush('boom', { status: 500, statusText: 'Error' });

    expect(guardados).toBe(0);
    expect(avisos[0].tipo).toBe('error');
  });

  it('un gimnasio nuevo viaja por nombre y sin id', () => {
    componente.mostrarInputGimnasio = true;
    componente.nuevoGimnasioNombre = 'Gimnasio del barrio';

    const cuerpo = cuerpoDelGuardado();

    expect(cuerpo.nuevoGimnasioNombre).toBe('Gimnasio del barrio');
    expect(cuerpo.gimnasioId).toBeNull();
  });
});
