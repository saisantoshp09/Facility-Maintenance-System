import { HttpContextToken, HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiFieldError, AppError } from '../models';

/** Set on a request to suppress the global error snackbar (caller handles the error). */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

const DEFAULT_MESSAGES: Record<number, [string, string]> = {
  0: ['NETWORK_ERROR', 'Cannot reach the server. Check your connection and try again.'],
  400: ['BAD_REQUEST', 'The request was not valid.'],
  401: ['UNAUTHENTICATED', 'Your session has expired. Please sign in again.'],
  403: ['ACCESS_DENIED', "You don't have permission to do that."],
  404: ['NOT_FOUND', 'Not found, or not available to you.'],
  405: ['METHOD_NOT_ALLOWED', 'This operation is not supported.'],
  409: ['CONFLICT', 'The resource was changed or the action is not allowed in its current state.'],
  502: ['SERVICE_UNAVAILABLE', 'A service is temporarily unavailable. Please retry.'],
  503: ['SERVICE_UNAVAILABLE', 'A service is temporarily unavailable. Please retry.'],
  504: ['UPSTREAM_TIMEOUT', 'The server took too long to respond. Please retry.']
};

/** Normalises any HTTP failure, including empty or non-JSON bodies (e.g. gateway 401). */
export function toAppError(err: HttpErrorResponse): AppError {
  const body = err.error;
  const status = err.status ?? 0;
  const [defCode, defMsg] = DEFAULT_MESSAGES[status] ?? ['INTERNAL_ERROR', 'Something went wrong on the server.'];
  if (body && typeof body === 'object' && !(body instanceof Blob) && !(body instanceof ProgressEvent)) {
    const fieldErrors: ApiFieldError[] = Array.isArray(body.fieldErrors) ? body.fieldErrors : [];
    return {
      status,
      code: typeof body.code === 'string' ? body.code : defCode,
      message: typeof body.message === 'string' && body.message ? body.message : defMsg,
      path: body.path,
      correlationId: body.correlationId,
      fieldErrors
    };
  }
  return { status, code: defCode, message: defMsg, fieldErrors: [] };
}

export function isAppError(e: unknown): e is AppError {
  return !!e && typeof e === 'object' && 'status' in e && 'code' in e && 'fieldErrors' in e;
}

export function errorMessage(e: unknown): string {
  if (isAppError(e)) {
    return e.correlationId ? `${e.message} (ref: ${e.correlationId})` : e.message;
  }
  return 'Unexpected error';
}

/** Copies server field errors onto form controls; returns messages that matched no control. */
export function applyFieldErrors(form: FormGroup, err: unknown): string[] {
  const leftover: string[] = [];
  if (!isAppError(err)) { return leftover; }
  for (const fe of err.fieldErrors) {
    const ctrl: AbstractControl | null = form.get(fe.field);
    if (ctrl) {
      ctrl.setErrors({ ...(ctrl.errors ?? {}), server: fe.message });
      ctrl.markAsTouched();
    } else {
      leftover.push(`${fe.field}: ${fe.message}`);
    }
  }
  if (err.fieldErrors.length === 0 && err.message) { leftover.push(err.message); }
  return leftover;
}

export function controlError(ctrl: AbstractControl | null): string {
  if (!ctrl || !ctrl.errors) { return ''; }
  const e = ctrl.errors;
  if (e['server']) { return e['server']; }
  if (e['required']) { return 'Required'; }
  if (e['maxlength']) { return `At most ${e['maxlength'].requiredLength} characters`; }
  if (e['minlength']) { return `At least ${e['minlength'].requiredLength} characters`; }
  if (e['email']) { return 'Enter a valid email'; }
  if (e['pattern']) { return 'Invalid format'; }
  if (e['min']) { return `Must be at least ${e['min'].min}`; }
  if (e['max']) { return `Must be at most ${e['max'].max}`; }
  if (e['future']) { return 'Must be in the future'; }
  if (e['notFuture']) { return 'Must not be in the future'; }
  return 'Invalid value';
}
