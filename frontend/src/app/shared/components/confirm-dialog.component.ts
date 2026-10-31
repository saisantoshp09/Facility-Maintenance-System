import { Component, Inject } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface ConfirmData {
  title: string;
  message?: string;
  confirmText?: string;
  warn?: boolean;
  reasonLabel?: string;
  reasonRequired?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p *ngIf="data.message">{{ data.message }}</p>
      <mat-form-field *ngIf="data.reasonLabel" class="full-width" appearance="outline">
        <mat-label>{{ data.reasonLabel }}</mat-label>
        <textarea matInput cdkFocusInitial rows="3" maxlength="1000" [formControl]="reason"></textarea>
        <mat-error>Required</mat-error>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close>Cancel</button>
      <button mat-flat-button type="button" [color]="data.warn ? 'warn' : 'primary'" (click)="ok()">
        {{ data.confirmText || 'Confirm' }}
      </button>
    </mat-dialog-actions>`
})
export class ConfirmDialogComponent {
  reason = new FormControl('', { nonNullable: true });

  constructor(private ref: MatDialogRef<ConfirmDialogComponent, { reason: string } | undefined>,
              @Inject(MAT_DIALOG_DATA) public data: ConfirmData) {
    if (data.reasonRequired) { this.reason.addValidators(Validators.required); }
  }

  ok(): void {
    this.reason.markAsTouched();
    if (this.reason.invalid) { return; }
    this.ref.close({ reason: this.reason.value.trim() });
  }
}
