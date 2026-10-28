import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard, RoleGuard } from './core/auth/guards';
import { ShellComponent } from './core/layout/shell.component';
import { ErrorPageComponent } from './core/layout/error-page.component';

const routes: Routes = [
  { path: 'login', loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule) },
      { path: 'properties', loadChildren: () => import('./features/properties/properties.module').then(m => m.PropertiesModule) },
      { path: 'work-orders', loadChildren: () => import('./features/work-orders/work-orders.module').then(m => m.WorkOrdersModule) },
      { path: 'assets', loadChildren: () => import('./features/assets/assets.module').then(m => m.AssetsModule) },
      {
        path: 'preventive-maintenance',
        loadChildren: () => import('./features/preventive-maintenance/preventive-maintenance.module').then(m => m.PreventiveMaintenanceModule)
      },
      { path: 'notifications', loadChildren: () => import('./features/notifications/notifications.module').then(m => m.NotificationsModule) },
      { path: 'profile', loadChildren: () => import('./features/profile/profile.module').then(m => m.ProfileModule) },
      {
        path: 'users',
        canActivate: [RoleGuard],
        data: { roles: ['SYSTEM_ADMIN'] },
        loadChildren: () => import('./features/users/users.module').then(m => m.UsersModule)
      },
      { path: 'forbidden', component: ErrorPageComponent, data: { kind: 'forbidden' } },
      { path: '**', component: ErrorPageComponent, data: { kind: 'notfound' } }
    ]
  },
  { path: '**', redirectTo: 'login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
