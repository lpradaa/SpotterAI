import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IdiomaService } from '../../services/idioma.service';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
  styleUrl: './footer.scss'
})
export class Footer {

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);
}
