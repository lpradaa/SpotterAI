/**
 * El catálogo en español, y la fuente de la verdad de qué claves existen.
 *
 * Este fichero manda: `en.ts` está tipado contra él, así que olvidarse de
 * traducir una clave —o dejar una traducción de una clave que ya no existe— es
 * un error de compilación y no algo que se descubre en la pantalla. Con medio
 * millar de textos, «se me ha pasado uno» deja de ser una posibilidad.
 *
 * Convención de claves: `pantalla.pieza`, en minúsculas y con puntos. Se lee de
 * lo general a lo concreto y ordena solo.
 *
 * Un mensaje es una cadena, o un par si el texto cambia con la cantidad. El
 * plural se pide con `{ cuenta: n }` y lo resuelve el servicio: español e
 * inglés parten igual —uno / otros— así que no hace falta la maquinaria de
 * CLDR para seis formas que aquí no se usan.
 *
 * La interpolación va con `{nombre}` y no con `{0}`: con esta cantidad de
 * textos, un `{0}` obliga a ir a buscar la llamada para saber qué es.
 */

/** Un texto, o el par que hace falta cuando depende de la cantidad. */
export type Mensaje = string | { uno: string; otros: string };

export const es = {
  // ===================== Cabecera y menú =====================
  'cabecera.abrirMenu': 'Abrir menú',
  'cabecera.cerrarMenu': 'Cerrar menú',
  'cabecera.inicio': 'Spotter, ir al inicio',
  'cabecera.avisos': { uno: '{cuenta} cosa pendiente por responder', otros: '{cuenta} cosas pendientes por responder' },
  'cabecera.sinAvisos': 'No tienes nada pendiente',
  'cabecera.temaClaro': 'Cambiar a tema claro',
  'cabecera.temaOscuro': 'Cambiar a tema oscuro',
  'cabecera.idioma': 'Cambiar idioma',
  'cabecera.idiomaA': 'Switch to English',

  'menu.tablero': 'Tablero',
  'menu.companeros': 'Compañeros',
  'menu.explorar': 'Explorar',
  'menu.solicitudes': 'Solicitudes',
  'menu.miPerfil': 'Mi perfil',
  'menu.cerrarSesion': 'Cerrar sesión',

  // ===================== Común =====================
  'comun.cargando': 'Cargando…',
  'comun.unMomento': 'Un momento…',
  'comun.cancelar': 'Cancelar',
  'comun.volver': '← Volver',
  'comun.guardar': 'Guardar',
  'comun.dias': { uno: '{cuenta} día', otros: '{cuenta} días' },
  'comun.personas': { uno: '{cuenta} persona', otros: '{cuenta} personas' },

  // ===================== Entrada =====================
  'login.lema': 'Encuentra a la persona con la que entrenar, no a cualquiera.',
  'login.venta1': 'Cruzamos vuestros horarios reales, no solo el perfil',
  'login.venta2': 'Te decimos por qué encajáis, con los días concretos',
  'login.venta3': 'Y llevas el registro de lo que entrenas',

  'login.tituloEntrar': 'Hola de nuevo',
  'login.tituloRegistro': 'Crea tu cuenta',
  'login.subtituloEntrar': 'Inicia sesión para continuar',
  'login.subtituloRegistro': 'Crea tu cuenta gratis',

  'login.nombre': 'Nombre completo',
  'login.nombreEjemplo': 'Ej. Carlos Pérez',
  'login.edad': 'Edad',
  'login.edadEjemplo': 'Ej. 25',
  'login.peso': 'Peso (kg)',
  'login.pesoEjemplo': 'Ej. 75.5',
  'login.genero': 'Género',
  'login.generoElige': 'Selecciona una opción',
  'login.generoMasculino': 'Masculino',
  'login.generoFemenino': 'Femenino',
  'login.generoOtro': 'Otro',
  'login.email': 'Email',
  'login.emailEjemplo': 'ejemplo@correo.com',
  'login.contrasena': 'Contraseña',
  'login.contrasenaMinimo': 'Al menos {minimo} caracteres',
  'login.contrasenaFaltan': { uno: 'Falta {cuenta} carácter', otros: 'Faltan {cuenta} caracteres' },

  'login.procesando': 'Procesando…',
  'login.entrar': 'Entrar a Spotter',
  'login.crearCuenta': 'Crear Cuenta',

  'login.olvidada': 'He olvidado mi contraseña',
  'login.olvidadaEnviando': 'Enviando…',
  'login.olvidadaHecho': 'Si ese correo tiene cuenta, te hemos mandado un enlace. Mira también en la carpeta de no deseados.',

  'login.sinCuenta': '¿No tienes cuenta?',
  'login.conCuenta': '¿Ya tienes una cuenta?',
  'login.registrate': 'Regístrate aquí',
  'login.iniciaSesion': 'Inicia sesión',

  // ===================== Reportes (moderación) =====================
  'reportes.titulo': 'Reportes',
  'reportes.texto': 'Lo que la gente ha reportado sobre otras personas, agrupado por quién ha sido reportado. Reportar no bloquea por sí solo: quien reporta puede seguir viendo la conversación.',
  'reportes.pendientes': '{cuenta} sin revisar',
  'reportes.sinAcceso': 'No tienes acceso a esta pantalla.',
  'reportes.errorCarga': 'No se han podido cargar los reportes.',
  'reportes.vacio': 'No hay ningún reporte todavía.',
  'reportes.patron': { uno: '{cuenta} persona', otros: '{cuenta} personas distintas' },
  'reportes.loHanReportado': 'lo han reportado',
  'reportes.deUnaSola': { uno: 'Un reporte de una sola persona', otros: '{cuenta} reportes de una sola persona' },
  'reportes.sinRevisar': '{cuenta} sin revisar',
  'reportes.de': 'de {nombre}',
  'reportes.marcarVisto': 'Marcar visto',
  'reportes.visto': 'Visto',

  // Motivos por los que se puede reportar. Los valores viajan al backend y no
  // se traducen; lo que se traduce es cómo se leen.
  'motivo.COMPORTAMIENTO_INAPROPIADO': 'Comportamiento inapropiado',
  'motivo.PERFIL_FALSO': 'Perfil falso o suplantación',
  'motivo.ACOSO': 'Acoso o mensajes no deseados',
  'motivo.SPAM': 'Spam o publicidad',
  'motivo.OTRO': 'Otro motivo',
} as const satisfies Record<string, Mensaje>;

/** Todas las claves que existen. Lo que no esté aquí no se puede pedir. */
export type ClaveDeMensaje = keyof typeof es;
