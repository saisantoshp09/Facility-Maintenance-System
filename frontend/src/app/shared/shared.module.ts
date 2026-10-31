import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BadgeComponent } from './components/badge.component';
import { ConfirmDialogComponent } from './components/confirm-dialog.component';
import { EmptyStateComponent, ErrorBannerComponent } from './components/empty-error.component';
import { UserNameComponent } from './components/user-name.component';
import { UserIdFieldComponent } from './components/user-id-field.component';
import { ServiceRecordDialogComponent } from './components/service-record-dialog.component';

const MATERIAL = [
  MatBadgeModule, MatButtonModule, MatCardModule, MatCheckboxModule, MatDialogModule, MatDividerModule,
  MatFormFieldModule, MatIconModule, MatInputModule, MatListModule, MatMenuModule, MatPaginatorModule,
  MatProgressBarModule, MatSelectModule, MatSidenavModule, MatSlideToggleModule, MatSnackBarModule,
  MatSortModule, MatTableModule, MatTabsModule, MatToolbarModule, MatTooltipModule
];
const DECLARED = [
  BadgeComponent, ConfirmDialogComponent, EmptyStateComponent, ErrorBannerComponent, UserNameComponent,
  UserIdFieldComponent, ServiceRecordDialogComponent
];

@NgModule({
  declarations: DECLARED,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, ...MATERIAL],
  exports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule, ...MATERIAL, ...DECLARED]
})
export class SharedModule {}
