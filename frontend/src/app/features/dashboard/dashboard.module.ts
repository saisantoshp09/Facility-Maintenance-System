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

interface Stat { label: string; value: number | null; link?: string; query?: Record<string, string>; }

@Component({
  selector: 'app-dashboard',
  template: `
    <div class="page">
      <div class="page-header"><h1>Dashboard</h1></div>
      <app-empty-state *ngIf="ctx.loaded$.value && !(ctx.properties$ | async)?.length" icon="apartment"
        message="You are not a member of any property yet. Ask an administrator to add you."></app-empty-state>
      <ng-container *ngIf="(ctx.selectedId$ | async)">
        <app-error-banner [message]="error" (retry)="load()"></app-error-banner>
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <div class="cards">
          <mat-card *ngFor="let s of stats" class="stat">
            <mat-card-subtitle>{{ s.label }}</mat-card-subtitle>
            <mat-card-title>{{ s.value === null ? '—' : s.value }}</mat-card-title>
            <mat-card-actions *ngIf="s.link"><a mat-button [routerLink]="s.link" [queryParams]="s.query">View</a></mat-card-actions>
          </mat-card>
        </div>
      </ng-container>
    </div>`,
  styles: ['.stat { min-width: 180px; }']
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
    if (!pid || !access) { return; }
    this.run?.unsubscribe();
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
        { label: 'Open requests', value: r['open'], link: '/work-orders', query: { status: 'OPEN' } },
        { label: 'In progress', value: r['progress'], link: '/work-orders', query: { status: 'IN_PROGRESS' } },
        { label: 'On hold', value: r['hold'], link: '/work-orders', query: { status: 'ON_HOLD' } },
        { label: 'Resolved (to close)', value: r['resolved'], link: '/work-orders', query: { status: 'RESOLVED' } },
        { label: 'Overdue work orders', value: r['overdue'], link: '/work-orders', query: { overdue: 'true' } }
      ];
      if ('due' in r) {
        s.push({ label: 'Tasks due in 7 days', value: r['due'], link: '/preventive-maintenance' });
        s.push({ label: 'Overdue tasks', value: r['overdueTasks'], link: '/preventive-maintenance', query: { overdue: 'true' } });
      }
      s.push({ label: 'Unread notifications', value: r['unread'], link: '/notifications' });
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
