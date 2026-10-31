import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppError } from '../models';

@Injectable({ providedIn: 'root' })
export class ErrorNotifier {
  constructor(private snack: MatSnackBar) {}

  show(err: AppError): void {
    const ref = err.correlationId ? ` (ref: ${err.correlationId})` : '';
    this.snack.open(`${err.message}${ref}`, 'Dismiss', { duration: 8000, panelClass: 'error-snack' });
  }

  info(message: string, duration = 4000): void {
    this.snack.open(message, 'OK', { duration });
  }
}
