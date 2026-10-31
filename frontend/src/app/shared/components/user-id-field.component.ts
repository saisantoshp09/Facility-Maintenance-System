import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { IdentityApi } from '../../core/api/identity.api';
import { AuthService } from '../../core/auth/auth.service';
import { Role, UserResponse } from '../../core/models';
import { controlError } from '../../core/services/error-utils';

export const UUID_RE = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

/**
 * Picks a user id. SYSTEM_ADMIN gets a picker backed by GET /users; everyone else must paste a
 * user id because the backend has no user directory for non-admins.
 */
@Component({
  selector: 'app-user-id-field',
  template: `
    <mat-form-field appearance="outline" class="full-width" *ngIf="users; else paste">
      <mat-label>{{ label }}</mat-label>
      <mat-select [formControl]="control">
        <mat-option *ngFor="let u of users" [value]="u.id">{{ u.displayName }} ({{ u.email }})</mat-option>
      </mat-select>
      <mat-error>{{ err() }}</mat-error>
    </mat-form-field>
    <ng-template #paste>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>{{ label }} (user ID)</mat-label>
        <input matInput [formControl]="control" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx">
        <mat-hint>Ask an administrator for the user's ID.</mat-hint>
        <mat-error>{{ err() }}</mat-error>
      </mat-form-field>
    </ng-template>`
})
export class UserIdFieldComponent implements OnInit {
  @Input() control!: FormControl<string>;
  @Input() label = 'User';
  @Input() role?: Role;
  users: UserResponse[] | null = null;

  constructor(private auth: AuthService, private identity: IdentityApi) {}

  ngOnInit(): void {
    if (!this.auth.isAdmin) { return; }
    this.identity.list({ page: 0, size: 100, sort: { field: 'displayName', dir: 'asc' } }).subscribe({
      next: p => this.users = p.content.filter(u => u.status === 'ACTIVE' && (!this.role || u.roles.includes(this.role))),
      error: () => this.users = null
    });
  }

  err(): string { return controlError(this.control); }
}
