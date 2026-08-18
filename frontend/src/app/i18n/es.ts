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

  /* Los tres avisos del login, que viven en el componente y no en la plantilla. */
  'login.sesionCaducada': 'Tu sesión ha caducado. Vuelve a entrar.',
  'login.escribeCorreo': 'Escribe tu correo y vuelve a pulsar.',
  'login.credencialesMal': 'Email o contraseña incorrectos. Inténtalo de nuevo.',

  'login.sinCuenta': '¿No tienes cuenta?',
  'login.conCuenta': '¿Ya tienes una cuenta?',
  'login.registrate': 'Regístrate aquí',
  'login.iniciaSesion': 'Inicia sesión',

  // ===================== Bienvenida =====================
  'bienvenida.tituloConNombre': '{nombre}, ¿cuándo entrenas?',
  'bienvenida.titulo': '¿Cuándo entrenas?',
  'bienvenida.porQue': 'Buscamos gente que coincida contigo en el gimnasio, así que sin saber tus horas no hay nada que cruzar.',
  'bienvenida.porQuePeso': 'Pesa el 40 % de la compatibilidad, más que el nivel y el objetivo juntos.',
  'bienvenida.marca': 'Marca las horas a las que podrías ir',
  'bienvenida.horas': { uno: '{cuenta} hora a la semana', otros: '{cuenta} horas a la semana' },
  'bienvenida.holgura': 'Pinta con holgura: esto es cuándo puedes, no un compromiso. Cuando sepas a qué vas siempre, podrás marcarlo en tu perfil y contará mucho más.',
  'bienvenida.guardando': 'Guardando…',
  'bienvenida.continuar': 'Continuar',
  'bienvenida.marcaUnaHora': 'Marca al menos una hora para continuar',

  'bienvenida.tituloDatos': 'Cinco cosas más y ya está',
  'bienvenida.porQueDatos': 'Son las que usa el motor para puntuar. Sin ellas tu compatibilidad se calcula con la mitad de los datos y no se puede comparar con la de nadie: ni tú sabrías qué significa tu número ni los demás el suyo.',
  'bienvenida.donde': '¿Dónde entrenas?',
  'bienvenida.eligeGimnasio': 'Elige tu gimnasio',
  'bienvenida.tuNivel': 'Tu nivel',
  'bienvenida.eligeNivel': 'Elige tu nivel',
  'bienvenida.queBuscas': 'Qué buscas',
  'bienvenida.eligeObjetivo': 'Elige tu objetivo',
  'bienvenida.comoRepartes': 'Cómo repartes la semana',
  'bienvenida.eligeRutina': 'Elige tu rutina',
  'bienvenida.edad': 'Edad',
  'bienvenida.sinRutina': 'Si no sigues ninguna rutina fija, elígelo: es una respuesta, no un hueco, y cuenta como tal.',
  'bienvenida.atras': 'Atrás',
  'bienvenida.empezar': 'Empezar',
  'bienvenida.faltanCampos': 'Faltan campos por elegir',
  'bienvenida.errorHorario': 'No se ha podido guardar tu horario. Inténtalo otra vez.',
  'bienvenida.errorDatos': 'No se han podido guardar tus datos. Inténtalo otra vez.',

  /* Las etiquetas de nivel y objetivo, para los desplegables del formulario.
     El VALOR que se elige viaja al backend en español y no se traduce nunca —es
     el dato que se guarda—; esto es solo cómo se lee.

     Existen también en messages.properties del backend, que las necesita para
     redactar sus frases ("Los dos entrenáis a nivel intermedio"). Son dos
     catálogos y tienen que decir lo mismo: aquí se elige, allí se cuenta. */
  'nivel.principiante': 'Principiante',
  'nivel.intermedio': 'Intermedio',
  'nivel.avanzado': 'Avanzado',
  'objetivo.hipertrofia': 'Hipertrofia',
  'objetivo.fuerza': 'Fuerza',
  'objetivo.perdidaDePeso': 'Pérdida de peso',
  'objetivo.resistencia': 'Resistencia',

  /* El género es otro valor que se guarda en español. Estaba bajo `login.` de
     cuando el único sitio donde se leía era el formulario de registro; en
     cuanto explorar lo usó de filtro había que elegir entre repetir el texto o
     llamar `login.` a algo que ya no es del login. Va donde el resto de los
     valores guardados, que es de donde lo pide quien lo necesite. */
  'genero.masculino': 'Masculino',
  'genero.femenino': 'Femenino',
  'genero.otro': 'Otro',

  // Los colores del avatar. Se guardan en espanol, como el resto de valores.
  'color.ascua': 'ascua',
  'color.ambar': 'ambar',
  'color.oliva': 'oliva',
  'color.acero': 'acero',
  'color.ciruela': 'ciruela',
  'color.pizarra': 'pizarra',

  /* Los días, que son el tercer valor guardado en español: la rejilla escribe
     «Lunes» en la franja y así viaja y así se compara. Existen también en
     `messages.properties` como `dia.LUNES`, porque el motor los mete dentro de
     sus frases («Los dos vais siempre 2 días a la misma hora (Lunes y
     Miércoles)»). Tienen que decir lo mismo.

     Tres formas y no una, porque la rejilla se dibuja a tres anchos:
       - completo  el texto que oye un lector de pantalla
       - abrev     la cabecera de la rejilla grande
       - estrecho  la fila de siete columnas de una tarjeta

     La forma estrecha se escribe y no se recorta de la larga: en español una
     letra basta y ademas la M de martes y la X de miércoles son un acuerdo que
     ningún corte automático produce; en inglés una letra deja martes y jueves
     escritos igual, así que van dos. */
  'dia.lunes': 'Lunes',
  'dia.lunesAbrev': 'Lun',
  'dia.lunesEstrecho': 'L',
  'dia.martes': 'Martes',
  'dia.martesAbrev': 'Mar',
  'dia.martesEstrecho': 'M',
  'dia.miercoles': 'Miércoles',
  'dia.miercolesAbrev': 'Mié',
  'dia.miercolesEstrecho': 'X',
  'dia.jueves': 'Jueves',
  'dia.juevesAbrev': 'Jue',
  'dia.juevesEstrecho': 'J',
  'dia.viernes': 'Viernes',
  'dia.viernesAbrev': 'Vie',
  'dia.viernesEstrecho': 'V',
  'dia.sabado': 'Sábado',
  'dia.sabadoAbrev': 'Sáb',
  'dia.sabadoEstrecho': 'S',
  'dia.domingo': 'Domingo',
  'dia.domingoAbrev': 'Dom',
  'dia.domingoEstrecho': 'D',

  // ===================== Tablero =====================
  'tablero.perfilSinCompletar': 'Perfil sin completar',
  'tablero.estaSemana': { uno: '{hechos} de {total} entrenamiento esta semana', otros: '{hechos} de {total} entrenamientos esta semana' },
  'tablero.buscarCompaneros': 'Buscar compañeros',
  'tablero.miPerfil': 'Mi perfil',

  'tablero.entrenaste': '¿Entrenaste con {nombre}?',
  'tablero.entrenasCon': 'Entrenas con {nombre}',
  'tablero.hasPropuesto': 'Has propuesto entrenar con {nombre}',
  'tablero.teProponen': '{nombre} propone entrenar contigo',
  'tablero.esperandoRespuesta': 'esperando respuesta',
  'tablero.siEntrenamos': 'Sí, entrenamos',
  'tablero.rechazar': 'Rechazar',
  'tablero.aceptar': 'Aceptar',
  'tablero.abrirChat': 'Abrir chat',

  'tablero.teFalta': 'Te falta {que}:',
  'tablero.puntosMenos': '−{puntos} puntos',
  'tablero.deCompatibilidad': 'de compatibilidad con cualquiera.',
  'tablero.todoLoQueFalta': 'Contando todo lo que falta, quedan {puntos} de 100 fuera de juego.',
  'tablero.completar': 'Completar',

  'tablero.tuGente': 'Tu gente',
  'tablero.buscarMas': 'Buscar más',
  'tablero.sinActividad': 'Cuando tus compañeros registren un entrenamiento o apunten una marca, aparecerá aquí.',
  'tablero.haApuntado': 'ha apuntado',
  'tablero.haEntrenado': 'ha entrenado',
  'tablero.verPerfilDe': 'Ver el perfil de {nombre}',

  'tablero.miActividad': 'Mi actividad',
  'tablero.registrar': 'Registrar',
  'tablero.sinEntrenamientos': 'Aún no has registrado entrenamientos.',
  'tablero.minutos': '{cuenta} min',
  'tablero.seguro': '¿Seguro?',
  'tablero.no': 'No',
  'tablero.si': 'Sí',
  'tablero.borrarEntreno': 'Borrar {tipo} del {fecha}',

  'tablero.registrarEntrenamiento': 'Registrar entrenamiento',
  'tablero.cerrar': 'Cerrar',
  'tablero.fecha': 'Fecha',
  'tablero.tipo': 'Tipo',
  'tablero.duracion': 'Duración (minutos)',
  'tablero.notas': 'Notas',
  'tablero.notasEjemplo': 'Récord en prensa…',

  /* Los avisos del tablero, que salen en un toast y no en la plantilla. Eran de
     los últimos textos en español que quedaban: el contador mira las plantillas
     y estos viven en el componente. */
  'tablero.quiereEntrenar': '{nombre} quiere entrenar contigo.',
  'tablero.proponeEntrenar': '{nombre} propone entrenar contigo.',
  'tablero.haAceptado': '{nombre} ha aceptado entrenar contigo.',
  'tablero.entrenamientoRegistrado': 'Entrenamiento registrado.',
  'tablero.errorRegistrar': 'Hubo un error al guardar tu entrenamiento.',

  /* Responder o confirmar una sesión pasa desde dos sitios —el tablero y la
     pantalla de solicitudes— y decía lo mismo escrito dos veces. */
  'sesion.hecho': 'Hecho. Entrenas con {nombre}.',
  'sesion.rechazada': 'Propuesta rechazada.',
  'sesion.apuntado': 'Apuntado en tu historial.',
  'sesion.errorResponder': 'No se ha podido responder.',
  'sesion.errorApuntar': 'No se ha podido apuntar.',

  /* Los tipos de entrenamiento. Igual que el nivel y el objetivo: el VALOR se
     guarda en español y viaja tal cual, la etiqueta es lo único que cambia. */
  'entreno.fuerzaTorso': 'Fuerza (Torso)',
  'entreno.fuerzaPierna': 'Fuerza (Pierna)',
  'entreno.fuerzaFullbody': 'Fuerza (Fullbody)',
  'entreno.cardioLiss': 'Cardio LISS',
  'entreno.hiit': 'HIIT',
  'entreno.claseDirigida': 'Clase dirigida',

  /* ===================== La semana =====================

     La rejilla no es una pantalla sino una pieza, y sale en cuatro sitios: cada
     tarjeta de explorar, la ficha de una en una, tu perfil y el de otra persona.
     Por eso tiene prefijo propio y no el de ninguna de ellas.

     Los tramos van con la frase entera y el día dentro, en vez de componer «X de
     A a B» y meterlo luego en «…: coincidís». Anidar obliga a que el orden de
     las dos piezas sea el mismo en los dos idiomas, y es la forma típica de
     acabar con media frase traducida. */
  'rejilla.vacia': 'Sin horarios que cruzar todavía.',
  'rejilla.tuSemana': 'Tu semana. No coincidís en ninguna franja.',
  'rejilla.coincidisEn': { uno: 'Coincidís en una franja de la semana.',
                           otros: 'Coincidís en {cuenta} franjas de la semana.' },

  'rejilla.tramo': '{dia} de {desde} a {hasta}',
  'rejilla.tramoCoincidis': '{dia} de {desde} a {hasta}: coincidís',
  'rejilla.tramoCoincidisFijo': '{dia} de {desde} a {hasta}: coincidís y los dos vais siempre',
  'rejilla.tramoVasSiempre': '{dia} de {desde} a {hasta}: vas siempre',

  'rejilla.hora': '{hora}h',
  'rejilla.tuDisponibilidad': 'Tu disponibilidad',
  'rejilla.coincidis': 'Coincidís',
  'rejilla.losDosSiempre': 'Los dos vais siempre',

  /* Cada celda de la rejilla que se pinta a clic. Es un botón sin texto, así que
     esto es lo único que puede decir qué se está marcando. */
  'rejilla.celda': '{dia} de {hora} a {siguiente}',

  // ===================== Explorar =====================
  'explorar.titulo': 'Explorar',
  'explorar.resumen': { uno: '{mostrados} de {total} persona en tu comunidad',
                        otros: '{mostrados} de {total} personas en tu comunidad' },
  'explorar.buscarPorNombre': 'Buscar por nombre',
  'explorar.buscar': 'Buscar',
  'explorar.filtros': 'Filtros',
  'explorar.hayFiltros': 'Hay filtros activos',
  'explorar.deUnaEnUna': 'De una en una',
  'explorar.nadieNuevo': 'No hay nadie nuevo por ahora.',

  'explorar.primeroTitulo': 'Eres el primero de tu gimnasio.',
  'explorar.primeroTexto': 'Aquí abajo hay gente, pero entrena en otros sitios, y coincidir a la misma hora en dos edificios distintos no es coincidir: por eso puntúan bajo. Con alguien de tu gimnasio los números son otros.',
  'explorar.invitar': 'Invitar a alguien de tu gimnasio',
  'explorar.enlaceCopiado': 'Enlace copiado',
  'explorar.noSeHaCopiado': 'No se ha podido copiar',
  /* Se manda por WhatsApp a alguien que todavía no tiene la aplicación, así que
     va en el idioma de quien invita: es lo único que sabemos de los dos. */
  'explorar.mensajeInvitacion': 'Estoy usando SpotterAI para no entrenar solo. Si te apuntas, cruzamos horarios y vemos qué días coincidimos: {enlace}',

  'explorar.vacioTitulo': 'Todavía no hay nadie',
  'explorar.vacioTexto': 'Esto se llena con gente de tu gimnasio. Trae a alguien con quien ya entrenes y el resto viene solo.',
  'explorar.sinResultados': 'Sin resultados',
  'explorar.sinResultadosTexto': 'No hay nadie que encaje con esa combinación de filtros.',
  'explorar.quitarFiltros': 'Quitar filtros',

  'explorar.verPerfilDe': 'Ver el perfil de {nombre}',
  'explorar.sinGimnasio': 'Sin gimnasio',
  'explorar.sinNivel': 'Sin nivel',
  'explorar.pesosParecidos': 'Movéis pesos parecidos',
  'explorar.podeisCubriros': 'Podéis cubriros',
  'explorar.diasFijos': { uno: '{cuenta} día fijo', otros: '{cuenta} días fijos' },
  'explorar.datosIncompletos': 'Datos incompletos',
  'explorar.escribir': 'Escribir',
  'explorar.retirar': 'Retirar',
  'explorar.retirarTitulo': 'Retirar la solicitud enviada a {nombre}',
  'explorar.conectar': 'Conectar',

  'explorar.solicitudEnviada': 'Solicitud enviada.',
  'explorar.solicitudNoEnviada': 'No se pudo enviar la solicitud.',
  'explorar.solicitudRetirada': 'Solicitud a {nombre} retirada.',
  'explorar.solicitudNoRetirada': 'No se ha podido retirar la solicitud.',

  'explorar.panelFiltros': 'Filtros de búsqueda',
  'explorar.cerrarFiltros': 'Cerrar filtros',
  'explorar.gimnasio': 'Gimnasio',
  'explorar.todos': 'Todos',
  'explorar.nivel': 'Nivel',
  'explorar.cualquiera': 'Cualquiera',
  'explorar.rutina': 'Rutina',
  'explorar.objetivo': 'Objetivo',
  'explorar.genero': 'Género',
  'explorar.edad': 'Edad',
  'explorar.desde': 'Desde',
  'explorar.hasta': 'Hasta',
  'explorar.edadMinima': 'Edad mínima',
  'explorar.edadMaxima': 'Edad máxima',
  'explorar.soloCubrirme': 'Solo quien pueda cubrirme',
  'explorar.soloCubrirmeNota': 'Deja fuera a quien no tenga marcas: sin ellas no se sabe.',
  'explorar.soloMiGimnasio': 'Solo mi gimnasio',
  'explorar.soloMiGimnasioNota': 'Va por el sitio, no por el nombre: hay gimnasios repetidos en la lista de arriba. Entrenar en el mismo vale 8 puntos y además cuadruplica lo que cuenta coincidir de horario.',
  'explorar.soloDiasFijos': 'Solo con días fijos en común',
  'explorar.soloDiasFijosNota': 'No que podáis coincidir: que los dos vais siempre ese día a esa hora. Es la señal más fuerte que tiene el motor.',
  'explorar.limpiar': 'Limpiar',

  // ===================== La página de una persona =====================
  'perfil.errorCarga': 'No se ha podido cargar el perfil.',
  'perfil.anos': { uno: '{cuenta} año', otros: '{cuenta} años' },
  'perfil.actividad': { uno: '{cuenta} entrenamiento esta semana',
                        otros: '{cuenta} entrenamientos esta semana' },
  'perfil.juntos': { uno: 'Ya habéis quedado una vez', otros: 'Ya habéis quedado {cuenta} veces' },

  'perfil.tuSemana': 'Tu semana',
  'perfil.metaSemanal': { uno: '{cuenta} día por semana', otros: '{cuenta} días por semana' },
  'perfil.sinHorarios': 'Todavía no has puesto tus horarios, y es el dato que más pesa: sin ellos no hay con quién cruzarte.',

  'perfil.loQueTeFalta': 'Lo que te falta',
  'perfil.puntosEnJuego': 'Son {puntos} puntos de compatibilidad.',
  'perfil.puntosEnJuegoTexto': 'Ahora mismo no los puedes ganar con nadie. No restan: se reparten entre lo demás, y eso hace que tu número diga menos de lo que podría.',
  'perfil.completar': 'Completar mi perfil',

  'perfil.ocultarDesglose': 'Ocultar el desglose',
  'perfil.verDesglose': 'Ver de dónde sale este {puntuacion} %',
  /* El día ya no va en negrita dentro de la frase: para eso habría que partirla
     en tres y volver a unirla en el mismo orden en los dos idiomas. */
  'perfil.ocasion': 'Podríais entrenar {cuando}, de {desde} a {hasta}.',
  'perfil.ocasionFija': 'Podríais entrenar {cuando}, de {desde} a {hasta}, y los dos vais siempre.',

  'perfil.loQueMueves': 'Lo que mueves',
  'perfil.loQueMoveis': 'Lo que movéis',
  'perfil.tablaMarcas': 'Vuestras marcas principales, con el máximo estimado a una repetición',
  'perfil.ejercicio': 'Ejercicio',
  'perfil.tu': 'Tú',
  'perfil.sinEjerciciosEnComun': 'No tenéis ningún ejercicio en común, así que no hay forma de saber si podríais cubriros.',
  'perfil.maximoEstimado': 'El número grande es el máximo estimado a una repetición.',
  'perfil.maximoEstimadoTexto': 'Es lo que permite comparar «100 kg × 3» con «85 kg × 8». Debajo va la marca tal cual la apuntasteis.',

  'perfil.misMarcas': 'Mis marcas',
  'perfil.susMarcas': 'Sus marcas',

  /* Cuándo fue un logro. En una lista de marcas importa si es reciente, no el
     día exacto, así que se cuenta hacia atrás. */
  'perfil.hoy': 'hoy',
  'perfil.ayer': 'ayer',
  'perfil.haceDias': 'hace {cuenta} días',
  'perfil.haceSemanas': { uno: 'hace {cuenta} semana', otros: 'hace {cuenta} semanas' },
  'perfil.haceMeses': { uno: 'hace {cuenta} mes', otros: 'hace {cuenta} meses' },

  'perfil.dejarDeSerCompaneros': 'Dejar de ser compañeros',
  'perfil.bloquearA': 'Bloquear a {nombre}',
  'perfil.bloquear': 'Bloquear',
  'perfil.avisoBloqueo': 'Dejaréis de veros en la aplicación y no podrá volver a escribirte. Si erais compañeros, se deshace. Puedes quitarlo cuando quieras desde tu perfil.',
  'perfil.reportarA': 'Reportar a {nombre}',
  'perfil.reporteEnviado': 'Reporte enviado. Gracias por avisar.',
  'perfil.motivo': 'Motivo',
  'perfil.eligeMotivo': 'Elige un motivo',
  'perfil.algoMas': 'Algo más que añadir (opcional)',
  'perfil.algoMasEjemplo': 'Lo que creas que ayuda a entender lo que ha pasado',
  'perfil.enviarReporte': 'Enviar reporte',

  'perfil.editar': 'Editar mi perfil',
  'perfil.proponerSesion': 'Proponer sesión',
  'perfil.escribirA': 'Escribir a {nombre}',
  'perfil.retirarSolicitud': 'Retirar solicitud',
  'perfil.conectarCon': 'Conectar con {nombre}',

  'perfil.actualizado': 'Perfil actualizado.',
  'perfil.errorFormulario': 'No se ha podido abrir el formulario.',
  'perfil.errorSolicitud': 'No se ha podido enviar la solicitud.',
  'perfil.errorDeshacer': 'No se ha podido deshacer.',

  // ===================== Editar perfil =====================
  'perfilEd.titulo': 'Editar perfil',
  'perfilEd.cerrar': 'Cerrar',
  'perfilEd.guardar': 'Guardar cambios',

  'perfilEd.tuColor': 'Tu color',
  'perfilEd.tuColorAyuda': 'Tu inicial te identifica en las listas; el color es para distinguirte de un vistazo.',
  'perfilEd.color': 'Color {color}',
  'perfilEd.subiendo': 'Subiendo…',
  'perfilEd.cambiarFoto': 'Cambiar foto',
  'perfilEd.subirFoto': 'Subir una foto',
  'perfilEd.quitar': 'Quitar',

  'perfilEd.datos': 'Datos y objetivos',
  'perfilEd.nivel': 'Nivel',
  'perfilEd.eligeNivel': 'Elige tu nivel',
  'perfilEd.objetivo': 'Objetivo',
  'perfilEd.eligeObjetivo': 'Elige tu objetivo',
  'perfilEd.rutina': 'Rutina',
  'perfilEd.eligeRutina': 'Cómo repartes la semana',
  'perfilEd.edad': 'Edad',
  'perfilEd.peso': 'Peso (kg)',
  'perfilEd.genero': 'Género',
  'perfilEd.sobreTi': 'Sobre ti',
  'perfilEd.sobreTiEjemplo': 'Qué entrenas, cómo te gusta entrenar, si buscas a alguien constante…',
  'perfilEd.caracteres': '{cuantos} caracteres',
  'perfilEd.avisarme': 'Avisarme por correo',
  'perfilEd.avisarmeNota': 'Solo cuando alguien quiere entrenar contigo o te propone un día, y solo si sigue sin respuesta diez minutos después.',

  'perfilEd.levantamientos': 'Mis levantamientos',
  'perfilEd.deTres': '{cuantos} de {tope}',
  'perfilEd.levantamientosNota': 'Cuenta 10 de los 100 puntos de compatibilidad.',
  'perfilEd.levantamientosNota2': 'Pon el peso de la barra completa y a cuántas repeticiones lo mueves.',
  'perfilEd.porQueLosBasicos': 'Los tres primeros van sugeridos porque para compararos hacen falta los mismos: solo se puede saber si podéis cubriros cuando los dos habéis apuntado el mismo ejercicio.',
  'perfilEd.ejercicio': 'Ejercicio',
  'perfilEd.eligeEjercicio': 'Elige un ejercicio',
  'perfilEd.pesoEnKilos': 'Peso en kilos',
  'perfilEd.repeticiones': 'Repeticiones',
  'perfilEd.reps': 'reps',
  'perfilEd.quitarLevantamiento': 'Quitar este levantamiento',
  'perfilEd.anadirLevantamiento': 'Añadir levantamiento',

  'perfilEd.marcas': 'Mis marcas',
  'perfilEd.borrarMarca': 'Borrar {titulo}',
  'perfilEd.borrar': 'Borrar',
  'perfilEd.tituloMarca': 'Título de la marca',
  'perfilEd.tituloMarcaEjemplo': 'Sentadilla 140 kg × 3',
  'perfilEd.descripcion': 'Descripción',
  'perfilEd.descripcionEjemplo': 'Cómo fue (opcional)',
  'perfilEd.fecha': 'Fecha',
  'perfilEd.archivoListo': 'Archivo listo',
  'perfilEd.fotoOVideo': 'Foto o vídeo',
  'perfilEd.anadir': 'Añadir',

  'perfilEd.metaSemanal': 'Meta semanal',
  'perfilEd.dias': { uno: '{cuenta} día', otros: '{cuenta} días' },
  'perfilEd.diasPorSemana': 'Días de entrenamiento por semana',

  'perfilEd.gimnasio': 'Gimnasio habitual',
  'perfilEd.dondeEntrenas': '¿Dónde entrenas?',
  'perfilEd.eligeGimnasio': 'Selecciona tu gimnasio',
  'perfilEd.nuevoGimnasio': '+ Añadir uno nuevo',
  'perfilEd.nombreGimnasio': 'Nombre del gimnasio',
  'perfilEd.nombreGimnasioEjemplo': 'Ej: McFit Centro',
  'perfilEd.puedoDesplazarme': 'Puedo entrenar en otro gimnasio',
  'perfilEd.puedoDesplazarmeNota': 'Si te viene bien desplazarte de vez en cuando, saldrá gente que entrena cerca pero no en tu sala. Cuenta menos que compartir gimnasio, porque moverse cuesta.',

  'perfilEd.horarios': 'Horarios de entrenamiento',
  'perfilEd.anadirDia': 'Añadir día',
  'perfilEd.horariosAyuda': 'Es el dato que más pesa en el emparejamiento. Marca «Voy siempre» en las franjas a las que acudes de forma fija: coincidir una hora a la que los dos vais seguro cuenta más que muchas horas de disponibilidad suelta. Puedes marcar hasta {tope}.',
  'perfilEd.diaSemana': 'Día de la semana',
  'perfilEd.horaInicio': 'Hora de inicio',
  'perfilEd.horaFin': 'Hora de fin',
  'perfilEd.voySiempre': 'Voy siempre',
  'perfilEd.puedoIr': 'Puedo ir',
  'perfilEd.sinHorarios': 'No tienes horarios añadidos.',
  'perfilEd.franjasFijas': 'Franjas fijas: {puestas} de {tope}',

  'perfilEd.bloqueados': 'Personas bloqueadas',
  'perfilEd.sinBloqueados': 'No has bloqueado a nadie.',
  'perfilEd.bloqueadosNota': 'No os veis en la aplicación. Quitar el bloqueo no os vuelve a conectar: eso lo decidís otra vez.',
  'perfilEd.quitarBloqueo': 'Quitar el bloqueo',

  'perfilEd.contrasena': 'Contraseña',
  'perfilEd.contrasenaNota': 'Al cambiarla se cierran todas las sesiones abiertas, incluida esta.',
  'perfilEd.cambiarContrasena': 'Cambiar mi contraseña',
  'perfilEd.contrasenaActual': 'Contraseña actual',
  'perfilEd.contrasenaActualEjemplo': 'Tu contraseña de ahora',
  'perfilEd.contrasenaNueva': 'Contraseña nueva',
  'perfilEd.cambiando': 'Cambiando…',
  'perfilEd.cambiar': 'Cambiar contraseña',
  'perfilEd.errorContrasena': 'No se ha podido cambiar la contraseña.',

  'perfilEd.borrarCuenta': 'Borrar mi cuenta',
  'perfilEd.borrarCuentaNota': 'Se borra todo: tu perfil, tus horarios, tus marcas y tus entrenamientos.',
  'perfilEd.borrarCuentaNota2': 'También las conversaciones enteras, que desaparecen igual para la otra persona. No se puede deshacer.',
  'perfilEd.quieroBorrar': 'Quiero borrar mi cuenta',
  'perfilEd.escribeContrasena': 'Escribe tu contraseña para confirmarlo.',
  'perfilEd.tuContrasena': 'Tu contraseña',
  'perfilEd.borrando': 'Borrando…',
  'perfilEd.borrarParaSiempre': 'Borrar para siempre',
  'perfilEd.errorBorrado': 'No se ha podido borrar la cuenta.',
  'perfilEd.guardado': 'Perfil actualizado.',
  'perfilEd.errorGuardar': 'Hubo un error al guardar tu perfil.',
  'perfilEd.errorFoto': 'No se ha podido subir la foto.',
  'perfilEd.errorArchivo': 'No se ha podido subir el archivo.',
  'perfilEd.errorMarca': 'No se ha podido guardar la marca.',
  'perfilEd.errorBorrarMarca': 'No se ha podido borrar.',
  'perfilEd.topeFijas': 'Puedes marcar {tope} franjas como fijas.',

  // ===================== Solicitudes =====================
  'solicitudes.titulo': 'Solicitudes',
  'solicitudes.subtitulo': 'Lo que espera tu respuesta.',
  'solicitudes.irACompaneros': 'Ir a Compañeros',
  'solicitudes.proponeEntrenar': 'Propone entrenar el {cuando}, de {desde} a {hasta}.',
  'solicitudes.proponeEntrenarEn': 'Propone entrenar el {cuando}, de {desde} a {hasta} en {gimnasio}.',
  'solicitudes.rechazar': 'Rechazar',
  'solicitudes.aceptar': 'Aceptar',
  'solicitudes.vacioTitulo': 'No hay nada pendiente',
  'solicitudes.vacioTexto': 'Cuando alguien quiera entrenar contigo, la solicitud aparecerá aquí sin que tengas que recargar.',
  'solicitudes.quiereConectar': 'Quiere conectar contigo para entrenar.',
  'solicitudes.verPerfil': 'Ver su perfil',
  'solicitudes.compatibles': 'compatibles',
  'solicitudes.aceptada': 'Aceptada. Ya podéis hablar desde Compañeros.',
  'solicitudes.rechazada': 'Solicitud rechazada.',
  'solicitudes.error': 'No se ha podido procesar la respuesta.',

  // ===================== ¿Acierta el motor? =====================
  'embudo.titulo': '¿Acierta el motor?',
  'embudo.texto': 'La compatibilidad se calcula con ocho factores y unos pesos que salen de razonar sobre cómo funciona un gimnasio, no de haber mirado qué pasa después. Esta tabla mira qué pasa después: de las solicitudes que salieron con cada puntuación, cuántas se aceptaron y cuántas acabaron en un entrenamiento que las dos personas confirmaron.',
  'embudo.error': 'No se ha podido cargar el embudo.',
  'embudo.tabla': 'Solicitudes enviadas, aceptadas y acabadas en entrenamiento, por tramo de compatibilidad',
  'embudo.compatibilidad': 'Compatibilidad',
  'embudo.enviadas': 'Enviadas',
  'embudo.aceptadas': 'Se aceptaron',
  'embudo.entrenaron': 'Entrenaron',

  /* Los tres tramos de la escala, con el umbral escrito: sin él, "alta" y
     "media" son etiquetas que cada uno interpreta a su manera. */
  'embudo.tramoAlta': 'Alta (70 % o más)',
  'embudo.tramoMedia': 'Media (40–69 %)',
  'embudo.tramoBaja': 'Baja (menos de 40 %)',

  'embudo.sinMuestraTitulo': 'Todavía no se puede concluir nada.',
  'embudo.sinMuestraTexto': 'Hacen falta al menos {minimo} solicitudes en cada tramo para que la diferencia entre unos y otros no se explique por quién pulsó el botón esa semana. Ahora mismo hay {cuantas} medibles en total.',
  'embudo.conMuestra': 'Hay muestra suficiente en los tres tramos: la comparación entre ellos ya significa algo.',
  'embudo.sinPuntuacion': 'Quedan fuera {cuantas} solicitudes anteriores a que se guardara la puntuación. No se rellenan con el número de hoy: la constancia lo mueve sola cada semana y los pesos han cambiado desde entonces, así que sería inventarse el dato.',

  // ===================== Pie =====================
  'pie.derechos': '© 2026 Spotter. Construyendo comunidad.',
  'pie.embudo': '¿Acierta el motor?',

  // ===================== Contraseña nueva =====================
  /* El enlace incompleto sale igual aquí y en la baja por correo: es el mismo
     accidente —copiarlo a mano y dejarse un trozo— y se cuenta igual. */
  'enlace.incompleto': 'Este enlace no está completo',
  'enlace.incompletoTexto': 'Ábrelo desde el correo que recibiste. Si lo copiaste a mano, puede que se quedara alguna parte por el camino.',

  'restablecer.volverAEntrar': 'Volver a entrar',
  'restablecer.hecha': 'Contraseña cambiada',
  'restablecer.hechaTexto': 'Ya puedes entrar con la nueva. Si tenías la sesión abierta en algún otro sitio, se ha cerrado.',
  'restablecer.entrar': 'Entrar',
  'restablecer.titulo': 'Elige una contraseña nueva',
  'restablecer.contrasena': 'Contraseña',
  'restablecer.minimo': 'Al menos {minimo} caracteres',
  'restablecer.faltan': { uno: 'Falta {cuenta} carácter', otros: 'Faltan {cuenta} caracteres' },
  'restablecer.consejo': 'Cuenta la longitud, no los símbolos raros: son más difíciles de adivinar y más fáciles de recordar.',
  'restablecer.guardando': 'Guardando…',
  'restablecer.cambiar': 'Cambiar la contraseña',
  'restablecer.error': 'No se ha podido cambiar la contraseña.',

  // ===================== Baja de los avisos por correo =====================
  'baja.hecha': 'Hecho, no te escribimos más',
  'baja.hechaTexto': 'No vas a recibir más avisos por correo. Las solicitudes y las propuestas te seguirán apareciendo dentro de la aplicación.',
  'baja.deshacer': 'Me he equivocado, quiero seguir recibiéndolos',
  'baja.activada': 'Vuelves a recibirlos',
  'baja.activadaTexto': 'Te avisaremos cuando alguien te mande una solicitud o te proponga entrenar.',
  'baja.dejarDeRecibir': 'Dejar de recibirlos',
  'baja.titulo': '¿Dejar de recibir avisos por correo?',
  'baja.texto1': 'Solo te escribimos por dos cosas, y solo si siguen esperando tu respuesta diez minutos después: cuando alguien quiere entrenar contigo y cuando alguien te propone un día concreto.',
  'baja.texto2': 'Si los desactivas seguirás viéndolo todo dentro de la aplicación; lo que no habrá es nada que te avise cuando la tengas cerrada.',
  'baja.enlaceCaducado': 'Ese enlace ya no vale. Prueba con el de un correo más reciente.',
  'baja.confirmar': 'Sí, dejar de recibirlos',

  // ===================== El chat =====================
  'chat.titulo': 'Compañeros',
  'chat.sinLeer': '{cuenta} sin leer',
  'chat.buscarCompanero': 'Buscar compañero',
  'chat.buscar': 'Buscar',
  'chat.canalCaido': 'Sin conexión en directo. Reintentando…',
  'chat.vacioTitulo': 'Todavía nadie',
  'chat.vacioTexto': 'Cuando alguien acepte tu solicitud aparecerá aquí y podréis hablar.',
  'chat.sinResultados': 'Ningún compañero se llama así.',
  'chat.tu': 'Tú:',
  'chat.sinHablar': 'Aún no habéis hablado',
  'chat.mensajesSinLeer': { uno: '{cuenta} mensaje sin leer', otros: '{cuenta} mensajes sin leer' },

  'chat.eligeCompanero': 'Elige un compañero',
  'chat.eligeTexto': 'Aquí podréis cuadrar el próximo entrenamiento.',
  'chat.volverALista': 'Volver a la lista',
  'chat.companeroDeEntrenamiento': 'Compañero de entrenamiento',
  'chat.dejarPregunta': '¿Dejar de ser compañeros?',
  'chat.dejarDeSerCompaneros': 'Dejar de ser compañeros',
  'chat.no': 'No',
  'chat.si': 'Sí',
  'chat.proponerSesion': 'Proponer sesión',

  /* El plan que hay en marcha, que va arriba y no como un mensaje más: no es una
     frase que se pierde hacia arriba en el historial, es un estado. */
  'chat.enGimnasio': 'en {gimnasio}',
  'chat.entrenasteis': '¿Entrenasteis?',
  'chat.confirmado': 'Confirmado',
  'chat.propuestoPorTi': 'Propuesto por ti · esperando respuesta',
  'chat.proponeQuedar': '{nombre} propone quedar',
  'chat.siEntrenamos': 'Sí, entrenamos',
  'chat.rechazar': 'Rechazar',
  'chat.aceptar': 'Aceptar',

  'chat.hoy': 'Hoy',
  'chat.ayer': 'Ayer',
  'chat.visto': 'Visto',
  'chat.enviado': 'Enviado',
  'chat.sinMensajes': 'Aún no hay mensajes.',
  'chat.sinMensajesTexto': 'Propón una hora y un día concretos: es lo que hace que un match acabe en entrenamiento.',
  'chat.escribeMensaje': 'Escribe un mensaje',
  'chat.mensaje': 'Mensaje',
  'chat.enviarMensaje': 'Enviar mensaje',

  'chat.errorResponder': 'No se ha podido responder.',
  'chat.errorCancelar': 'No se ha podido cancelar.',
  'chat.errorApuntar': 'No se ha podido apuntar.',

  // ===================== Proponer una sesión =====================
  /* El formulario vive en un sitio y se usa en dos: el chat y la ficha de una
     persona. Por eso tiene prefijo propio y no el de ninguna de las dos. */
  'propuesta.titulo': 'Proponer una sesión',
  'propuesta.cerrar': 'Cerrar',
  'propuesta.hueco': 'Es el próximo hueco que compartís. Puedes cambiarlo.',
  'propuesta.huecoFijo': 'Es el próximo hueco que compartís, y los dos vais siempre. Puedes cambiarlo.',
  'propuesta.sinFranjas': 'No tenéis ninguna franja en común, así que hay que elegir día y hora a mano.',
  'propuesta.dia': 'Día',
  'propuesta.desde': 'Desde',
  'propuesta.hasta': 'Hasta',
  'propuesta.donde': '¿Dónde quedáis?',
  'propuesta.dondePista': 'Entrenáis en sitios distintos, así que uno de los dos se desplaza. Miradlo entre vosotros: muchos gimnasios venden entrada suelta o dejan traer invitado.',
  'propuesta.elTuyo': 'El tuyo',
  'propuesta.elSuyo': 'El suyo',
  'propuesta.nota': 'Nota (opcional)',
  'propuesta.notaEjemplo': 'Empiezo por pierna, llevo las bandas…',
  'propuesta.proponer': 'Proponer',
  'propuesta.error': 'No se ha podido proponer la sesión.',

  // ===================== Fichas de una en una =====================
  'sugerencia.cerrar': 'Cerrar',
  'sugerencia.verDesglose': 'Ver de dónde sale',
  'sugerencia.ocultarDesglose': 'Ocultar el desglose',
  'sugerencia.nivel': 'Nivel',
  'sugerencia.objetivo': 'Objetivo',
  'sugerencia.gimnasio': 'Gimnasio',
  'sugerencia.vuestraSemana': 'Vuestra semana',
  'sugerencia.enComun': '{tiempo} en común',
  'sugerencia.conectarCon': 'Conectar con {nombre}',
  'sugerencia.anterior': 'Sugerencia anterior',
  'sugerencia.siguiente': 'Sugerencia siguiente',
  'sugerencia.posicion': '{actual} de {total}',

  /* Cuánto tiempo coincidís, que se dice de tres maneras según lo que salga. Es
     lo mismo que hace `duracion.horasYMinutos` en el backend, y aquí hace falta
     otra vez porque este total lo suma el frontend. */
  'duracion.minutos': '{cuenta} min',
  'duracion.horas': { uno: '{cuenta} hora', otros: '{cuenta} horas' },
  'duracion.horasYMinutos': '{horas}h {minutos}min',

  // ===================== De dónde sale el número =====================
  /* Las etiquetas y los detalles de cada factor llegan redactados del backend,
     que es quien los calcula. Lo de aquí es lo que los rodea.

     El énfasis va en una frase entera y no en dos palabras dentro de otra: para
     poner <strong> en medio habría que partir el texto en trozos y volver a
     unirlos en el mismo orden en los dos idiomas. */
  'desglose.noResta': 'Esto no resta.',
  'desglose.avisoResto': 'No se ha podido mirar, así que su peso se reparte entre lo demás.',
  'desglose.sinDatos': 'sin datos',
  'desglose.nota': 'Con el perfil completo el número se parece más a lo que de verdad encajáis.',

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
