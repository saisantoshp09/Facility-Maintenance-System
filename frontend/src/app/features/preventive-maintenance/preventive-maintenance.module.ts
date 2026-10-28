import { Component, NgModule, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Observable, Subscription } from 'rxjs';
import { AssetApi } from '../../core/api/asset.api';
import { Page, PageQuery, TASK_STATUSES, TaskStatus, TaskView } from '../../core/models';
import { PagedList } from '../../core/services/paged-list';
import { PropertyContext, canUseAssets } from '../../core/services/property-context';
import { ServiceRecordDialogComponent } from '../../shared/components/service-record-dialog.component';
import { todayDate } from '../../shared/form-utils';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-tasks',
  template: `
    <div class="page">
      <div class="page-header"><h1>Preventive maintenance</h1></div>
      <app-empty-state *ngIf="!(ctx.selectedId$ | async)" icon="apartment" message="Select a property to see its tasks."></app-empty-state>
      <app-empty-state *ngIf="(ctx.selectedId$ | async) && ctx.access && !canUse" icon="lock" message="Preventive maintenance is not available for your role."></app-empty-state>
      <ng-container *ngIf="(ctx.selectedId$ | async) && canUse">
        <mat-card class="due" *ngIf="due.length">
          <mat-card-title>Due in the next 7 days</mat-card-title>
          <mat-list><mat-list-item *ngFor="let t of due">
            <a [routerLink]="['/assets', t.assetId]">Asset {{ t.assetId.substring(0, 8) }}</a>&nbsp;due {{ t.dueDate | date:'mediumDate' }}
          </mat-list-item></mat-list>
        </mat-card>
        <div class="filters">
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Status</mat-label>
            <mat-select [value]="status" (selectionChange)="status = $event.value; resetAndReload()">
              <mat-option [value]="null">Any</mat-option><mat-option *ngFor="let s of statuses" [value]="s">{{ s }}</mat-option></mat-select></mat-form-field>
          <mat-slide-toggle [checked]="overdue" (change)="overdue = $event.checked; resetAndReload()">Overdue only</mat-slide-toggle>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Due before</mat-label>
            <input matInput type="date" [value]="dueBefore" (change)="dueBefore = $any($event.target).value; resetAndReload()"></mat-form-field>
        </div>
        <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <app-empty-state *ngIf="!loading && !error && !items.length" message="No tasks."></app-empty-state>
        <table mat-table [dataSource]="items" matSort matSortActive="dueDate" matSortDirection="asc" (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
          <ng-container matColumnDef="dueDate"><th mat-header-cell *matHeaderCellDef mat-sort-header="dueDate">Due</th>
            <td mat-cell *matCellDef="let t">{{ t.dueDate | date:'mediumDate' }}<app-badge *ngIf="isOverdue(t)" value="OVERDUE" text="Overdue"></app-badge></td></ng-container>
          <ng-container matColumnDef="asset"><th mat-header-cell *matHeaderCellDef>Asset</th>
            <td mat-cell *matCellDef="let t"><a [routerLink]="['/assets', t.assetId]">{{ t.assetId.substring(0, 8) }}</a></td></ng-container>
          <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let t"><app-badge [value]="t.status"></app-badge></td></ng-container>
          <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let t"><button mat-button type="button" *ngIf="t.status === 'PENDING'" (click)="record(t)">Record service</button></td></ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let r; columns: cols"></tr>
        </table>
        <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
      </ng-container>
    </div>`,
  styles: ['.filters { display: flex; gap: 12px; align-items: center; margin: 12px 0; flex-wrap: wrap; } .due { margin-bottom: 12px; }']
})
export class TasksComponent extends PagedList<TaskView> implements OnInit, OnDestroy {
  readonly cols = ['dueDate', 'asset', 'status', 'actions'];
  readonly statuses = TASK_STATUSES;
  status: TaskStatus | null = null;
  overdue = false;
  dueBefore = '';
  due: TaskView[] = [];
  private selSub?: Subscription;

  constructor(public ctx: PropertyContext, private api: AssetApi, private dialog: MatDialog, private route: ActivatedRoute) { super(); }

  get canUse(): boolean { return canUseAssets(this.ctx.access); }

  ngOnInit(): void {
    this.overdue = this.route.snapshot.queryParamMap.get('overdue') === 'true';
    this.selSub = this.ctx.access$.subscribe(a => {
      if (a && canUseAssets(a)) {
        this.resetAndReload();
        this.api.tasksDue(a.propertyId, 7).subscribe(t => this.due = t);
      }
    });
  }

  override ngOnDestroy(): void { this.selSub?.unsubscribe(); super.ngOnDestroy(); }

  protected fetch(q: PageQuery): Observable<Page<TaskView>> {
    return this.api.tasks({ propertyId: this.ctx.selectedId!, status: this.status, overdue: this.overdue ? true : null, dueBefore: this.dueBefore || null }, q);
  }

  isOverdue(t: TaskView): boolean { return t.status === 'PENDING' && t.dueDate < todayDate(); }

  record(t: TaskView): void {
    this.dialog.open(ServiceRecordDialogComponent, { width: '460px', data: { assetId: t.assetId, taskId: t.id } })
      .afterClosed().subscribe(r => { if (r) { this.reload(); this.api.tasksDue(this.ctx.selectedId!, 7).subscribe(d => this.due = d); } });
  }
}

@NgModule({
  declarations: [TasksComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: TasksComponent }])]
})
export class PreventiveMaintenanceModule {}
