import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from './components/header/header';
import { Footer } from './components/footer/footer';
import { EventosService } from './services/eventos.service';
import { AvisosService } from './services/avisos.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly title = signal('SpotterAI');

  private eventos = inject(EventosService);
  private avisos = inject(AvisosService);

  ngOnInit(): void {
    // El canal se abre aquí y no en cada pantalla: es uno por sesión, no uno por
    // vista. Si no hay token no hace nada, y al entrar el login lo arranca.
    this.eventos.conectar();
    this.avisos.iniciar();
  }
}
