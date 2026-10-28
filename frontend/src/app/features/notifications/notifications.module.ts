import { Component, Inject, NgModule, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { NotificationApi } from '../../core/api/notification.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  NOTIFICATION_STATUSES, NotificationAttempt, NotificationResponse, NotificationStatus, Page, PageQuery
} from '../../core/models';
import { PagedList } from '../../core/services/paged-list';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-attempts-dialog',
  template: `
    <h2 mat-dialog-title>Delivery attempts</h2>
    <mat-dialog-content>
      <p *ngIf="loading">Loading…</p>
      <p *ngIf="error" class="error-text">{{ error }}</p>
      <app-empty-state *ngIf="!loading && !error && !attempts.length" message="No attempts recorded."></app-empty-state>
      <mat-list *ngIf="attempts.length">
        <mat-list-item *ngFor="let a of attempts">
          #{{ a.attemptNo }} <app-badge [value]="a.outcome"></app-badge> {{ a.attemptedAt | date:'medium' }}
          <span *ngIf="a.error" class="muted"> — {{ a.error }}</span>
        </mat-list-item>
      </mat-list>
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Close</button></mat-dialog-actions>`
})
export class AttemptsDialogComponent implements OnInit {
  attempts: NotificationAttempt[] = [];
  loading = true;
  error: string | null = null;
  constructor(private api: NotificationApi, @Inject(MAT_DIALOG_DATA) public data: { id: string }) {}
  ngOnInit(): void {
    this.api.attempts(this.data.id).subscribe({
      next: a => { this.attempts = a; this.loading = false; },
      error: () => { this.error = 'Could not load attempts.'; this.loading = false; }
    });
  }
}

@Component({
  selector: 'app-notifications',
  template: `
    <div class="page">
      <div class="page-header"><h1>Notifications</h1></div>
      <p class="muted">Delivery uses a mock channel; nothing is emailed or texted. Notifications can take a few seconds to appear.</p>
      <div class="filters">
        <mat-slide-toggle [checked]="unreadOnly" (change)="unreadOnly = $event.checked; resetAndReload()">Unread only</mat-slide-toggle>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Delivery status</mat-label>
          <mat-select [value]="status" (selectionChange)="status = $event.value; resetAndReload()">
            <mat-option [value]="null">Any</mat-option>
            <mat-option *ngFor="let s of statuses" [value]="s">{{ s }}</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-stroked-button type="button" (click)="reload()">Refresh</button>
      </div>
      <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
      <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
      <app-empty-state *ngIf="!loading && !error && !items.length" icon="notifications_none" message="No notifications."></app-empty-state>
      <table mat-table [dataSource]="items" matSort (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
        <ng-container matColumnDef="subject">
          <th mat-header-cell *matHeaderCellDef>Notification</th>
          <td mat-cell *matCellDef="let n">
            <strong [class.unread]="!n.readAt">{{ n.subject }}</strong><br><span class="muted">{{ n.message }}</span>
          </td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="status">Delivery</th>
          <td mat-cell *matCellDef="let n"><app-badge [value]="n.status"></app-badge></td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="createdAt">Created</th>
          <td mat-cell *matCellDef="let n">{{ n.createdAt | date:'medium' }}</td>
        </ng-container>
        <ng-container matColumnDef="readAt">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="readAt">Read</th>
          <td mat-cell *matCellDef="let n">{{ n.readAt ? (n.readAt | date:'short') : 'Unread' }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let n">
            <a mat-button *ngIf="n.referenceType === 'WORK_ORDER'" [routerLink]="['/work-orders', n.referenceId]" (click)="read(n)">Open</a>
            <button mat-button type="button" *ngIf="!n.readAt" (click)="read(n)">Mark read</button>
            <button mat-icon-button type="button" *ngIf="auth.isAdmin" aria-label="Delivery attempts" (click)="attempts(n)">
              <mat-icon>history</mat-icon>
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr>
        <tr mat-row *matRowDef="let r; columns: cols"></tr>
      </table>
      <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
    </div>`,
  styles: ['.filters { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 8px; } .unread { font-weight: 700; }']
})
export class NotificationsComponent extends PagedList<NotificationResponse> implements OnInit {
  unreadOnly = false;
  status: NotificationStatus | null = null;
  readonly statuses = NOTIFICATION_STATUSES;
  readonly cols = ['subject', 'status', 'createdAt', 'readAt', 'actions'];

  constructor(private api: NotificationApi, public auth: AuthService, private dialog: MatDialog) { super(); }

  ngOnInit(): void { this.reload(); }

  protected fetch(q: PageQuery): Observable<Page<NotificationResponse>> {
    return this.api.list(q, this.unreadOnly ? true : null, this.status);
  }

  read(n: NotificationResponse): void {
    if (n.readAt) { return; }
    this.api.markRead(n.id).subscribe(u => { n.readAt = u.readAt; });
  }

  attempts(n: NotificationResponse): void {
    this.dialog.open(AttemptsDialogComponent, { data: { id: n.id }, width: '480px' });
  }
}

@NgModule({
  declarations: [NotificationsComponent, AttemptsDialogComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: NotificationsComponent }])]
})
export class NotificationsModule {}
