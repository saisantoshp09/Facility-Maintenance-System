import { Component, Inject, NgModule, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subscription } from 'rxjs';
import { AssetApi } from '../../core/api/asset.api';
import {
  ASSET_STATUSES, AssetStatus, AssetView, Page, PageQuery, ScheduleView, ServiceRecordView
} from '../../core/models';
import { applyFieldErrors, controlError, errorMessage } from '../../core/services/error-utils';
import { PagedList } from '../../core/services/paged-list';
import { PropertyContext, canManage, canUseAssets } from '../../core/services/property-context';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';
import { ServiceRecordDialogComponent } from '../../shared/components/service-record-dialog.component';
import { blankToNull, todayDate } from '../../shared/form-utils';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-asset-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.asset ? 'Edit asset' : 'New asset' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="160" cdkFocusInitial><mat-error>{{ err('name') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Category</mat-label>
          <input matInput formControlName="category" maxlength="80"><mat-error>{{ err('category') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Serial number (optional)</mat-label>
          <input matInput formControlName="serialNumber" maxlength="120"><mat-error>{{ err('serialNumber') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Location</mat-label>
          <input matInput formControlName="location" maxlength="200"><mat-error>{{ err('location') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Installed on (optional)</mat-label>
          <input matInput type="date" formControlName="installedOn"><mat-error>{{ err('installedOn') }}</mat-error></mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Save</button>
      </mat-dialog-actions>
    </form>`
})
export class AssetDialogComponent {
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    name: [this.data.asset?.name ?? '', [Validators.required, Validators.maxLength(160)]],
    category: [this.data.asset?.category ?? '', [Validators.required, Validators.maxLength(80)]],
    serialNumber: [this.data.asset?.serialNumber ?? '', [Validators.maxLength(120)]],
    location: [this.data.asset?.location ?? '', [Validators.required, Validators.maxLength(200)]],
    installedOn: [this.data.asset?.installedOn ?? '']
  });

  constructor(private fb: FormBuilder, private api: AssetApi, private ref: MatDialogRef<AssetDialogComponent, AssetView>,
              @Inject(MAT_DIALOG_DATA) public data: { propertyId: string; asset?: AssetView }) {}

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    const body = {
      unitId: this.data.asset?.unitId ?? null, name: v.name.trim(), category: v.category.trim(),
      serialNumber: blankToNull(v.serialNumber), location: v.location.trim(), installedOn: v.installedOn || null
    };
    const call = this.data.asset ? this.api.update(this.data.asset.id, body) : this.api.create({ propertyId: this.data.propertyId, ...body });
    call.subscribe({
      next: a => this.ref.close(a),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

@Component({
  selector: 'app-schedule-dialog',
  template: `
    <h2 mat-dialog-title>New preventive schedule</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Title</mat-label>
          <input matInput formControlName="title" maxlength="160" cdkFocusInitial><mat-error>{{ err('title') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Description</mat-label>
          <textarea matInput rows="3" formControlName="description" maxlength="1000"></textarea><mat-error>{{ err('description') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Repeat every (days)</mat-label>
          <input matInput type="number" min="1" formControlName="intervalDays"><mat-error>{{ err('intervalDays') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>First due date</mat-label>
          <input matInput type="date" formControlName="startDate"><mat-error>{{ err('startDate') }}</mat-error></mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Create</button>
      </mat-dialog-actions>
    </form>`
})
export class ScheduleDialogComponent {
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    intervalDays: [30, [Validators.required, Validators.min(1)]],
    startDate: [todayDate(), [Validators.required]]
  });

  constructor(private fb: FormBuilder, private api: AssetApi, private ref: MatDialogRef<ScheduleDialogComponent, ScheduleView>,
              @Inject(MAT_DIALOG_DATA) public data: { assetId: string }) {}

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    this.api.createSchedule(this.data.assetId, { ...v, title: v.title.trim(), description: v.description.trim(), intervalDays: Number(v.intervalDays) }).subscribe({
      next: s => this.ref.close(s),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

@Component({
  selector: 'app-assets',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Assets</h1>
        <button mat-flat-button color="primary" type="button" *ngIf="manage" (click)="create()"><mat-icon>add</mat-icon> New asset</button>
      </div>
      <app-empty-state *ngIf="!(ctx.selectedId$ | async)" icon="apartment" message="Select a property to see its assets."></app-empty-state>
      <ng-container *ngIf="ctx.selectedId$ | async">
        <div class="filters">
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Status</mat-label>
            <mat-select [value]="status" (selectionChange)="status = $event.value; resetAndReload()">
              <mat-option [value]="null">Any</mat-option><mat-option *ngFor="let s of statuses" [value]="s">{{ s }}</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Category</mat-label>
            <input matInput [value]="category" (keyup.enter)="category = $any($event.target).value; resetAndReload()" (blur)="category = $any($event.target).value; resetAndReload()"></mat-form-field>
        </div>
        <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <app-empty-state *ngIf="!loading && !error && !items.length" icon="build" message="No assets."></app-empty-state>
        <table mat-table [dataSource]="items" matSort matSortActive="createdAt" matSortDirection="desc" (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
          <ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef mat-sort-header="name">Name</th>
            <td mat-cell *matCellDef="let a"><a [routerLink]="['/assets', a.id]">{{ a.name }}</a></td></ng-container>
          <ng-container matColumnDef="category"><th mat-header-cell *matHeaderCellDef mat-sort-header="category">Category</th>
            <td mat-cell *matCellDef="let a">{{ a.category }}</td></ng-container>
          <ng-container matColumnDef="location"><th mat-header-cell *matHeaderCellDef>Location</th>
            <td mat-cell *matCellDef="let a">{{ a.location }}</td></ng-container>
          <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef mat-sort-header="status">Status</th>
            <td mat-cell *matCellDef="let a"><app-badge [value]="a.status"></app-badge></td></ng-container>
          <ng-container matColumnDef="updatedAt"><th mat-header-cell *matHeaderCellDef mat-sort-header="updatedAt">Updated</th>
            <td mat-cell *matCellDef="let a">{{ a.updatedAt | date:'short' }}</td></ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let r; columns: cols"></tr>
        </table>
        <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
      </ng-container>
    </div>`,
  styles: ['.filters { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }']
})
export class AssetsComponent extends PagedList<AssetView> implements OnInit {
  readonly cols = ['name', 'category', 'location', 'status', 'updatedAt'];
  readonly statuses = ASSET_STATUSES;
  status: AssetStatus | null = null;
  category = '';
  private selSub?: Subscription;

  constructor(public ctx: PropertyContext, private api: AssetApi, private dialog: MatDialog) { super(); }

  get manage(): boolean { return this.ctx.canManage; }

  ngOnInit(): void {
    this.selSub = this.ctx.selectedId$.subscribe(id => { if (id) { this.resetAndReload(); } });
  }

  override ngOnDestroy(): void { this.selSub?.unsubscribe(); super.ngOnDestroy(); }

  protected fetch(q: PageQuery): Observable<Page<AssetView>> {
    return this.api.list({ propertyId: this.ctx.selectedId!, status: this.status, category: blankToNull(this.category) }, q);
  }

  create(): void {
    this.dialog.open(AssetDialogComponent, { width: '460px', data: { propertyId: this.ctx.selectedId! } })
      .afterClosed().subscribe(a => { if (a) { this.reload(); } });
  }
}

@Component({
  selector: 'app-asset-detail',
  template: `
    <div class="page">
      <app-error-banner [message]="error" (retry)="load()"></app-error-banner>
      <ng-container *ngIf="asset as a">
        <div class="page-header">
          <h1>{{ a.name }} <app-badge [value]="a.status"></app-badge></h1>
          <div>
            <button mat-stroked-button type="button" *ngIf="manage && a.status !== 'RETIRED'" (click)="edit()">Edit</button>
            <button mat-stroked-button color="warn" type="button" *ngIf="manage && a.status !== 'RETIRED'" (click)="retire()">Retire</button>
            <button mat-flat-button color="primary" type="button" *ngIf="a.status !== 'RETIRED'" (click)="record()">Record service</button>
          </div>
        </div>
        <mat-card>
          <p><strong>Category:</strong> {{ a.category }}</p>
          <p><strong>Location:</strong> {{ a.location }}</p>
          <p><strong>Serial number:</strong> {{ a.serialNumber || '—' }}</p>
          <p><strong>Installed on:</strong> {{ a.installedOn ? (a.installedOn | date:'mediumDate') : '—' }}</p>
        </mat-card>

        <div class="page-header"><h2>Preventive schedules</h2>
          <button mat-stroked-button type="button" *ngIf="manage && a.status !== 'RETIRED'" (click)="addSchedule()">Add schedule</button></div>
        <app-empty-state *ngIf="!schedules.length" message="No schedules."></app-empty-state>
        <mat-card *ngFor="let s of schedules" class="sched">
          <strong>{{ s.title }}</strong> <app-badge [value]="s.active ? 'ACTIVE' : 'CANCELLED'" [text]="s.active ? 'Active' : 'Inactive'"></app-badge>
          <p>{{ s.description }}</p>
          <p class="muted">Every {{ s.intervalDays }} days · next due {{ s.nextDueDate ? (s.nextDueDate | date:'mediumDate') : '—' }}
            · last completed {{ s.lastCompletedDate ? (s.lastCompletedDate | date:'mediumDate') : '—' }}</p>
          <button mat-button color="warn" type="button" *ngIf="manage && s.active" (click)="deactivate(s)">Deactivate</button>
        </mat-card>

        <h2>Service records</h2>
        <app-empty-state *ngIf="!records.length" message="No service records."></app-empty-state>
        <table mat-table [dataSource]="records" matSort matSortActive="performedOn" matSortDirection="desc" (matSortChange)="recSort($event)" *ngIf="records.length" class="full-width">
          <ng-container matColumnDef="performedOn"><th mat-header-cell *matHeaderCellDef mat-sort-header="performedOn">Date</th>
            <td mat-cell *matCellDef="let r">{{ r.performedOn | date:'mediumDate' }}</td></ng-container>
          <ng-container matColumnDef="summary"><th mat-header-cell *matHeaderCellDef>Summary</th>
            <td mat-cell *matCellDef="let r">{{ r.summary }}</td></ng-container>
          <ng-container matColumnDef="by"><th mat-header-cell *matHeaderCellDef>By</th>
            <td mat-cell *matCellDef="let r"><app-user-name [id]="r.performedBy"></app-user-name></td></ng-container>
          <ng-container matColumnDef="cost"><th mat-header-cell *matHeaderCellDef>Cost</th>
            <td mat-cell *matCellDef="let r">{{ r.cost === null ? '—' : (r.cost | number:'1.2-2') }}</td></ng-container>
          <tr mat-header-row *matHeaderRowDef="recCols"></tr>
          <tr mat-row *matRowDef="let r; columns: recCols"></tr>
        </table>
        <mat-paginator [length]="recTotal" [pageIndex]="recPage" [pageSize]="20" (page)="recPage = $event.pageIndex; loadRecords()"></mat-paginator>
      </ng-container>
    </div>`,
  styles: ['.sched { margin: 8px 0; }']
})
export class AssetDetailComponent implements OnInit {
  id = this.route.snapshot.paramMap.get('id')!;
  asset: AssetView | null = null;
  schedules: ScheduleView[] = [];
  records: ServiceRecordView[] = [];
  recPage = 0;
  recTotal = 0;
  recSortSpec: { field: string; dir: 'asc' | 'desc' } = { field: 'performedOn', dir: 'desc' };
  error: string | null = null;
  readonly recCols = ['performedOn', 'summary', 'by', 'cost'];

  constructor(private route: ActivatedRoute, private api: AssetApi, public ctx: PropertyContext, private dialog: MatDialog) {}

  get manage(): boolean { return canManage(this.ctx.access); }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.error = null;
    this.api.get(this.id).subscribe({
      next: a => { this.asset = a; this.ctx.ensure(a.propertyId); this.loadSchedules(); this.loadRecords(); },
      error: e => this.error = e?.status === 403 ? 'You do not have access to assets.' : e?.status === 404 ? 'Asset not found or not available to you.' : errorMessage(e)
    });
  }

  loadSchedules(): void { this.api.schedules(this.id).subscribe(s => this.schedules = s); }

  loadRecords(): void {
    this.api.serviceRecords(this.id, { page: this.recPage, size: 20, sort: this.recSortSpec }).subscribe(p => {
      this.records = p.content; this.recTotal = p.totalElements;
    });
  }

  recSort(s: { active: string; direction: string }): void {
    this.recSortSpec = s.direction ? { field: s.active, dir: s.direction as 'asc' | 'desc' } : { field: 'performedOn', dir: 'desc' };
    this.recPage = 0;
    this.loadRecords();
  }

  edit(): void {
    this.dialog.open(AssetDialogComponent, { width: '460px', data: { propertyId: this.asset!.propertyId, asset: this.asset! } })
      .afterClosed().subscribe((a?: AssetView) => { if (a) { this.asset = a; } });
  }

  retire(): void {
    this.dialog.open(ConfirmDialogComponent, { data: { title: 'Retire asset?', message: 'Retired assets cannot get schedules or service records.', confirmText: 'Retire', warn: true } })
      .afterClosed().subscribe(r => { if (r) { this.api.retire(this.id).subscribe(a => this.asset = a); } });
  }

  addSchedule(): void {
    this.dialog.open(ScheduleDialogComponent, { width: '440px', data: { assetId: this.id } })
      .afterClosed().subscribe(s => { if (s) { this.loadSchedules(); } });
  }

  deactivate(s: ScheduleView): void {
    this.dialog.open(ConfirmDialogComponent, { data: { title: 'Deactivate schedule?', confirmText: 'Deactivate', warn: true } })
      .afterClosed().subscribe(r => { if (r) { this.api.deactivateSchedule(s.id).subscribe(() => this.loadSchedules()); } });
  }

  record(): void {
    this.dialog.open(ServiceRecordDialogComponent, { width: '460px', data: { assetId: this.id } })
      .afterClosed().subscribe(r => { if (r) { this.loadRecords(); this.loadSchedules(); } });
  }
}

@NgModule({
  declarations: [AssetsComponent, AssetDetailComponent, AssetDialogComponent, ScheduleDialogComponent],
  imports: [SharedModule, RouterModule.forChild([
    { path: '', component: AssetsComponent },
    { path: ':id', component: AssetDetailComponent }
  ])]
})
export class AssetsModule {}
