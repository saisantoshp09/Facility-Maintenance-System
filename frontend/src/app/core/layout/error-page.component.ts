import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-error-page',
  template: `
    <div class="page err" role="alert">
      <mat-icon>{{ icon }}</mat-icon>
      <h1>{{ title }}</h1>
      <p class="muted">{{ text }}</p>
      <a mat-flat-button color="primary" routerLink="/dashboard">Back to dashboard</a>
    </div>`,
  styles: ['.err { text-align: center; padding-top: 64px; } mat-icon { font-size: 56px; height: 56px; width: 56px; }']
})
export class ErrorPageComponent {
  title: string; text: string; icon: string;
  constructor(route: ActivatedRoute) {
    const kind = route.snapshot.data['kind'];
    if (kind === 'forbidden') {
      this.title = 'Access denied'; this.icon = 'lock';
      this.text = "You don't have permission to view this page.";
    } else {
      this.title = 'Page not found'; this.icon = 'search_off';
      this.text = 'The page does not exist, or is not available to you.';
    }
  }
}
