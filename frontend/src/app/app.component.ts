import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavigationHistoryService } from './services/navigation-history.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html'
})
export class AppComponent {
  constructor(private navigationHistory: NavigationHistoryService) {
  }
}
