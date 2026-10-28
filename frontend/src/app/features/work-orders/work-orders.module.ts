import { Component, Inject, NgModule, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable, Subscription } from 'rxjs';
import { PropertyApi } from '../../core/api/property.api';
import { WorkOrderApi } from '../../core/api/work-order.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  CommentResponse, HistoryResponse, PRIORITIES, Page, PageQuery, Priority, UnitResponse, WORK_ORDER_STATUSES,
  WorkOrderResponse, WorkOrderStatus
} from '../../core/models';
import { applyFieldErrors, controlError, errorMessage } from '../../core/services/error-utils';
import { PagedList } from '../../core/services/paged-list';
import { PropertyContext, canManage, isTenant } from '../../core/services/property-context';
import { blankToNull, futureDateTime, localInputToIso } from '../../shared/form-utils';
import { UUID_RE } from '../../shared/components/user-id-field.component';
import { SharedModule } from '../../shared/shared.module';
import { actionLabel, allowedActions } from './work-order-actions';

// ---------- Transition dialog
@Component({
  selector: 'app-transition-dialog',
  template: `
    <h2 mat-dialog-title>{{ title }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <app-user-id-field *ngIf="data.target === 'ASSIGNED'" [control]="form.controls.assigneeId" label="Technician" role="TECHNICIAN"></app-user-id-field>
        <mat-form-field *ngIf="data.target === 'ASSIGNED'" appearance="outline" class="full-width">
          <mat-label>Due date (optional)</mat-label>
          <input matInput type="datetime-local" formControlName="dueAt"><mat-error>{{ err('dueAt') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ data.target === 'REJECTED' ? 'Reason (required)' : 'Note (optional)' }}</mat-label>
          <textarea matInput rows="3" maxlength="1000" formControlName="note"></textarea>
          <mat-error>{{ err('note') }}</mat-error>
        </mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">{{ title }}</button>
      </mat-dialog-actions>
    </form>`
})
export class TransitionDialogComponent {
  busy = false;
  error: string | null = null;
  title = actionLabel(this.data.wo.status, this.data.target);
  form = this.fb.nonNullable.group({
    note: ['', this.data.target === 'REJECTED' ? [Validators.required, Validators.maxLength(1000)] : [Validators.maxLength(1000)]],
    assigneeId: ['', this.data.target === 'ASSIGNED' ? [Validators.required, Validators.pattern(UUID_RE)] : []],
    dueAt: ['', [futureDateTime]]
  });

  constructor(private fb: FormBuilder, private api: WorkOrderApi, private ref: MatDialogRef<TransitionDialogComponent, WorkOrderResponse>,
              @Inject(MAT_DIALOG_DATA) public data: { wo: WorkOrderResponse; target: WorkOrderStatus }) {}

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    const assigning = this.data.target === 'ASSIGNED';
    this.api.transition(this.data.wo.id, {
      targetStatus: this.data.target,
      note: blankToNull(v.note),
      assigneeId: assigning ? v.assigneeId.trim() : null,
      dueAt: assigning ? localInputToIso(v.dueAt) : null
    }).subscribe({
      next: wo => this.ref.close(wo),
      error: e => {
        this.busy = false;
        if (e?.status === 409) { this.ref.close(null as unknown as undefined); return; }
        applyFieldErrors(this.form, e); this.error = errorMessage(e);
      }
    });
  }
}

// ---------- Due date dialog
@Component({
  selector: 'app-due-date-dialog',
  template: `
    <h2 mat-dialog-title>Change due date</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>New due date</mat-label>
          <input matInput type="datetime-local" formControlName="dueAt" cdkFocusInitial><mat-error>{{ err() }}</mat-error>
        </mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Save</button>
      </mat-dialog-actions>
    </form>`
})
export class DueDateDialogComponent {
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({ dueAt: ['', [Validators.required, futureDateTime]] });

  constructor(private fb: FormBuilder, private api: WorkOrderApi, private ref: MatDialogRef<DueDateDialogComponent, WorkOrderResponse>,
              @Inject(MAT_DIALOG_DATA) public data: { wo: WorkOrderResponse }) {}

  err(): string { return controlError(this.form.controls.dueAt); }

  submit(): void {
    this.form.markAllAsTouched();
    const iso = localInputToIso(this.form.controls.dueAt.value);
    if (this.form.invalid || !iso || this.busy) { return; }
    this.busy = true;
    this.api.setDueDate(this.data.wo.id, iso).subscribe({
      next: wo => this.ref.close(wo),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

// ---------- List
@Component({
  selector: 'app-work-orders',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Work orders</h1>
        <a mat-flat-button color="primary" routerLink="/work-orders/new" *ngIf="canCreate"><mat-icon>add</mat-icon> New request</a>
      </div>
      <app-empty-state *ngIf="!(ctx.selectedId$ | async)" icon="apartment" message="Select a property to see its work orders."></app-empty-state>
      <ng-container *ngIf="ctx.selectedId$ | async">
        <form [formGroup]="filters" class="filters" (ngSubmit)="resetAndReload()">
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Status</mat-label>
            <mat-select formControlName="status"><mat-option [value]="null">Any</mat-option>
              <mat-option *ngFor="let s of statuses" [value]="s">{{ s }}</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Priority</mat-label>
            <mat-select formControlName="priority"><mat-option [value]="null">Any</mat-option>
              <mat-option *ngFor="let p of priorities" [value]="p">{{ p }}</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" *ngIf="manage"><mat-label>Assignee ID</mat-label>
            <input matInput formControlName="assigneeId"><mat-error>Must be a user ID (UUID)</mat-error></mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Overdue</mat-label>
            <mat-select formControlName="overdue"><mat-option [value]="null">Any</mat-option>
              <mat-option [value]="true">Overdue</mat-option><mat-option [value]="false">Not overdue</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic"><mat-label>Due before</mat-label>
            <input matInput type="datetime-local" formControlName="dueBefore"></mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="filters.invalid">Apply</button>
          <button mat-button type="button" (click)="clear()">Clear</button>
        </form>
        <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
        <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
        <app-empty-state *ngIf="!loading && !error && !items.length" message="No work orders match."></app-empty-state>
        <table mat-table [dataSource]="items" matSort matSortActive="createdAt" matSortDirection="desc" (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
          <ng-container matColumnDef="title"><th mat-header-cell *matHeaderCellDef>Title</th>
            <td mat-cell *matCellDef="let w"><a [routerLink]="['/work-orders', w.id]">{{ w.title }}</a></td></ng-container>
          <ng-container matColumnDef="priority"><th mat-header-cell *matHeaderCellDef mat-sort-header="priority">Priority</th>
            <td mat-cell *matCellDef="let w"><app-badge [value]="w.priority"></app-badge></td></ng-container>
          <ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef mat-sort-header="status">Status</th>
            <td mat-cell *matCellDef="let w"><app-badge [value]="w.status"></app-badge><app-badge *ngIf="w.overdue" value="OVERDUE" text="Overdue"></app-badge></td></ng-container>
          <ng-container matColumnDef="dueAt"><th mat-header-cell *matHeaderCellDef mat-sort-header="dueAt">Due</th>
            <td mat-cell *matCellDef="let w">{{ w.dueAt ? (w.dueAt | date:'short') : '—' }}</td></ng-container>
          <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef mat-sort-header="createdAt">Created</th>
            <td mat-cell *matCellDef="let w">{{ w.createdAt | date:'short' }}</td></ng-container>
          <ng-container matColumnDef="updatedAt"><th mat-header-cell *matHeaderCellDef mat-sort-header="updatedAt">Updated</th>
            <td mat-cell *matCellDef="let w">{{ w.updatedAt | date:'short' }}</td></ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let r; columns: cols"></tr>
        </table>
        <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
      </ng-container>
    </div>`,
  styles: ['.filters { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 12px; }']
})
export class WorkOrdersComponent extends PagedList<WorkOrderResponse> implements OnInit {
  readonly cols = ['title', 'priority', 'status', 'dueAt', 'createdAt', 'updatedAt'];
  readonly statuses = WORK_ORDER_STATUSES;
  readonly priorities = PRIORITIES;
  private subs = new Subscription();
  filters = this.fb.group({
    status: [null as WorkOrderStatus | null],
    priority: [null as Priority | null],
    assigneeId: ['', [Validators.pattern(UUID_RE)]],
    overdue: [null as boolean | null],
    dueBefore: ['']
  });

  constructor(private api: WorkOrderApi, public ctx: PropertyContext, private fb: FormBuilder, private route: ActivatedRoute) { super(); }

  get manage(): boolean { return this.ctx.canManage; }
  get canCreate(): boolean { return canManage(this.ctx.access) || isTenant(this.ctx.access); }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    const status = q.get('status') as WorkOrderStatus | null;
    if (status && this.statuses.includes(status)) { this.filters.patchValue({ status }); }
    if (q.get('overdue') === 'true') { this.filters.patchValue({ overdue: true }); }
    this.subs.add(this.ctx.selectedId$.subscribe(id => { if (id) { this.resetAndReload(); } }));
  }

  override ngOnDestroy(): void { this.subs.unsubscribe(); super.ngOnDestroy(); }

  protected fetch(q: PageQuery): Observable<Page<WorkOrderResponse>> {
    const v = this.filters.getRawValue();
    return this.api.list({
      propertyId: this.ctx.selectedId!, status: v.status, priority: v.priority, assigneeId: blankToNull(v.assigneeId),
      overdue: v.overdue, dueBefore: localInputToIso(v.dueBefore)
    }, q);
  }

  clear(): void { this.filters.reset({ status: null, priority: null, assigneeId: '', overdue: null, dueBefore: '' }); this.resetAndReload(); }
}

// ---------- Create
@Component({
  selector: 'app-work-order-create',
  template: `
    <div class="page">
      <div class="page-header"><h1>New request</h1></div>
      <app-empty-state *ngIf="!ctx.selectedId" message="Select a property first."></app-empty-state>
      <mat-card *ngIf="ctx.selectedId">
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="full-width"><mat-label>Title</mat-label>
            <input matInput formControlName="title" maxlength="200"><mat-error>{{ err('title') }}</mat-error></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>Description</mat-label>
            <textarea matInput rows="5" formControlName="description" maxlength="4000"></textarea><mat-error>{{ err('description') }}</mat-error></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>Priority</mat-label>
            <mat-select formControlName="priority"><mat-option *ngFor="let p of priorities" [value]="p">{{ p }}</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>{{ tenant ? 'Unit' : 'Unit (optional)' }}</mat-label>
            <mat-select formControlName="unitId">
              <mat-option *ngIf="!tenant" [value]="''">None</mat-option>
              <mat-option *ngFor="let u of units" [value]="u.id">{{ u.unitNumber }}</mat-option>
            </mat-select><mat-error>{{ err('unitId') }}</mat-error></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>Due date (optional)</mat-label>
            <input matInput type="datetime-local" formControlName="dueAt"><mat-error>{{ err('dueAt') }}</mat-error></mat-form-field>
          <p class="error-text" *ngIf="error">{{ error }}</p>
          <button mat-flat-button color="primary" type="submit" [disabled]="busy">Submit request</button>
          <a mat-button routerLink="/work-orders">Cancel</a>
        </form>
      </mat-card>
    </div>`
})
export class WorkOrderCreateComponent implements OnInit {
  readonly priorities = PRIORITIES;
  units: UnitResponse[] = [];
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.maxLength(4000)]],
    priority: ['MEDIUM' as Priority, [Validators.required]],
    unitId: [''],
    dueAt: ['', [futureDateTime]]
  });

  constructor(private fb: FormBuilder, private api: WorkOrderApi, private props: PropertyApi, public ctx: PropertyContext, private router: Router) {}

  get tenant(): boolean { return isTenant(this.ctx.access) && !canManage(this.ctx.access); }

  ngOnInit(): void {
    this.ctx.access$.subscribe(a => {
      this.form.controls.unitId.setValidators(this.tenant ? [Validators.required] : []);
      this.form.controls.unitId.updateValueAndValidity();
      const pid = this.ctx.selectedId;
      if (!a || !pid) { return; }
      this.props.units(pid, { page: 0, size: 100, sort: { field: 'unitNumber', dir: 'asc' } }).subscribe(p => {
        this.units = this.tenant ? p.content.filter(u => a.unitIds.includes(u.id)) : p.content;
      });
    });
  }

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    const pid = this.ctx.selectedId;
    if (this.form.invalid || this.busy || !pid) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    this.api.create({
      propertyId: pid, unitId: v.unitId || null, title: v.title.trim(), description: v.description.trim(),
      priority: v.priority, dueAt: localInputToIso(v.dueAt)
    }).subscribe({
      next: wo => this.router.navigate(['/work-orders', wo.id]),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

// ---------- Detail
@Component({
  selector: 'app-work-order-detail',
  template: `
    <div class="page">
      <app-error-banner [message]="error" (retry)="load()"></app-error-banner>
      <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
      <ng-container *ngIf="wo as w">
        <div class="page-header">
          <h1>{{ w.title }}</h1>
          <div>
            <app-badge [value]="w.priority"></app-badge><app-badge [value]="w.status"></app-badge>
            <app-badge *ngIf="w.overdue" value="OVERDUE" text="Overdue"></app-badge>
          </div>
        </div>
        <mat-card>
          <p class="pre">{{ w.description }}</p>
          <p><strong>Requester:</strong> <app-user-name [id]="w.requesterId"></app-user-name></p>
          <p><strong>Assignee:</strong> <app-user-name *ngIf="w.assigneeId; else none" [id]="w.assigneeId"></app-user-name><ng-template #none>Unassigned</ng-template></p>
          <p><strong>Due:</strong> {{ w.dueAt ? (w.dueAt | date:'medium') : 'Not set' }}</p>
          <p class="muted">Created {{ w.createdAt | date:'medium' }} · Updated {{ w.updatedAt | date:'medium' }}</p>
          <div class="actions">
            <button *ngFor="let t of actions" mat-flat-button [color]="t === 'REJECTED' ? 'warn' : 'primary'" type="button" [disabled]="busy" (click)="act(t)">
              {{ label(t) }}
            </button>
            <button mat-stroked-button type="button" *ngIf="manage && !terminal" (click)="changeDue()">Change due date</button>
          </div>
        </mat-card>

        <h2>History</h2>
        <app-empty-state *ngIf="!history.length" message="No history."></app-empty-state>
        <ol class="timeline">
          <li *ngFor="let h of history">
            <app-badge *ngIf="h.fromStatus" [value]="h.fromStatus"></app-badge><span *ngIf="h.fromStatus">→</span>
            <app-badge [value]="h.toStatus"></app-badge>
            <span class="muted"> {{ h.createdAt | date:'medium' }} by <app-user-name [id]="h.actorId"></app-user-name></span>
            <div *ngIf="h.note" class="pre">{{ h.note }}</div>
          </li>
        </ol>

        <h2>Comments</h2>
        <form [formGroup]="commentForm" (ngSubmit)="comment()">
          <mat-form-field appearance="outline" class="full-width"><mat-label>Add a comment</mat-label>
            <textarea matInput rows="3" maxlength="2000" formControlName="body"></textarea><mat-error>{{ commentErr() }}</mat-error></mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="busy">Post comment</button>
        </form>
        <app-empty-state *ngIf="!comments.length" message="No comments yet."></app-empty-state>
        <mat-card *ngFor="let c of comments" class="comment">
          <div class="muted"><app-user-name [id]="c.authorId"></app-user-name> · {{ c.createdAt | date:'medium' }}</div>
          <div class="pre">{{ c.body }}</div>
        </mat-card>
        <mat-paginator [length]="commentTotal" [pageIndex]="commentPage" [pageSize]="20" (page)="commentPage = $event.pageIndex; loadComments()"></mat-paginator>
      </ng-container>
    </div>`,
  styles: ['.actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; } .pre { white-space: pre-wrap; } .timeline li { margin-bottom: 8px; } .comment { margin: 8px 0; }']
})
export class WorkOrderDetailComponent implements OnInit {
  id = this.route.snapshot.paramMap.get('id')!;
  wo: WorkOrderResponse | null = null;
  history: HistoryResponse[] = [];
  comments: CommentResponse[] = [];
  commentPage = 0;
  commentTotal = 0;
  loading = false;
  busy = false;
  error: string | null = null;
  commentForm = this.fb.nonNullable.group({ body: ['', [Validators.required, Validators.maxLength(2000)]] });

  constructor(private route: ActivatedRoute, private fb: FormBuilder, private api: WorkOrderApi, public ctx: PropertyContext,
              private auth: AuthService, private dialog: MatDialog) {}

  get manage(): boolean { return canManage(this.ctx.access); }
  get terminal(): boolean { return this.wo?.status === 'REJECTED' || this.wo?.status === 'CLOSED'; }
  get actions(): WorkOrderStatus[] {
    return this.wo ? allowedActions(this.wo, { userId: this.auth.userId, access: this.ctx.access }) : [];
  }

  ngOnInit(): void { this.load(); }

  label(t: WorkOrderStatus): string { return actionLabel(this.wo!.status, t); }
  commentErr(): string { return controlError(this.commentForm.controls.body); }

  load(): void {
    this.loading = true;
    this.error = null;
    this.api.get(this.id).subscribe({
      next: wo => {
        this.wo = wo;
        this.loading = false;
        this.ctx.ensure(wo.propertyId);
        this.loadHistory();
        this.loadComments();
      },
      error: e => {
        this.loading = false;
        this.error = e?.status === 404 ? 'Work order not found or not available to you.' : errorMessage(e);
      }
    });
  }

  loadHistory(): void { this.api.history(this.id).subscribe(h => this.history = h); }

  loadComments(): void {
    this.api.comments(this.id, { page: this.commentPage, size: 20, sort: { field: 'createdAt', dir: 'asc' } }).subscribe(p => {
      this.comments = p.content; this.commentTotal = p.totalElements;
    });
  }

  act(target: WorkOrderStatus): void {
    if (!this.wo) { return; }
    this.dialog.open(TransitionDialogComponent, { width: '460px', data: { wo: this.wo, target } }).afterClosed().subscribe(
      (r?: WorkOrderResponse | null) => {
        if (r) { this.wo = r; this.loadHistory(); } else if (r === null) { this.load(); } // 409: stale state, refresh
      }
    );
  }

  changeDue(): void {
    this.dialog.open(DueDateDialogComponent, { width: '420px', data: { wo: this.wo! } }).afterClosed().subscribe(
      (r?: WorkOrderResponse) => { if (r) { this.wo = r; } }
    );
  }

  comment(): void {
    this.commentForm.markAllAsTouched();
    if (this.commentForm.invalid || this.busy) { return; }
    this.busy = true;
    this.api.addComment(this.id, this.commentForm.controls.body.value.trim()).subscribe({
      next: () => { this.busy = false; this.commentForm.reset(); this.loadComments(); },
      error: e => { this.busy = false; applyFieldErrors(this.commentForm, e); }
    });
  }
}

@NgModule({
  declarations: [WorkOrdersComponent, WorkOrderCreateComponent, WorkOrderDetailComponent, TransitionDialogComponent, DueDateDialogComponent],
  imports: [SharedModule, RouterModule.forChild([
    { path: '', component: WorkOrdersComponent },
    { path: 'new', component: WorkOrderCreateComponent },
    { path: ':id', component: WorkOrderDetailComponent }
  ])]
})
export class WorkOrdersModule {}
