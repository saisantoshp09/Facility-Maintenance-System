import { Component, NgModule, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Observable, Subscription, combineLatest, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AssetApi } from '../../core/api/asset.api';
import { NotificationApi } from '../../core/api/notification.api';
import { WorkOrderApi } from '../../core/api/work-order.api';
import { AccessResponse, WorkOrderFilter } from '../../core/models';
import { PropertyContext, canUseAssets } from '../../core/services/property-context';
import { SharedModule } from '../../shared/shared.module';

interface Stat { label: string; icon: string; value: number | null; link?: string; query?: Record<string, string>; }

@Component({
  selector: 'app-dashboard',
  template: `
    <div class="page dashboard">
      <header class="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p>An overview of your facility maintenance.</p>
        </div>
      </header>
      <section class="empty-card" *ngIf="ctx.loaded$.value && !(ctx.properties$ | async)?.length" role="status">
        <div class="empty-icon"><mat-icon aria-hidden="true">apartment</mat-icon></div>
        <h2>You are not a member of any property yet.</h2>
        <p>Ask an administrator to add you to a property to see its maintenance activity here.</p>
      </section>
      <ng-container *ngIf="(ctx.selectedId$ | async)">
        <app-error-banner [message]="error" (retry)="load()"></app-error-banner>
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <div class="stats-grid">
          <mat-card *ngFor="let s of stats" class="stat">
            <div class="stat-top">
              <span class="stat-label">{{ s.label }}</span>
              <span class="stat-icon"><mat-icon aria-hidden="true">{{ s.icon }}</mat-icon></span>
            </div>
            <div class="stat-value">{{ s.value === null ? '—' : s.value }}</div>
            <a *ngIf="s.link" class="stat-link" [routerLink]="s.link" [queryParams]="s.query"
               [attr.aria-label]="'View ' + s.label">View details <mat-icon aria-hidden="true">arrow_forward</mat-icon></a>
          </mat-card>
        </div>
      </ng-container>
    </div>`,
  styles: [`
    .dashboard { max-width: 1440px; padding: 32px; }
    .dashboard-header { margin-bottom: 28px; }
    .dashboard-header h1 { margin: 0 0 6px; color: #172b4d; font-size: 26px; line-height: 1.3; font-weight: 600; }
    .dashboard-header p { margin: 0; color: #697d95; font-size: 14px; }
    .empty-card { box-sizing: border-box; display: flex; flex-direction: column; align-items: center;
      text-align: center; padding: 56px 24px; background: #fff; border: 1px solid #e3eaf3;
      border-radius: 12px; box-shadow: 0 4px 18px rgba(24, 49, 85, .04); }
    .empty-icon, .stat-icon { display: grid; place-items: center; flex-shrink: 0; color: #3266bd; background: #edf3ff; }
    .empty-icon { width: 64px; height: 64px; margin-bottom: 20px; border-radius: 18px; }
    .empty-icon mat-icon { width: 32px; height: 32px; font-size: 32px; }
    .empty-card h2 { margin: 0 0 8px; color: #172b4d; font-size: 18px; line-height: 1.4; font-weight: 600; }
    .empty-card p { max-width: 420px; margin: 0; color: #697d95; font-size: 14px; line-height: 1.6; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; }
    .stat { box-sizing: border-box; min-width: 0; margin: 0; padding: 20px; border: 1px solid #e3eaf3;
      border-radius: 12px; box-shadow: 0 4px 18px rgba(24, 49, 85, .04); }
    .stat-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .stat-label { color: #697d95; font-size: 14px; font-weight: 500; }
    .stat-icon { width: 38px; height: 38px; border-radius: 10px; }
    .stat-icon mat-icon { width: 20px; height: 20px; font-size: 20px; }
    .stat-value { margin: 12px 0 18px; color: #172b4d; font-size: 30px; font-weight: 600; line-height: 1.2; }
    .stat-link { display: inline-flex; align-items: center; gap: 4px; color: #2459b5; font-size: 13px;
      font-weight: 500; text-decoration: none; }
    .stat-link:hover { text-decoration: underline; }
    .stat-link mat-icon { width: 16px; height: 16px; font-size: 16px; }
    mat-progress-bar { margin-bottom: 16px; }
    @media (max-width: 600px) {
      .dashboard { padding: 24px 16px; }
      .dashboard-header { margin-bottom: 20px; }
      .empty-card { padding: 40px 20px; }
      .stats-grid { grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; }
      .stat { padding: 16px; }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: Stat[] = [];
  loading = false;
  error: string | null = null;
  private sub?: Subscription;
  private run?: Subscription;

  constructor(public ctx: PropertyContext, private wo: WorkOrderApi, private assets: AssetApi, private notif: NotificationApi) {}

  ngOnInit(): void {
    this.sub = combineLatest([this.ctx.selectedId$, this.ctx.access$]).subscribe(() => this.load());
  }

  ngOnDestroy(): void { this.sub?.unsubscribe(); this.run?.unsubscribe(); }

  load(): void {
    const pid = this.ctx.selectedId;
    const access: AccessResponse | null = this.ctx.access;
    this.run?.unsubscribe();
    this.stats = [];
    if (!pid || !access || access.propertyId !== pid) {
      this.loading = false;
      this.error = null;
      return;
    }
    this.loading = true;
    this.error = null;
    const count = (f: Partial<WorkOrderFilter>) =>
      this.wo.list({ propertyId: pid, ...f }, { page: 0, size: 1 }, true).pipe(map(p => p.totalElements), catchError(() => of(null)));
    const calls: Record<string, Observable<number | null>> = {
      open: count({ status: 'OPEN' }), progress: count({ status: 'IN_PROGRESS' }), hold: count({ status: 'ON_HOLD' }),
      resolved: count({ status: 'RESOLVED' }), overdue: count({ overdue: true }),
      unread: this.notif.unreadCount().pipe(map(r => r.count), catchError(() => of(null)))
    };
    if (canUseAssets(access)) {
      calls['due'] = this.assets.tasksDue(pid, 7, true).pipe(map(t => t.length), catchError(() => of(null)));
      calls['overdueTasks'] = this.assets.tasks({ propertyId: pid, overdue: true }, { page: 0, size: 1 }, true)
        .pipe(map(p => p.totalElements), catchError(() => of(null)));
    }
    this.run = forkJoin(calls).subscribe((r: Record<string, number | null>) => {
      this.loading = false;
      const s: Stat[] = [
        { label: 'Open requests', icon: 'assignment', value: r['open'], link: '/work-orders', query: { status: 'OPEN' } },
        { label: 'In progress', icon: 'build', value: r['progress'], link: '/work-orders', query: { status: 'IN_PROGRESS' } },
        { label: 'On hold', icon: 'pause_circle', value: r['hold'], link: '/work-orders', query: { status: 'ON_HOLD' } },
        { label: 'Resolved (to close)', icon: 'check_circle', value: r['resolved'], link: '/work-orders', query: { status: 'RESOLVED' } },
        { label: 'Overdue work orders', icon: 'schedule', value: r['overdue'], link: '/work-orders', query: { overdue: 'true' } }
      ];
      if ('due' in r) {
        s.push({ label: 'Tasks due in 7 days', icon: 'event', value: r['due'], link: '/preventive-maintenance' });
        s.push({ label: 'Overdue tasks', icon: 'event_busy', value: r['overdueTasks'], link: '/preventive-maintenance', query: { overdue: 'true' } });
      }
      s.push({ label: 'Unread notifications', icon: 'notifications', value: r['unread'], link: '/notifications' });
      this.stats = s;
      if (s.every(x => x.value === null)) { this.error = 'Could not load dashboard data.'; }
    });
  }
}

@NgModule({
  declarations: [DashboardComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: DashboardComponent }])]
})
export class DashboardModule {}
