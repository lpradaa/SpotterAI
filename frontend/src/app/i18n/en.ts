import type { ClaveDeMensaje, Mensaje } from './es';

/**
 * The English catalogue.
 *
 * Typed as `Record<ClaveDeMensaje, Mensaje>` on purpose: the Spanish catalogue
 * is the source of truth for which keys exist, so a missing translation — or a
 * leftover one for a key that no longer exists — fails the build instead of
 * showing up on screen. With this many strings, "I forgot one" stops being a
 * possibility.
 *
 * Interpolation placeholders (`{name}`) must match the Spanish ones by name,
 * not by position: the word order changes between the two languages and that is
 * exactly the point of naming them.
 */
export const en: Record<ClaveDeMensaje, Mensaje> = {
  // ===================== Header and menu =====================
  'cabecera.abrirMenu': 'Open menu',
  'cabecera.cerrarMenu': 'Close menu',
  'cabecera.inicio': 'Spotter, go to home',
  'cabecera.avisos': { uno: '{cuenta} thing waiting for your reply', otros: '{cuenta} things waiting for your reply' },
  'cabecera.sinAvisos': 'Nothing waiting for you',
  'cabecera.temaClaro': 'Switch to light theme',
  'cabecera.temaOscuro': 'Switch to dark theme',
  'cabecera.idioma': 'Change language',
  'cabecera.idiomaA': 'Cambiar a español',

  'menu.tablero': 'Home',
  'menu.companeros': 'Partners',
  'menu.explorar': 'Explore',
  'menu.solicitudes': 'Requests',
  'menu.miPerfil': 'My profile',
  'menu.cerrarSesion': 'Log out',

  // ===================== Shared =====================
  'comun.cargando': 'Loading…',
  'comun.unMomento': 'One moment…',
  'comun.cancelar': 'Cancel',
  'comun.volver': '← Back',
  'comun.guardar': 'Save',
  'comun.dias': { uno: '{cuenta} day', otros: '{cuenta} days' },
  'comun.personas': { uno: '{cuenta} person', otros: '{cuenta} people' },

  // ===================== Sign in =====================
  'login.lema': 'Find the person to train with, not just anyone.',
  'login.venta1': 'We cross your actual schedules, not just your profile',
  'login.venta2': 'We tell you why you fit, with the specific days',
  'login.venta3': 'And you keep a log of what you train',

  'login.tituloEntrar': 'Welcome back',
  'login.tituloRegistro': 'Create your account',
  'login.subtituloEntrar': 'Sign in to continue',
  'login.subtituloRegistro': 'Create your free account',

  'login.nombre': 'Full name',
  'login.nombreEjemplo': 'e.g. Carlos Pérez',
  'login.edad': 'Age',
  'login.edadEjemplo': 'e.g. 25',
  'login.peso': 'Weight (kg)',
  'login.pesoEjemplo': 'e.g. 75.5',
  'login.genero': 'Gender',
  'login.generoElige': 'Choose an option',
  'login.generoMasculino': 'Male',
  'login.generoFemenino': 'Female',
  'login.generoOtro': 'Other',
  'login.email': 'Email',
  'login.emailEjemplo': 'example@email.com',
  'login.contrasena': 'Password',
  'login.contrasenaMinimo': 'At least {minimo} characters',
  'login.contrasenaFaltan': { uno: '{cuenta} character to go', otros: '{cuenta} characters to go' },

  'login.procesando': 'Working…',
  'login.entrar': 'Sign in to Spotter',
  'login.crearCuenta': 'Create account',

  'login.olvidada': 'I forgot my password',
  'login.olvidadaEnviando': 'Sending…',
  'login.olvidadaHecho': 'If that address has an account, we have sent a link. Check your spam folder too.',

  'login.sinCuenta': "Don't have an account?",
  'login.conCuenta': 'Already have an account?',
  'login.registrate': 'Sign up here',
  'login.iniciaSesion': 'Sign in',

  // ===================== Onboarding =====================
  'bienvenida.tituloConNombre': '{nombre}, when do you train?',
  'bienvenida.titulo': 'When do you train?',
  'bienvenida.porQue': 'We look for people whose gym hours line up with yours, so without your hours there is nothing to cross.',
  'bienvenida.porQuePeso': 'It is worth 40 % of the match, more than level and goal combined.',
  'bienvenida.marca': 'Mark the hours you could go',
  'bienvenida.horas': { uno: '{cuenta} hour a week', otros: '{cuenta} hours a week' },
  'bienvenida.holgura': 'Be generous: this is when you can go, not a commitment. Once you know what you always attend, you can mark it in your profile and it will count for much more.',
  'bienvenida.guardando': 'Saving…',
  'bienvenida.continuar': 'Continue',
  'bienvenida.marcaUnaHora': 'Mark at least one hour to continue',

  'bienvenida.tituloDatos': 'Five more things and you are done',
  'bienvenida.porQueDatos': 'These are what the engine scores. Without them your match is computed with half the data and cannot be compared to anyone: neither you nor they would know what the number means.',
  'bienvenida.donde': 'Where do you train?',
  'bienvenida.eligeGimnasio': 'Choose your gym',
  'bienvenida.tuNivel': 'Your level',
  'bienvenida.eligeNivel': 'Choose your level',
  'bienvenida.queBuscas': 'What you are after',
  'bienvenida.eligeObjetivo': 'Choose your goal',
  'bienvenida.comoRepartes': 'How you split the week',
  'bienvenida.eligeRutina': 'Choose your split',
  'bienvenida.edad': 'Age',
  'bienvenida.sinRutina': 'If you do not follow a fixed split, pick that: it is an answer, not a gap, and it counts as one.',
  'bienvenida.atras': 'Back',
  'bienvenida.empezar': 'Get started',
  'bienvenida.faltanCampos': 'Some fields are still empty',

  'nivel.principiante': 'Beginner',
  'nivel.intermedio': 'Intermediate',
  'nivel.avanzado': 'Advanced',
  'objetivo.hipertrofia': 'Hypertrophy',
  'objetivo.fuerza': 'Strength',
  'objetivo.perdidaDePeso': 'Weight loss',
  'objetivo.resistencia': 'Endurance',

  // ===================== Home =====================
  'tablero.perfilSinCompletar': 'Profile not filled in',
  'tablero.estaSemana': { uno: '{hechos} of {total} session this week', otros: '{hechos} of {total} sessions this week' },
  'tablero.buscarCompaneros': 'Find partners',
  'tablero.miPerfil': 'My profile',

  'tablero.entrenaste': 'Did you train with {nombre}?',
  'tablero.entrenasCon': 'You are training with {nombre}',
  'tablero.hasPropuesto': 'You proposed training with {nombre}',
  'tablero.teProponen': '{nombre} proposes training with you',
  'tablero.esperandoRespuesta': 'waiting for a reply',
  'tablero.siEntrenamos': 'Yes, we did',
  'tablero.rechazar': 'Decline',
  'tablero.aceptar': 'Accept',
  'tablero.abrirChat': 'Open chat',

  'tablero.teFalta': 'You are missing {que}:',
  'tablero.puntosMenos': '−{puntos} points',
  'tablero.deCompatibilidad': 'of compatibility with anyone.',
  'tablero.todoLoQueFalta': 'Counting everything missing, {puntos} out of 100 are off the table.',
  'tablero.completar': 'Fill it in',

  'tablero.tuGente': 'Your people',
  'tablero.buscarMas': 'Find more',
  'tablero.sinActividad': 'When your partners log a session or add a milestone, it will show up here.',
  'tablero.haApuntado': 'added',
  'tablero.haEntrenado': 'trained',
  'tablero.verPerfilDe': "View {nombre}'s profile",

  'tablero.miActividad': 'My activity',
  'tablero.registrar': 'Log one',
  'tablero.sinEntrenamientos': 'You have not logged any sessions yet.',
  'tablero.minutos': '{cuenta} min',
  'tablero.seguro': 'Sure?',
  'tablero.no': 'No',
  'tablero.si': 'Yes',
  'tablero.borrarEntreno': 'Delete {tipo} from {fecha}',

  'tablero.registrarEntrenamiento': 'Log a session',
  'tablero.cerrar': 'Close',
  'tablero.fecha': 'Date',
  'tablero.tipo': 'Type',
  'tablero.duracion': 'Duration (minutes)',
  'tablero.notas': 'Notes',
  'tablero.notasEjemplo': 'PR on the press…',

  'entreno.fuerzaTorso': 'Strength (Upper)',
  'entreno.fuerzaPierna': 'Strength (Legs)',
  'entreno.fuerzaFullbody': 'Strength (Full body)',
  'entreno.cardioLiss': 'LISS cardio',
  'entreno.hiit': 'HIIT',
  'entreno.claseDirigida': 'Group class',

  // ===================== Reports (moderation) =====================
  'reportes.titulo': 'Reports',
  'reportes.texto': 'What people have reported about others, grouped by who was reported. Reporting does not block anyone on its own: whoever reports can still see the conversation.',
  'reportes.pendientes': '{cuenta} unreviewed',
  'reportes.sinAcceso': 'You do not have access to this screen.',
  'reportes.errorCarga': 'The reports could not be loaded.',
  'reportes.vacio': 'No reports yet.',
  'reportes.patron': { uno: '{cuenta} person', otros: '{cuenta} different people' },
  'reportes.loHanReportado': 'reported them',
  'reportes.deUnaSola': { uno: 'One report from a single person', otros: '{cuenta} reports from a single person' },
  'reportes.sinRevisar': '{cuenta} unreviewed',
  'reportes.de': 'from {nombre}',
  'reportes.marcarVisto': 'Mark as seen',
  'reportes.visto': 'Seen',

  // Report reasons. The values themselves travel to the backend and are never
  // translated; what gets translated is how they read.
  'motivo.COMPORTAMIENTO_INAPROPIADO': 'Inappropriate behaviour',
  'motivo.PERFIL_FALSO': 'Fake profile or impersonation',
  'motivo.ACOSO': 'Harassment or unwanted messages',
  'motivo.SPAM': 'Spam or advertising',
  'motivo.OTRO': 'Other',
};
