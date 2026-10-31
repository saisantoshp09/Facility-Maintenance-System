import { Component, Inject, NgModule, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { IdentityApi } from '../../core/api/identity.api';
import { AuthService } from '../../core/auth/auth.service';
import { ALL_ROLES, Page, PageQuery, Role, UserResponse } from '../../core/models';
import { applyFieldErrors, controlError, errorMessage } from '../../core/services/error-utils';
import { PagedList } from '../../core/services/paged-list';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-user-create-dialog',
  template: `
    <h2 mat-dialog-title>New user</h2>
    <form [formGroup]="form" (ngSubmit)="submit()" autocomplete="off">
      <mat-dialog-content>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" type="email" maxlength="254" cdkFocusInitial>
          <mat-error>{{ err('email') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Display name</mat-label>
          <input matInput formControlName="displayName" maxlength="120">
          <mat-error>{{ err('displayName') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Initial password</mat-label>
          <input matInput formControlName="password" type="password" autocomplete="new-password">
          <mat-hint>12–72 characters</mat-hint>
          <mat-error>{{ err('password') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Roles</mat-label>
          <mat-select formControlName="roles" multiple>
            <mat-option *ngFor="let r of roles" [value]="r">{{ r }}</mat-option>
          </mat-select>
          <mat-error>{{ err('roles') }}</mat-error>
        </mat-form-field>
        <p class="error-text" *ngIf="error">{{ error }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Create</button>
      </mat-dialog-actions>
    </form>`
})
export class UserCreateDialogComponent {
  roles = ALL_ROLES;
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    roles: [[] as Role[], [Validators.required]]
  });

  constructor(private fb: FormBuilder, private api: IdentityApi, private ref: MatDialogRef<UserCreateDialogComponent, UserResponse>) {}

  err(name: string): string { return controlError(this.form.get(name)); }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    this.api.create(this.form.getRawValue()).subscribe({
      next: u => this.ref.close(u),
      error: e => { this.busy = false; applyFieldErrors(this.form, e); this.error = errorMessage(e); }
    });
  }
}

@Component({
  selector: 'app-user-roles-dialog',
  template: `
    <h2 mat-dialog-title>Roles for {{ data.user.displayName }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Roles</mat-label>
        <mat-select [formControl]="roles" multiple>
          <mat-option *ngFor="let r of all" [value]="r">{{ r }}</mat-option>
        </mat-select>
        <mat-error>At least one role is required</mat-error>
      </mat-form-field>
      <p class="muted" *ngIf="self">You cannot remove your own SYSTEM_ADMIN role.</p>
      <p class="error-text" *ngIf="error">{{ error }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" type="button" [disabled]="busy" (click)="save()">Save</button>
    </mat-dialog-actions>`
})
export class UserRolesDialogComponent {
  all = ALL_ROLES;
  busy = false;
  error: string | null = null;
  self = this.auth.userId === this.data.user.id;
  roles = this.fb.nonNullable.control<Role[]>([...this.data.user.roles], [Validators.required]);

  constructor(private fb: FormBuilder, private api: IdentityApi, private auth: AuthService,
              private ref: MatDialogRef<UserRolesDialogComponent, UserResponse>, @Inject(MAT_DIALOG_DATA) public data: { user: UserResponse }) {}

  save(): void {
    this.roles.markAsTouched();
    if (this.roles.invalid || this.busy) { return; }
    if (this.self && !this.roles.value.includes('SYSTEM_ADMIN')) {
      this.error = 'You cannot remove your own SYSTEM_ADMIN role.';
      return;
    }
    this.busy = true;
    this.api.setRoles(this.data.user.id, this.roles.value).subscribe({
      next: u => this.ref.close(u),
      error: e => { this.busy = false; this.error = errorMessage(e); }
    });
  }
}

@Component({
  selector: 'app-users',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Users</h1>
        <button mat-flat-button color="primary" type="button" (click)="create()"><mat-icon>add</mat-icon> New user</button>
      </div>
      <app-error-banner [message]="error" (retry)="reload()"></app-error-banner>
      <mat-progress-bar *ngIf="loading" mode="indeterminate"></mat-progress-bar>
      <app-empty-state *ngIf="!loading && !error && !items.length" icon="group" message="No users."></app-empty-state>
      <table mat-table [dataSource]="items" matSort matSortActive="createdAt" matSortDirection="desc" (matSortChange)="onSort($event)" *ngIf="items.length" class="full-width">
        <ng-container matColumnDef="email">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="email">Email</th>
          <td mat-cell *matCellDef="let u">{{ u.email }}</td>
        </ng-container>
        <ng-container matColumnDef="displayName">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="displayName">Name</th>
          <td mat-cell *matCellDef="let u">{{ u.displayName }}</td>
        </ng-container>
        <ng-container matColumnDef="roles">
          <th mat-header-cell *matHeaderCellDef>Roles</th>
          <td mat-cell *matCellDef="let u"><app-badge *ngFor="let r of u.roles" [value]="r"></app-badge></td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="status">Status</th>
          <td mat-cell *matCellDef="let u"><app-badge [value]="u.status"></app-badge></td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef mat-sort-header="createdAt">Created</th>
          <td mat-cell *matCellDef="let u">{{ u.createdAt | date:'mediumDate' }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let u">
            <button mat-button type="button" (click)="editRoles(u)">Roles</button>
            <button mat-button type="button" [disabled]="u.id === auth.userId" (click)="toggle(u)"
                    [matTooltip]="u.id === auth.userId ? 'You cannot change your own status' : ''">
              {{ u.status === 'ACTIVE' ? 'Disable' : 'Enable' }}
            </button>
            <button mat-icon-button type="button" aria-label="Copy user ID" matTooltip="Copy user ID" (click)="copy(u)">
              <mat-icon>content_copy</mat-icon>
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="cols"></tr>
        <tr mat-row *matRowDef="let r; columns: cols"></tr>
      </table>
      <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="pageSizes" (page)="onPage($event)"></mat-paginator>
    </div>`
})
export class UsersComponent extends PagedList<UserResponse> implements OnInit {
  readonly cols = ['email', 'displayName', 'roles', 'status', 'createdAt', 'actions'];

  constructor(private api: IdentityApi, public auth: AuthService, private dialog: MatDialog) { super(); }

  ngOnInit(): void { this.reload(); }

  protected fetch(q: PageQuery): Observable<Page<UserResponse>> { return this.api.list(q); }

  create(): void {
    this.dialog.open(UserCreateDialogComponent, { width: '440px' }).afterClosed().subscribe(u => { if (u) { this.reload(); } });
  }

  editRoles(u: UserResponse): void {
    this.dialog.open(UserRolesDialogComponent, { width: '440px', data: { user: u } }).afterClosed().subscribe(r => { if (r) { this.reload(); } });
  }

  toggle(u: UserResponse): void {
    if (u.id === this.auth.userId) { return; }
    this.api.setStatus(u.id, u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE').subscribe(() => this.reload());
  }

  copy(u: UserResponse): void { void navigator.clipboard?.writeText(u.id); }
}

@NgModule({
  declarations: [UsersComponent, UserCreateDialogComponent, UserRolesDialogComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: UsersComponent }])]
})
export class UsersModule {}
