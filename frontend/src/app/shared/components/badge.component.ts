import { Component, Input } from '@angular/core';
import { humanize } from '../../core/models';

@Component({
  selector: 'app-badge',
  template: `<span class="badge" [ngClass]="cls">{{ label }}</span>`,
  styles: [`
    .badge { display: inline-block; padding: 2px 10px; margin-right: 4px; border-radius: 12px; font-size: 12px; font-weight: 500; background: #e0e0e0; color: #333; white-space: nowrap; }
    .s-OPEN, .s-PENDING { background: #e3f2fd; color: #0d47a1; }
    .s-APPROVED, .s-ACTIVE, .s-SENT, .s-SUCCESS, .s-COMPLETED { background: #e8f5e9; color: #1b5e20; }
    .s-ASSIGNED, .s-IN_PROGRESS, .s-UNDER_MAINTENANCE { background: #ede7f6; color: #4527a0; }
    .s-ON_HOLD, .s-MEDIUM { background: #fff8e1; color: #8d6e00; }
    .s-RESOLVED { background: #e0f7fa; color: #006064; }
    .s-CLOSED, .s-LOW, .s-RETIRED, .s-CANCELLED, .s-DISABLED { background: #eceff1; color: #455a64; }
    .s-REJECTED, .s-URGENT, .s-FAILED, .s-FAILURE, .s-OVERDUE { background: #ffebee; color: #b71c1c; }
    .s-HIGH { background: #fff3e0; color: #e65100; }
  `]
})
export class BadgeComponent {
  @Input() value: string | null | undefined = '';
  @Input() text?: string;
  get label(): string { return this.text ?? humanize(this.value); }
  get cls(): string { return `s-${this.value ?? ''}`; }
}
