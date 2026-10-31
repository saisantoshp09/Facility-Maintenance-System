import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AssetApi } from '../../core/api/asset.api';
import { ServiceRecordView } from '../../core/models';
import { applyFieldErrors } from '../../core/services/error-utils';
import { blankToNull, notFutureDate, todayDate } from '../form-utils';

export interface ServiceRecordDialogData { assetId: string; taskId?: string | null; }

@Component({
  selector: 'app-service-record-dialog',
  template: `
    <h2 mat-dialog-title>Record service</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content>
        <p class="muted" *ngIf="data.taskId">This completes the selected preventive task and schedules the next one.</p>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Performed on</mat-label>
          <input matInput type="date" formControlName="performedOn" [max]="today">
          <mat-error>{{ err('performedOn') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Summary</mat-label>
          <textarea matInput rows="3" maxlength="1000" formControlName="summary"></textarea>
          <mat-error>{{ err('summary') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Cost (optional)</mat-label>
          <input matInput type="number" min="0" step="0.01" formControlName="cost">
          <mat-error>{{ err('cost') }}</mat-error>
        </mat-form-field>
        <p class="error-text" *ngIf="formError">{{ formError }}</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="busy">Save</button>
      </mat-dialog-actions>
    </form>`
})
export class ServiceRecordDialogComponent {
  today = todayDate();
  busy = false;
  formError: string | null = null;
  form = this.fb.group({
    performedOn: [todayDate(), [Validators.required, notFutureDate]],
    summary: ['', [Validators.required, Validators.maxLength(1000)]],
    cost: [null as number | null, [Validators.min(0)]]
  });

  constructor(private fb: FormBuilder, private api: AssetApi, private ref: MatDialogRef<ServiceRecordDialogComponent, ServiceRecordView>,
              @Inject(MAT_DIALOG_DATA) public data: ServiceRecordDialogData) {}

  err(name: string): string {
    const c = this.form.get(name);
    return c?.errors?.['server'] ?? (c?.errors?.['required'] ? 'Required' : c?.errors?.['notFuture'] ? 'Must not be in the future'
      : c?.errors?.['min'] ? 'Must be 0 or more' : c?.errors ? 'Invalid value' : '');
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    const v = this.form.getRawValue();
    this.api.addServiceRecord(this.data.assetId, {
      taskId: this.data.taskId ?? null, performedOn: v.performedOn!, summary: v.summary!.trim(),
      cost: v.cost === null || v.cost === undefined ? null : Number(v.cost)
    }).subscribe({
      next: r => this.ref.close(r),
      error: e => { this.busy = false; this.formError = applyFieldErrors(this.form, e).join('; ') || null; }
    });
  }
}
