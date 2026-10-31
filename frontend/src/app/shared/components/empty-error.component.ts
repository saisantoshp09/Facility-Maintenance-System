import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  template: `
    <div class="empty" role="status">
      <mat-icon>{{ icon }}</mat-icon>
      <p>{{ message }}</p>
      <ng-content></ng-content>
    </div>`,
  styles: ['.empty { text-align: center; padding: 32px 16px; color: rgba(0,0,0,.6); } mat-icon { font-size: 40px; height: 40px; width: 40px; }']
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() message = 'Nothing to show.';
}

@Component({
  selector: 'app-error-banner',
  template: `
    <div *ngIf="message" class="banner" role="alert">
      <mat-icon>error_outline</mat-icon>
      <span class="msg">{{ message }}</span>
      <button mat-stroked-button type="button" (click)="retry.emit()">Retry</button>
    </div>`,
  styles: ['.banner { display: flex; align-items: center; gap: 12px; background: #ffebee; color: #b71c1c; padding: 8px 16px; margin-bottom: 12px; } .msg { flex: 1; }']
})
export class ErrorBannerComponent {
  @Input() message: string | null = null;
  @Output() retry = new EventEmitter<void>();
}
