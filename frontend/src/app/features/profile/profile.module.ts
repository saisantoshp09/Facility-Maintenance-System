import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';

@Component({
  selector: 'app-profile',
  template: `
    <div class="page">
      <div class="page-header"><h1>My profile</h1></div>
      <mat-card *ngIf="auth.user as u">
        <p><strong>Name:</strong> {{ u.displayName }}</p>
        <p><strong>Email:</strong> {{ u.email }}</p>
        <p><strong>User ID:</strong> <code>{{ u.id }}</code></p>
        <p><strong>Status:</strong> <app-badge [value]="u.status"></app-badge></p>
        <p><strong>Roles:</strong> <app-badge *ngFor="let r of u.roles" [value]="r"></app-badge></p>
        <p><strong>Last sign-in:</strong> {{ u.lastLoginAt ? (u.lastLoginAt | date:'medium') : '—' }}</p>
        <p class="muted">Your per-property permissions depend on your membership in each property.</p>
      </mat-card>
    </div>`
})
export class ProfileComponent {
  constructor(public auth: AuthService) {}
}

@NgModule({
  declarations: [ProfileComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: ProfileComponent }])]
})
export class ProfileModule {}
