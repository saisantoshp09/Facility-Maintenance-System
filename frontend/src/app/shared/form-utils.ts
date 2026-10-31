import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Converts a <input type="datetime-local"> value to a UTC ISO instant. */
export function localInputToIso(value: string | null | undefined): string | null {
  if (!value) { return null; }
  const d = new Date(value);
  return isNaN(d.getTime()) ? null : d.toISOString();
}

export function todayDate(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function futureDateTime(control: AbstractControl): ValidationErrors | null {
  const v = control.value as string;
  if (!v) { return null; }
  const d = new Date(v);
  return !isNaN(d.getTime()) && d.getTime() <= Date.now() ? { future: true } : null;
}

export function notFutureDate(control: AbstractControl): ValidationErrors | null {
  const v = control.value as string;
  return v && v > todayDate() ? { notFuture: true } : null;
}

export function blankToNull(v: string | null | undefined): string | null {
  const t = (v ?? '').trim();
  return t ? t : null;
}
