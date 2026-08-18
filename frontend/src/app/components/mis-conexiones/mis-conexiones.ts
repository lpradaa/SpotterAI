import {
  Component, OnInit, inject, ChangeDetectorRef, ViewChild, ElementRef,
  AfterViewChecked, DestroyRef, signal, computed
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EventosService } from '../../services/eventos.service';
import { MensajesService, Conversacion, Mensaje } from '../../services/mensajes.service';
import { AvisosService } from '../../services/avisos.service';
import { UsuarioService } from '../../services/usuario.service';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { ProponerSesionComponent } from '../proponer-sesion/proponer-sesion';
import { Avatar } from '../avatar/avatar';
import { PerfilesService } from '../../services/perfiles.service';
import { IdiomaService } from '../../services/idioma.service';

/**
 * Una fila del historial: o un mensaje, o la marca del día en que empieza.
 *
 * Las marcas no son mensajes vacíos con una bandera: eso obliga a que todo lo
 * que recorra el historial se acuerde de saltárselas.
 */
export type LineaDelChat =
  | { tipo: 'dia'; clave: string; etiqueta: string }
  | { tipo: 'mensaje'; msg: Mensaje };

@Component({
  selector: 'app-mis-conexiones',
  standalone: true,
  imports: [CommonModule, FormsModule, Avatar, ProponerSesionComponent],
  templateUrl: './mis-conexiones.html',
  styleUrl: './mis-conexiones.scss'
})
export class MisConexionesComponent implements OnInit, AfterViewChecked {
  private cdr = inject(ChangeDetectorRef);
  private perfiles = inject(PerfilesService);

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /** La foto de alguien, resuelta a URL servible. */
  foto(ruta: string | null | undefined): string | null {
    return this.perfiles.urlDeMedio(ruta ?? null);
  }

  private route = inject(ActivatedRoute);
  private eventos = inject(EventosService);
  private mensajes = inject(MensajesService);
  private avisos = inject(AvisosService);
  private usuarios = inject(UsuarioService);
  private sesiones = inject(SesionesService);
  private destroyRef = inject(DestroyRef);

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  // --- ESTADO DE LA LISTA ---
  conversaciones = signal<Conversacion[]>([]);
  miId!: number;
  busqueda = signal('');

  // --- ESTADO DEL CHAT ---
  chatActivo = signal<Conversacion | null>(null);
  historial = signal<Mensaje[]>([]);
  nuevoMensaje = '';
  enviando = signal(false);

  estadoCanal = this.eventos.estado;

  /** Confirmación en línea, sin diálogo del navegador. */
  confirmandoBorrado = signal(false);

  // --- SESIÓN ---
  /**
   * La sesión que hay en marcha con quien tienes abierto, si la hay.
   *
   * Vive arriba del chat y no como un mensaje más: un plan no es una frase que
   * pasa y se pierde hacia arriba en el historial, es un estado. Si estáis
   * quedando el martes, eso tiene que verse sin buscar.
   */
  sesionActiva = signal<Sesion | null>(null);

  /** Formulario de propuesta abierto. */
  proponiendo = signal(false);
  errorSesion = signal<string | null>(null);

  totalSinLeer = computed(() =>
    this.conversaciones().reduce((suma, c) => suma + c.sinLeer, 0));

  conversacionesFiltradas = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    if (!texto) return this.conversaciones();
    return this.conversaciones().filter(c => c.nombre.toLowerCase().includes(texto));
  });

  /**
   * El id de tu último mensaje, que es el único que lleva el "visto".
   *
   * En todos sería una columna de "Visto" repetida que no aporta nada: lo que
   * se quiere saber es si lo último que dijiste ha llegado a leerse.
   */
  ultimoMio = computed(() => {
    const otro = this.chatActivo()?.usuarioId;
    if (!otro) return null;

    const mios = this.historial().filter(m => m.emisorId !== otro);
    return mios.length ? mios[mios.length - 1].id : null;
  });

  /**
   * El historial con una marca de día donde cambia el día.
   *
   * Cada burbuja llevaba solo la hora, así que una conversación de tres semanas
   * era una columna de "18:30" sin saber a qué día pertenecía ninguna. En una
   * aplicación cuyo asunto entero es acordar cuándo entrenar, eso no es un
   * detalle de presentación: "quedamos mañana a las 7" leído tres días después
   * es información falsa, y no hay forma de darse cuenta.
   *
   * Se arma aquí y no en la plantilla —comparando con el mensaje anterior sobre
   * la marcha— porque así se puede probar sin montar el componente entero, que
   * es lo que hace que esto se quede probado.
   */
  lineas = computed<LineaDelChat[]>(() => {
    const salida: LineaDelChat[] = [];
    let diaAnterior = '';

    for (const msg of this.historial()) {
      const dia = new Date(msg.fechaEnvio).toDateString();
      if (dia !== diaAnterior) {
        salida.push({ tipo: 'dia', clave: dia, etiqueta: this.etiquetaDeDia(msg.fechaEnvio) });
        diaAnterior = dia;
      }
      salida.push({ tipo: 'mensaje', msg });
    }
    return salida;
  });

  /**
   * "Hoy", "Ayer" o la fecha escrita.
   *
   * Los dos primeros son los que se usan al hablar, y son justo los días en los
   * que un plan todavía sirve. A partir de ahí se pone la fecha entera: "jue"
   * a secas vuelve a ser ambiguo en cuanto pasa una semana.
   */
  etiquetaDeDia(fecha: string): string {
    const d = new Date(fecha);
    const hoy = new Date();
    const ayer = new Date(hoy);
    ayer.setDate(hoy.getDate() - 1);

    if (d.toDateString() === hoy.toDateString()) return this.i18n.t('chat.hoy');
    if (d.toDateString() === ayer.toDateString()) return this.i18n.t('chat.ayer');

    const mismoAno = d.getFullYear() === hoy.getFullYear();
    return this.i18n.fecha(d, {
      weekday: 'long', day: 'numeric', month: 'long',
      ...(mismoAno ? {} : { year: 'numeric' }),
    });
  }

  /** La fecha completa, para el title y el atributo datetime de cada burbuja. */
  fechaCompleta(fecha: string): string {
    return this.i18n.fecha(fecha, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }

  /**
   * La hora de cada burbuja.
   *
   * <p>Un método y no `| date:'shortTime'`: el pipe de Angular usa `LOCALE_ID`,
   * que se fija al arrancar y no se puede cambiar en caliente — que es justo lo
   * que hace el selector de idioma. Era el único `| date` de las veinte
   * plantillas.
   */
  hora(fecha: string): string {
    return this.i18n.fecha(fecha, { hour: '2-digit', minute: '2-digit' });
  }

  ngOnInit(): void {
    this.miId = Number(localStorage.getItem('userId') || localStorage.getItem('id'));
    this.cargarConversaciones();
    this.escucharEventos();
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      if (this.myScrollContainer) {
        this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
      }
    } catch { }
  }

  cargarConversaciones(): void {
    this.mensajes.conversaciones().subscribe({
      next: lista => {
        this.conversaciones.set(lista);
        this.abrirDesdeLaUrl();
        this.cdr.detectChanges();
      },
      error: err => console.error('Error al cargar las conversaciones:', err)
    });
  }

  /** Si se ha llegado desde el tablero con ?con=ID, se abre esa conversación. */
  private abrirDesdeLaUrl(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('con'));
    if (!id || this.chatActivo()) return;

    const conversacion = this.conversaciones().find(c => c.usuarioId === id);
    if (conversacion) this.abrirChat(conversacion);
  }

  private escucharEventos(): void {
    this.eventos.mensajes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(msg => this.alLlegarMensaje(msg));

    // El otro ha abierto la conversación: lo que le mandaste pasa a "visto".
    this.eventos.mensajesLeidos
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(datos => this.alLeerlosElOtro(datos?.conId));

    // Una solicitud aceptada estrena compañero, y por tanto conversación.
    this.eventos.respuestas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        if (solicitud.estado === 'ACEPTADA') this.cargarConversaciones();
      });

    this.eventos.relacionesDeshechas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(datos => this.alDeshacerseLaRelacion(datos));

    // Una propuesta que llega o se responde mientras miras el chat: se pinta
    // sola. Sin esto habría que recargar para enterarse de que te han dicho que
    // sí, que es justo lo que el canal de eventos vino a resolver.
    this.eventos.sesiones
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(sesion => this.alCambiarLaSesion(sesion));

    this.eventos.sesionesRespondidas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(sesion => this.alCambiarLaSesion(sesion));
  }

  private alCambiarLaSesion(sesion: Sesion): void {
    if (this.chatActivo()?.usuarioId !== sesion.conId) return;

    // Una rechazada o cancelada deja de ocupar sitio: no hay nada que hacer con
    // ella y el hueco es para la siguiente propuesta.
    const sigueViva = sesion.estado === 'PROPUESTA' || sesion.estado === 'ACEPTADA';
    this.sesionActiva.set(sigueViva ? sesion : null);
    this.cdr.detectChanges();
  }

  /**
   * El otro ha abierto la conversación que tienes con él.
   *
   * Solo se tocan los tuyos: los suyos ya estaban leídos por tu parte desde que
   * los viste.
   */
  private alLeerlosElOtro(conId: number | undefined): void {
    if (!conId || this.chatActivo()?.usuarioId !== conId) return;

    this.historial.update(lista =>
      lista.map(m => (m.emisorId === conId ? m : { ...m, leido: true })));
  }

  private alLlegarMensaje(msg: Mensaje): void {
    const abierto = this.chatActivo();
    const esDelChatAbierto = abierto && msg.emisorId === abierto.usuarioId;

    if (esDelChatAbierto) {
      this.historial.update(lista => [...lista, msg]);
      // Lo estás viendo, así que ya está leído; el servidor no puede saberlo.
      this.mensajes.marcarLeida(abierto.usuarioId).subscribe({ error: () => {} });
    }

    this.refrescarFila(msg.emisorId, msg, !esDelChatAbierto);
    this.cdr.detectChanges();
    if (esDelChatAbierto) this.scrollToBottom();
  }

  /**
   * Actualiza en el sitio la fila de una conversación y la sube arriba.
   *
   * Se hace en local en vez de volver a pedir la lista: el evento ya trae todo
   * lo que cambia, y una petición por mensaje recibido sobra.
   */
  private refrescarFila(otroId: number, msg: Mensaje, sumaSinLeer: boolean): void {
    this.conversaciones.update(lista => {
      const actualizada = lista.map(c => c.usuarioId !== otroId ? c : {
        ...c,
        ultimoMensaje: msg.contenido,
        ultimaFecha: msg.fechaEnvio,
        mioElUltimo: msg.emisorId === this.miId,
        sinLeer: sumaSinLeer ? c.sinLeer + 1 : 0
      });

      return actualizada.sort((a, b) => {
        if (!a.ultimaFecha) return 1;
        if (!b.ultimaFecha) return -1;
        return b.ultimaFecha.localeCompare(a.ultimaFecha);
      });
    });
  }

  /** Vuelve a la lista. En móvil las dos vistas no caben a la vez. */
  cerrarChat(): void {
    this.chatActivo.set(null);
    this.historial.set([]);
    this.confirmandoBorrado.set(false);
    this.sesionActiva.set(null);
    this.cerrarPropuesta();
    this.cdr.detectChanges();
  }

  eliminarCompanero(): void {
    const destino = this.chatActivo();
    if (!destino) return;

    this.usuarios.deshacerRelacion(destino.usuarioId).subscribe({
      next: () => {
        this.conversaciones.update(lista => lista.filter(c => c.usuarioId !== destino.usuarioId));
        this.cerrarChat();
      },
      error: err => {
        console.error('Error al eliminar el compañero:', err);
        this.confirmandoBorrado.set(false);
      }
    });
  }

  /**
   * El otro lado ha deshecho la relación.
   *
   * Sin esto seguirías viendo un chat que ya no existe, y el primer mensaje que
   * mandaras se llevaría un 403 sin ninguna explicación.
   */
  private alDeshacerseLaRelacion(datos: { usuarioId: number }): void {
    const id = Number(datos.usuarioId);
    this.conversaciones.update(lista => lista.filter(c => c.usuarioId !== id));

    if (this.chatActivo()?.usuarioId === id) this.cerrarChat();
    this.cdr.detectChanges();
  }

  abrirChat(conversacion: Conversacion): void {
    this.chatActivo.set(conversacion);
    this.confirmandoBorrado.set(false);
    this.cerrarPropuesta();

    this.sesiones.conMigo(conversacion.usuarioId).subscribe(sesion => {
      this.sesionActiva.set(sesion);
      this.cdr.detectChanges();
    });

    // El backend las marca leídas al servir el historial, así que el contador
    // local se pone a cero sin esperar a la respuesta.
    this.conversaciones.update(lista =>
      lista.map(c => c.usuarioId === conversacion.usuarioId ? { ...c, sinLeer: 0 } : c));

    // El indicador de la cabecera cuenta lo mismo, así que hay que bajarlo.
    this.avisos.refrescar();

    this.mensajes.historial(conversacion.usuarioId).subscribe({
      next: data => {
        this.historial.set(data);
        this.cdr.detectChanges();
        this.scrollToBottom();
      },
      error: err => console.error('Error al cargar el historial:', err)
    });
  }

  enviar(): void {
    const texto = this.nuevoMensaje.trim();
    const destino = this.chatActivo();
    if (!texto || !destino || this.enviando()) return;

    this.enviando.set(true);

    this.mensajes.enviar(destino.usuarioId, texto).subscribe({
      next: mensaje => {
        this.nuevoMensaje = '';
        this.enviando.set(false);
        this.historial.update(lista => [...lista, mensaje]);
        this.refrescarFila(destino.usuarioId, mensaje, false);
        this.cdr.detectChanges();
        this.scrollToBottom();
      },
      error: err => {
        this.enviando.set(false);
        console.error('Error al enviar el mensaje:', err);
      }
    });
  }

  // ================= Quedar =================

  /**
   * Abre el formulario.
   *
   * Ya no carga la sugerencia aquí: eso lo hace <app-proponer-sesion> al
   * montarse, que es quien la necesita. El chat solo decide si se ve.
   */
  abrirPropuesta(): void {
    this.proponiendo.set(true);
    this.errorSesion.set(null);
    this.confirmandoBorrado.set(false);
  }

  cerrarPropuesta(): void {
    this.proponiendo.set(false);
    this.errorSesion.set(null);
  }

  /** La propuesta ya está creada: pasa a ser el plan que se ve arriba. */
  alProponer(sesion: Sesion): void {
    this.sesionActiva.set(sesion);
    this.cerrarPropuesta();
    this.cdr.detectChanges();
  }

  responderSesion(acepta: boolean): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    const peticion = acepta ? this.sesiones.aceptar(sesion.id) : this.sesiones.rechazar(sesion.id);

    peticion.subscribe({
      next: actualizada => {
        this.sesionActiva.set(acepta ? actualizada : null);
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorSesion.set(err?.error?.error ?? this.i18n.t('chat.errorResponder'));
        this.cdr.detectChanges();
      }
    });
  }

  cancelarSesion(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    this.sesiones.cancelar(sesion.id).subscribe({
      next: () => {
        this.sesionActiva.set(null);
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorSesion.set(err?.error?.error ?? this.i18n.t('chat.errorCancelar'));
        this.cdr.detectChanges();
      }
    });
  }

  /** "Sí, entrenamos": aquí es donde el match acaba en el historial. */
  confirmarSesion(): void {
    const sesion = this.sesionActiva();
    if (!sesion) return;

    this.sesiones.confirmar(sesion.id).subscribe({
      next: () => {
        this.sesionActiva.set(null);
        this.cdr.detectChanges();
      },
      error: err => {
        this.errorSesion.set(err?.error?.error ?? this.i18n.t('chat.errorApuntar'));
        this.cdr.detectChanges();
      }
    });
  }

  /** "Viernes 3 de julio", que es como se dice una fecha cuando se queda. */
  diaLargo(fecha: string): string {
    return this.i18n.fecha(`${fecha}T00:00:00`,
      { weekday: 'long', day: 'numeric', month: 'long' });
  }

  /** Las horas llegan como "18:00:00" y en un plan sobran los segundos. */
  hhmm(hora: string | null): string {
    return (hora ?? '').slice(0, 5);
  }

  /**
   * Fecha corta al estilo de cualquier app de mensajería: la hora si es de hoy,
   * el día de la semana esta semana, y la fecha si es más viejo.
   */
  cuando(fecha: string | null): string {
    if (!fecha) return '';
    const d = new Date(fecha);
    const ahora = new Date();

    const mismoDia = d.toDateString() === ahora.toDateString();
    if (mismoDia) {
      return this.i18n.fecha(d, { hour: '2-digit', minute: '2-digit' });
    }

    const diasDeDiferencia = Math.floor((ahora.getTime() - d.getTime()) / 86_400_000);
    if (diasDeDiferencia < 7) {
      return this.i18n.fecha(d, { weekday: 'short' });
    }
    return this.i18n.fecha(d, { day: '2-digit', month: '2-digit' });
  }
}
