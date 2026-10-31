import { Component, Inject, NgModule, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { PropertyApi } from '../../core/api/property.api';
import { AuthService } from '../../core/auth/auth.service';
import {
  BuildingResponse, FloorResponse, MEMBERSHIP_ROLES, MembershipResponse, MembershipRole, Page, PageQuery,
  PropertyResponse, UnitResponse
} from '../../core/models';
import { applyFieldErrors, controlError, errorMessage } from '../../core/services/error-utils';
import { PagedList } from '../../core/services/paged-list';
import { PropertyContext, canManage } from '../../core/services/property-context';
import { blankToNull } from '../../shared/form-utils';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog.component';
import { UUID_RE } from '../../shared/components/user-id-field.component';
import { SharedModule } from '../../shared/shared.module';

// ---------- Property create/edit dialog
@Component({
  selector: 'app-property-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.property ? 'Edit property' : 'New property' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="150" cdkFocusInitial><mat-error>{{ err('name') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Address</mat-label>
          <input matInput formControlName="addressLine" maxlength="255"><mat-error>{{ err('addressLine') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>City</mat-label>
          <input matInput formControlName="city" maxlength="100"><mat-error>{{ err('city') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Postal code</mat-label>
          <input matInput formControlName="postalCode" maxlength="20"><mat-error>{{ err('postalCode') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Country (ISO code, e.g. US)</mat-label>
          <input matInput formControlName="country" maxlength="2"><mat-error>{{ err('country') }}</mat-error></mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Save</button>
      </mat-dialog-actions>
    </form>`
})
export class PropertyDialogComponent {
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    name: [this.data.property?.name ?? '', [Validators.required, Validators.maxLength(150)]],
    addressLine: [this.data.property?.addressLine ?? '', [Validators.required, Validators.maxLength(255)]],
    city: [this.data.property?.city ?? '', [Validators.required, Validators.maxLength(100)]],
    postalCode: [this.data.property?.postalCode ?? '', [Validators.required, Validators.maxLength(20)]],
    country: [this.data.property?.country ?? '', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]]
  });

  constructor(private fb: FormBuilder, private api: PropertyApi, private ref: MatDialogRef<PropertyDialogComponent, PropertyResponse>,
              @Inject(MAT_DIALOG_DATA) public data: { property?: PropertyResponse }) {}

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const req = this.form.getRawValue();
    const call = this.data.property ? this.api.update(this.data.property.id, req) : this.api.create(req);
    call.subscribe({
      next: p => this.ref.close(p),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

// ---------- List
@Component({
  selector: 'app-properties',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Properties</h1>
        <button mat-flat-button color="primary" type="button" *ngIf="auth.isAdmin" (click)="create()"><mat-icon>add</mat-icon> New property</button>
      </div>
      <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
      <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
      <app-empty-state *ngIf="!loading && !error && !items.length" icon="apartment" message="No properties available to you."></app-empty-state>
      <table mat-table [dataSource]="items" matSort (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="name">Name</th>
          <td mat-cell *matCellDef="let p"><a [routerLink]="['/properties', p.id]">{{ p.name }}</a></td>
        </ng-container>
        <ng-container matColumnDef="city">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="city">City</th>
          <td mat-cell *matCellDef="let p">{{ p.city }}, {{ p.country }}</td>
        </ng-container>
        <ng-container matColumnDef="active">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let p"><app-badge [value]="p.active ? 'ACTIVE' : 'DISABLED'" [text]="p.active ? 'Active' : 'Inactive'"></app-badge></td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="createdAt">Created</th>
          <td mat-cell *matCellDef="let p">{{ p.createdAt | date:'mediumDate' }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr>
        <tr mat-row *matRowDef="let r; columns: cols"></tr>
      </table>
      <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
    </div>`
})
export class PropertiesComponent extends PagedList<PropertyResponse> implements OnInit {
  readonly cols = ['name', 'city', 'active', 'createdAt'];
  constructor(private api: PropertyApi, public auth: AuthService, private dialog: MatDialog, private ctx: PropertyContext) { super(); }

  ngOnInit(): void { this.reload(); }
  protected fetch(q: PageQuery): Observable<Page<PropertyResponse>> { return this.api.list(q); }

  create(): void {
    this.dialog.open(PropertyDialogComponent, { width: '460px', data: {} }).afterClosed().subscribe(p => {
      if (p) { this.ctx.load().subscribe(); this.reload(); }
    });
  }
}

// ---------- Structure dialogs
@Component({
  selector: 'app-structure-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.kind === 'building' ? 'New building' : data.kind === 'floor' ? 'New floor' : 'New unit' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field *ngIf="data.kind === 'building'" appearance="outline" class="full-width"><mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="100" cdkFocusInitial><mat-error>{{ err('name') }}</mat-error></mat-form-field>
        <ng-container *ngIf="data.kind === 'floor'">
          <mat-form-field appearance="outline" class="full-width"><mat-label>Level number (-10 to 200)</mat-label>
            <input matInput type="number" formControlName="levelNumber" cdkFocusInitial><mat-error>{{ err('levelNumber') }}</mat-error></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>Label (optional)</mat-label>
            <input matInput formControlName="label" maxlength="50"><mat-error>{{ err('label') }}</mat-error></mat-form-field>
        </ng-container>
        <ng-container *ngIf="data.kind === 'unit'">
          <mat-form-field appearance="outline" class="full-width"><mat-label>Unit number</mat-label>
            <input matInput formControlName="unitNumber" maxlength="30" cdkFocusInitial><mat-error>{{ err('unitNumber') }}</mat-error></mat-form-field>
          <mat-form-field appearance="outline" class="full-width"><mat-label>Building (optional)</mat-label>
            <mat-select formControlName="buildingId" (selectionChange)="buildingChanged($event.value)">
              <mat-option [value]="''">None</mat-option>
              <mat-option *ngFor="let b of data.buildings" [value]="b.id">{{ b.name }}</mat-option>
            </mat-select></mat-form-field>
          <mat-form-field appearance="outline" class="full-width" *ngIf="floors.length"><mat-label>Floor (optional)</mat-label>
            <mat-select formControlName="floorId">
              <mat-option [value]="''">None</mat-option>
              <mat-option *ngFor="let f of floors" [value]="f.id">{{ f.label || 'Level ' + f.levelNumber }}</mat-option>
            </mat-select></mat-form-field>
        </ng-container>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Create</button>
      </mat-dialog-actions>
    </form>`
})
export class StructureDialogComponent {
  busy = false;
  error: string | null = null;
  floors: FloorResponse[] = [];
  form = this.fb.nonNullable.group({
    name: ['', this.data.kind === 'building' ? [Validators.required, Validators.maxLength(100)] : []],
    levelNumber: [0, this.data.kind === 'floor' ? [Validators.required, Validators.min(-10), Validators.max(200)] : []],
    label: ['', [Validators.maxLength(50)]],
    unitNumber: ['', this.data.kind === 'unit' ? [Validators.required, Validators.maxLength(30)] : []],
    buildingId: [''],
    floorId: ['']
  });

  constructor(private fb: FormBuilder, private api: PropertyApi, private ref: MatDialogRef<StructureDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) public data: { kind: 'building' | 'floor' | 'unit'; propertyId: string; buildingId?: string; buildings?: BuildingResponse[] }) {}

  err(n: string): string { return controlError(this.form.get(n)); }

  buildingChanged(id: string): void {
    this.floors = [];
    this.form.patchValue({ floorId: '' });
    if (id) { this.api.floors(this.data.propertyId, id).subscribe(f => this.floors = f); }
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    const pid = this.data.propertyId;
    const call: Observable<unknown> = this.data.kind === 'building' ? this.api.createBuilding(pid, v.name.trim())
      : this.data.kind === 'floor' ? this.api.createFloor(pid, this.data.buildingId!, { levelNumber: Number(v.levelNumber), label: blankToNull(v.label) })
      : this.api.createUnit(pid, { unitNumber: v.unitNumber.trim(), buildingId: v.buildingId || null, floorId: v.floorId || null });
    call.subscribe({
      next: () => this.ref.close(true),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

// ---------- Membership dialog
@Component({
  selector: 'app-membership-dialog',
  template: `
    <h2 mat-dialog-title>Add member</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <app-user-id-field [control]="form.controls.userId" label="User"></app-user-id-field>
        <mat-form-field appearance="outline" class="full-width"><mat-label>Role</mat-label>
          <mat-select formControlName="role">
            <mat-option *ngFor="let r of roles" [value]="r">{{ r }}</mat-option>
          </mat-select><mat-error>{{ err('role') }}</mat-error></mat-form-field>
        <mat-form-field appearance="outline" class="full-width" *ngIf="form.controls.role.value === 'TENANT'">
          <mat-label>Unit</mat-label>
          <mat-select formControlName="unitId">
            <mat-option *ngFor="let u of data.units" [value]="u.id">{{ u.unitNumber }}</mat-option>
          </mat-select><mat-error>{{ err('unitId') }}</mat-error></mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Add</button>
      </mat-dialog-actions>
    </form>`
})
export class MembershipDialogComponent {
  roles = MEMBERSHIP_ROLES;
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    userId: ['', [Validators.required, Validators.pattern(UUID_RE)]],
    role: ['TENANT' as MembershipRole, [Validators.required]],
    unitId: ['']
  });

  constructor(private fb: FormBuilder, private api: PropertyApi, private ref: MatDialogRef<MembershipDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) public data: { propertyId: string; units: UnitResponse[] }) {
    this.form.controls.role.valueChanges.subscribe(r => {
      const unit = this.form.controls.unitId;
      if (r === 'TENANT') { unit.addValidators(Validators.required); } else { unit.removeValidators(Validators.required); unit.setValue(''); }
      unit.updateValueAndValidity();
    });
    this.form.controls.unitId.addValidators(Validators.required);
  }

  err(n: string): string { return controlError(this.form.get(n)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const v = this.form.getRawValue();
    this.api.addMembership(this.data.propertyId, {
      userId: v.userId.trim(), role: v.role, unitId: v.role === 'TENANT' ? v.unitId : null
    }).subscribe({
      next: () => this.ref.close(true),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

// ---------- Detail
@Component({
  selector: 'app-property-detail',
  template: `
    <div class="page">
      <app-error-banner [message]="error" (retry)="load()"></app-error-banner>
      <div *ngIf="property as p">
        <div class="page-header">
          <h1>{{ p.name }}</h1>
          <button mat-stroked-button type="button" *ngIf="manage" (click)="edit()"><mat-icon>edit</mat-icon> Edit</button>
        </div>
        <mat-tab-group animationDuration="0ms">
          <mat-tab label="Overview">
            <mat-card class="tab-card">
              <p>{{ p.addressLine }}, {{ p.city }} {{ p.postalCode }}, {{ p.country }}</p>
              <p><app-badge [value]="p.active ? 'ACTIVE' : 'DISABLED'" [text]="p.active ? 'Active' : 'Inactive'"></app-badge></p>
              <p class="muted" *ngIf="access">Your access: {{ access.systemAdmin ? 'System administrator' : (access.roles.join(', ')) }}</p>
              <p class="muted">Updated {{ p.updatedAt | date:'medium' }}</p>
            </mat-card>
          </mat-tab>
          <mat-tab label="Structure">
            <div class="tab-card">
              <div class="row" *ngIf="manage">
                <button mat-stroked-button type="button" (click)="add('building')">Add building</button>
                <button mat-stroked-button type="button" (click)="add('unit')">Add unit</button>
              </div>
              <ng-container *ngIf="manage">
                <h3>Buildings</h3>
                <app-empty-state *ngIf="!buildings.length" message="No buildings yet."></app-empty-state>
                <mat-card *ngFor="let b of buildings" class="building">
                  <div class="row"><strong>{{ b.name }}</strong>
                    <span class="spacer"></span>
                    <button mat-button type="button" (click)="toggleFloors(b)">{{ floors[b.id] ? 'Hide floors' : 'Floors' }}</button>
                    <button mat-button type="button" (click)="add('floor', b)">Add floor</button>
                  </div>
                  <ul *ngIf="floors[b.id]">
                    <li *ngIf="!floors[b.id]?.length" class="muted">No floors.</li>
                    <li *ngFor="let f of floors[b.id]">Level {{ f.levelNumber }}<span *ngIf="f.label"> — {{ f.label }}</span></li>
                  </ul>
                </mat-card>
              </ng-container>
              <h3>Units</h3>
              <app-empty-state *ngIf="!units.length" message="No units available."></app-empty-state>
              <mat-list *ngIf="units.length">
                <mat-list-item *ngFor="let u of units">Unit {{ u.unitNumber }}</mat-list-item>
              </mat-list>
              <mat-paginator [length]="unitTotal" [pageIndex]="unitPage" [pageSize]="20" (page)="unitPage = $event.pageIndex; loadUnits()"></mat-paginator>
            </div>
          </mat-tab>
          <mat-tab label="Members" *ngIf="manage">
            <div class="tab-card">
              <div class="row"><button mat-flat-button color="primary" type="button" (click)="addMember()">Add member</button></div>
              <app-empty-state *ngIf="!members.length" message="No members."></app-empty-state>
              <table mat-table [dataSource]="members" class="full-width" *ngIf="members.length">
                <ng-container matColumnDef="user"><th mat-header-cell *matHeaderCellDef>User</th>
                  <td mat-cell *matCellDef="let m"><app-user-name [id]="m.userId"></app-user-name></td></ng-container>
                <ng-container matColumnDef="role"><th mat-header-cell *matHeaderCellDef>Role</th>
                  <td mat-cell *matCellDef="let m"><app-badge [value]="m.role"></app-badge></td></ng-container>
                <ng-container matColumnDef="unit"><th mat-header-cell *matHeaderCellDef>Unit</th>
                  <td mat-cell *matCellDef="let m">{{ unitName(m.unitId) }}</td></ng-container>
                <ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let m"><button mat-button color="warn" type="button" (click)="remove(m)">Remove</button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="memberCols"></tr>
                <tr mat-row *matRowDef="let r; columns: memberCols"></tr>
              </table>
              <mat-paginator [length]="memberTotal" [pageIndex]="memberPage" [pageSize]="20" (page)="memberPage = $event.pageIndex; loadMembers()"></mat-paginator>
            </div>
          </mat-tab>
        </mat-tab-group>
      </div>
    </div>`,
  styles: ['.tab-card { margin: 16px 0; display: block; } .row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; } .spacer { flex: 1; } .building { margin-bottom: 8px; padding: 8px 16px; }']
})
export class PropertyDetailComponent implements OnInit {
  id = this.route.snapshot.paramMap.get('id')!;
  property: PropertyResponse | null = null;
  error: string | null = null;
  buildings: BuildingResponse[] = [];
  floors: Record<string, FloorResponse[] | undefined> = {};
  units: UnitResponse[] = [];
  unitPage = 0;
  unitTotal = 0;
  allUnits: UnitResponse[] = [];
  members: MembershipResponse[] = [];
  memberPage = 0;
  memberTotal = 0;
  readonly memberCols = ['user', 'role', 'unit', 'actions'];

  constructor(private route: ActivatedRoute, private api: PropertyApi, private ctx: PropertyContext,
              private dialog: MatDialog) {}

  get access() { return this.ctx.access?.propertyId === this.id ? this.ctx.access : null; }
  get manage(): boolean { return canManage(this.access); }

  ngOnInit(): void {
    this.ctx.ensure(this.id);
    this.ctx.access$.subscribe(a => {
      if (a?.propertyId === this.id && this.manage) { this.loadBuildings(); this.loadMembers(); }
    });
    this.load();
  }

  load(): void {
    this.error = null;
    this.api.get(this.id).subscribe({
      next: p => { this.property = p; this.loadUnits(); },
      error: e => this.error = e?.status === 404 ? 'Property not found or not available to you.' : errorMessage(e)
    });
  }

  loadBuildings(): void {
    this.api.buildings(this.id, { page: 0, size: 100, sort: { field: 'name', dir: 'asc' } }).subscribe(p => this.buildings = p.content);
  }

  loadUnits(): void {
    this.api.units(this.id, { page: this.unitPage, size: 20, sort: { field: 'unitNumber', dir: 'asc' } }).subscribe(p => {
      this.units = p.content; this.unitTotal = p.totalElements;
    });
    this.api.units(this.id, { page: 0, size: 100, sort: { field: 'unitNumber', dir: 'asc' } }).subscribe(p => this.allUnits = p.content);
  }

  loadMembers(): void {
    this.api.memberships(this.id, { page: this.memberPage, size: 20, sort: { field: 'createdAt', dir: 'desc' } }).subscribe(p => {
      this.members = p.content; this.memberTotal = p.totalElements;
    });
  }

  unitName(id: string | null): string { return id ? (this.allUnits.find(u => u.id === id)?.unitNumber ?? id.substring(0, 8)) : '—'; }

  toggleFloors(b: BuildingResponse): void {
    if (this.floors[b.id]) { this.floors[b.id] = undefined; return; }
    this.api.floors(this.id, b.id).subscribe(f => this.floors[b.id] = f);
  }

  edit(): void {
    this.dialog.open(PropertyDialogComponent, { width: '460px', data: { property: this.property! } }).afterClosed().subscribe(p => {
      if (p) { this.property = p; this.ctx.load().subscribe(); }
    });
  }

  add(kind: 'building' | 'floor' | 'unit', b?: BuildingResponse): void {
    this.dialog.open(StructureDialogComponent, { width: '440px', data: { kind, propertyId: this.id, buildingId: b?.id, buildings: this.buildings } })
      .afterClosed().subscribe(ok => {
        if (!ok) { return; }
        if (kind === 'building') { this.loadBuildings(); }
        if (kind === 'floor' && b) { this.floors[b.id] = undefined; this.toggleFloors(b); }
        if (kind === 'unit') { this.loadUnits(); }
      });
  }

  addMember(): void {
    this.dialog.open(MembershipDialogComponent, { width: '460px', data: { propertyId: this.id, units: this.allUnits } })
      .afterClosed().subscribe(ok => { if (ok) { this.loadMembers(); } });
  }

  remove(m: MembershipResponse): void {
    this.dialog.open(ConfirmDialogComponent, { data: { title: 'Remove member?', message: 'The user loses access to this property.', confirmText: 'Remove', warn: true } })
      .afterClosed().subscribe(r => {
        if (r) { this.api.removeMembership(this.id, m.id).subscribe(() => this.loadMembers()); }
      });
  }
}

@NgModule({
  declarations: [PropertiesComponent, PropertyDetailComponent, PropertyDialogComponent, StructureDialogComponent, MembershipDialogComponent],
  imports: [SharedModule, RouterModule.forChild([
    { path: '', component: PropertiesComponent },
    { path: ':id', component: PropertyDetailComponent }
  ])]
})
export class PropertiesModule {}
