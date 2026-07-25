import { Component, OnInit, inject, ChangeDetectorRef, ViewChild, ElementRef, AfterViewChecked, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { Avatar } from '../avatar/avatar';

@Component({
  selector: 'app-mis-conexiones',
  standalone: true,
  imports: [CommonModule, FormsModule, Avatar],
  templateUrl: './mis-conexiones.html',
  styleUrl: './mis-conexiones.scss'
})
export class MisConexionesComponent implements OnInit, AfterViewChecked {
  private usuarioService = inject(UsuarioService);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private eventos = inject(EventosService);
  private destroyRef = inject(DestroyRef);

  @ViewChild('scrollMe') private myScrollContainer!: ElementRef;

  // --- ESTADO DE LA LISTA ---
  conexiones: any[] = [];
  miId!: number;
  busqueda: string = ''; 

  // --- ESTADO DEL CHAT ---
  chatActivo: { id: number, nombre: string, avatar: string } | null = null;
  historial: any[] = [];
  nuevoMensaje: string = '';
  private apiUrl = 'http://localhost:8080/api/mensajes';

  /** Identificadores de compañeros con mensajes sin abrir. */
  noLeidos = new Set<number>();

  /** Para avisar en la cabecera cuando el canal se ha caído. */
  estadoCanal = this.eventos.estado;

  ngOnInit(): void {
    this.miId = Number(localStorage.getItem('userId') || localStorage.getItem('id'));
    this.cargarConexiones();
    this.escucharEventos();
  }

  /**
   * Los mensajes que llegan por el canal se pintan sin pedir nada al servidor:
   * el evento trae el mensaje entero. Si la conversación no está abierta, se
   * marca el contacto y ya está.
   */
  private escucharEventos(): void {
    this.eventos.mensajes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(msg => {
        if (this.chatActivo && msg.emisorId === this.chatActivo.id) {
          this.historial = [...this.historial, msg];
          this.cdr.detectChanges();
          this.scrollToBottom();
        } else {
          this.noLeidos.add(Number(msg.emisorId));
          this.cdr.detectChanges();
        }
      });

    // Una solicitud aceptada estrena compañero, y por tanto conversación.
    this.eventos.respuestas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        if (solicitud.estado === 'ACEPTADA') this.cargarConexiones();
      });
  }

  /**
   * Si se ha llegado desde el tablero con ?con=ID, se abre esa conversación en
   * cuanto están cargadas las conexiones.
   */
  private abrirDesdeLaUrl(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('con'));
    if (!id) return;

    const conexion = this.conexiones.find(c => this.nombreODeId(c).id === id);
    if (conexion) this.abrirChat(conexion);
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      if (this.myScrollContainer) {
        this.myScrollContainer.nativeElement.scrollTop = this.myScrollContainer.nativeElement.scrollHeight;
      }
    } catch(err) { }
  }

  cargarConexiones(): void {
    this.usuarioService.obtenerMisConexiones().subscribe({
      next: (data) => {
        this.conexiones = data;
        this.abrirDesdeLaUrl();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al cargar conexiones:', err)
    });
  }

  // Buscador rápido en memoria
  get conexionesFiltradas() {
    if (!this.busqueda.trim()) return this.conexiones;
    return this.conexiones.filter(c =>
      this.nombreODeId(c).nombre.toLowerCase().includes(this.busqueda.toLowerCase()));
  }

  /**
   * Quién es "el otro" en una solicitud.
   *
   * La plantilla repetía este ternario cinco veces para sacar nombre, inicial e
   * identificador; ahora se resuelve en un sitio.
   */
  nombreODeId(solicitud: any): { id: number, nombre: string, inicial: string } {
    const soyElEmisor = solicitud.emisorId === this.miId;
    const nombre = soyElEmisor ? solicitud.receptorNombre : solicitud.emisorNombre;
    return {
      id: Number(soyElEmisor ? solicitud.receptorId : solicitud.emisorId),
      nombre: nombre ?? 'Compañero',
      inicial: (nombre ?? '?').charAt(0).toUpperCase()
    };
  }

  /** Vuelve a la lista. En móvil las dos vistas no caben a la vez. */
  cerrarChat(): void {
    this.chatActivo = null;
    this.historial = [];
    this.cdr.detectChanges();
  }

  // --- LÓGICA DEL CHAT FUSIONADA ---
  abrirChat(solicitud: any): void {
    const otro = this.nombreODeId(solicitud);
    this.chatActivo = { id: otro.id, nombre: otro.nombre, avatar: otro.inicial };
    this.noLeidos.delete(otro.id);
    this.cargarHistorial();
  }

  cargarHistorial(): void {
    if (!this.chatActivo) return;
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

    this.http.get<any[]>(`${this.apiUrl}/historial/${this.chatActivo.id}`, { headers }).subscribe({
      next: (data) => {
        this.historial = data;
        this.cdr.detectChanges(); 
        this.scrollToBottom(); 
      },
      error: (err) => console.error('Error al cargar el historial:', err)
    });
  }

  enviar(): void {
    if (!this.nuevoMensaje.trim() || !this.chatActivo) return;

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({ 
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    // La respuesta ya trae el mensaje guardado, así que se añade directamente.
    // Antes se recargaba el historial entero por cada mensaje enviado.
    this.http.post<any>(`${this.apiUrl}/enviar/${this.chatActivo.id}`, this.nuevoMensaje, { headers }).subscribe({
      next: (mensaje) => {
        this.nuevoMensaje = '';
        this.historial = [...this.historial, mensaje];
        this.cdr.detectChanges();
        this.scrollToBottom();
      },
      error: (err) => console.error('Error al enviar el mensaje:', err)
    });
  }

}