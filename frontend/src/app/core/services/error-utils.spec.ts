import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { applyFieldErrors, controlError, errorMessage, toAppError } from './error-utils';

describe('error utils', () => {
  it('normalises the documented envelope', () => {
    const e = toAppError(new HttpErrorResponse({
      status: 400,
      error: { status: 400, code: 'VALIDATION_ERROR', message: 'Invalid', path: '/x', correlationId: 'c1', fieldErrors: [{ field: 'name', message: 'required' }] }
    }));
    expect(e.code).toBe('VALIDATION_ERROR');
    expect(e.fieldErrors.length).toBe(1);
    expect(errorMessage(e)).toContain('c1');
  });

  it('handles empty and non-JSON bodies', () => {
    expect(toAppError(new HttpErrorResponse({ status: 401, error: null })).code).toBe('UNAUTHENTICATED');
    expect(toAppError(new HttpErrorResponse({ status: 502, error: '<html>bad gateway</html>' })).code).toBe('SERVICE_UNAVAILABLE');
    expect(toAppError(new HttpErrorResponse({ status: 0 })).code).toBe('NETWORK_ERROR');
    expect(toAppError(new HttpErrorResponse({ status: 504 })).code).toBe('UPSTREAM_TIMEOUT');
  });

  it('maps field errors to form controls and returns unmatched ones', () => {
    const form = new FormGroup({ name: new FormControl('', Validators.required) });
    const left = applyFieldErrors(form, {
      status: 400, code: 'VALIDATION_ERROR', message: 'x', fieldErrors: [{ field: 'name', message: 'Too long' }, { field: 'other', message: 'Bad' }]
    });
    expect(controlError(form.get('name'))).toBe('Too long');
    expect(left).toEqual(['other: Bad']);
  });
});
