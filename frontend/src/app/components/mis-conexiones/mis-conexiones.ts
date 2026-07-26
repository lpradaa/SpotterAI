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
import { Avatar } from '../avatar/avatar';

@Component({
  selector: 'app-mis-conexiones',
  standalone: true,
  imports: [CommonModule, FormsModule, Avatar],
  templateUrl: './mis-conexiones.html',
  styleUrl: './mis-conexiones.scss'
})
export class MisConexionesComponent implements OnInit, AfterViewChecked {
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private eventos = inject(EventosService);
  private mensajes = inject(MensajesService);
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

  /** Suma de lo pendiente, para el contador de la cabecera de la lista. */
  totalSinLeer = computed(() =>
    this.conversaciones().reduce((suma, c) => suma + c.sinLeer, 0));

  conversacionesFiltradas = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    if (!texto) return this.conversaciones();
    return this.conversaciones().filter(c => c.nombre.toLowerCase().includes(texto));
  });

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

    // Una solicitud aceptada estrena compañero, y por tanto conversación.
    this.eventos.respuestas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        if (solicitud.estado === 'ACEPTADA') this.cargarConversaciones();
      });
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
    this.cdr.detectChanges();
  }

  abrirChat(conversacion: Conversacion): void {
    this.chatActivo.set(conversacion);

    // El backend las marca leídas al servir el historial, así que el contador
    // local se pone a cero sin esperar a la respuesta.
    this.conversaciones.update(lista =>
      lista.map(c => c.usuarioId === conversacion.usuarioId ? { ...c, sinLeer: 0 } : c));

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
      return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    }

    const diasDeDiferencia = Math.floor((ahora.getTime() - d.getTime()) / 86_400_000);
    if (diasDeDiferencia < 7) {
      return d.toLocaleDateString('es-ES', { weekday: 'short' });
    }
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit' });
  }
}
