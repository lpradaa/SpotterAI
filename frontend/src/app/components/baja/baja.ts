import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { api } from '../../config/api';

/**
 * Dejar de recibir avisos por correo, desde el enlace del propio correo.
 *
 * <p>Es la única pantalla de la aplicación que funciona sin sesión, y es
 * deliberado: quien quiere dejar de recibir correos no va a iniciar sesión para
 * conseguirlo. Lo que hace es marcar el remitente como no deseado, y a partir de
 * ahí tampoco le llegan los avisos que sí quería.
 *
 * <p>Y no da de baja al abrirla. El botón existe porque los antivirus de correo
 * y los previsualizadores de las bandejas siguen los enlaces por su cuenta: con
 * una baja de un solo clic, media lista se daría de baja sola sin que nadie
 * hubiera tocado nada, y en el registro se vería gente dándose de baja con toda
 * normalidad.
 */
@Component({
  selector: 'app-baja',
  standalone: true,
  templateUrl: './baja.html',
  styleUrl: './baja.scss',
})
export class BajaComponent {
  private http = inject(HttpClient);
  private ruta = inject(ActivatedRoute);

  private token = this.ruta.snapshot.queryParamMap.get('t') ?? '';

  protected sinEnlace = !this.token;
  protected enviando = signal(false);
  protected error = signal(false);

  /** null mientras no se ha hecho nada; luego, si quedan activados o no. */
  protected resultado = signal<boolean | null>(null);

  protected cambiar(quiereRecibir: boolean): void {
    this.enviando.set(true);
    this.error.set(false);

    this.http
      .post(api(quiereRecibir ? '/api/avisos/alta' : '/api/avisos/baja'), { token: this.token })
      .subscribe({
        next: () => {
          this.resultado.set(quiereRecibir);
          this.enviando.set(false);
        },
        error: () => {
          this.error.set(true);
          this.enviando.set(false);
        },
      });
  }
}
