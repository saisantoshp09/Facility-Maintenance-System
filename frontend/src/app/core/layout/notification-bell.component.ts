import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { NotificationApi } from '../api/notification.api';
import { NotificationResponse } from '../models';

export const POLL_MS = 45_000;

@Component({
  selector: 'app-notification-bell',
  template: `
    <button mat-icon-button type="button" [matMenuTriggerFor]="menu" (menuOpened)="onOpen()"
            aria-label="Notifications" id="bell-btn">
      <mat-icon [matBadge]="count" [matBadgeHidden]="count === 0" matBadgeColor="warn" matBadgeSize="small">notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" xPosition="before">
      <div class="head" (click)="$event.stopPropagation()">
        <strong>Notifications</strong>
        <span class="muted">{{ count }} unread</span>
      </div>
      <div class="muted pad" *ngIf="loading" (click)="$event.stopPropagation()">Loading…</div>
      <div class="muted pad" *ngIf="!loading && items.length === 0" (click)="$event.stopPropagation()">No notifications yet.</div>
      <button mat-menu-item type="button" *ngFor="let n of items" (click)="open(n)" [class.unread]="!n.readAt">
        <span class="n-text"><strong>{{ n.subject }}</strong><br><span class="muted">{{ n.createdAt | date:'short' }}</span></span>
      </button>
      <a mat-menu-item routerLink="/notifications">View all</a>
    </mat-menu>`,
  styles: ['.head { display: flex; justify-content: space-between; padding: 8px 16px; min-width: 260px; } .pad { padding: 8px 16px; } .unread { background: rgba(63,81,181,.08); } .n-text { line-height: 1.3; }']
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  count = 0;
  items: NotificationResponse[] = [];
  loading = false;
  private sub?: Subscription;
  private onVisibility = () => { if (!document.hidden) { this.refreshCount(); } };

  constructor(private api: NotificationApi, private router: Router) {}

  ngOnInit(): void {
    this.refreshCount();
    // Backend has no push channel; poll the unread counter.
    this.sub = interval(POLL_MS).subscribe(() => { if (!document.hidden) { this.refreshCount(); } });
    document.addEventListener('visibilitychange', this.onVisibility);
  }

  refreshCount(): void {
    this.api.unreadCount().subscribe({ next: r => this.count = r.count, error: () => undefined });
  }

  onOpen(): void {
    this.loading = true;
    this.api.list({ page: 0, size: 5, sort: { field: 'createdAt', dir: 'desc' } }, null, null, true).subscribe({
      next: p => { this.items = p.content; this.loading = false; },
      error: () => { this.items = []; this.loading = false; }
    });
    this.refreshCount();
  }

  open(n: NotificationResponse): void {
    const nav = () => {
      if (n.referenceType === 'WORK_ORDER') { this.router.navigate(['/work-orders', n.referenceId]); }
      else { this.router.navigate(['/notifications']); }
    };
    if (n.readAt) { nav(); return; }
    this.api.markRead(n.id).subscribe({
      next: () => { this.count = Math.max(0, this.count - 1); nav(); },
      error: () => nav()
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    document.removeEventListener('visibilitychange', this.onVisibility);
  }
}
