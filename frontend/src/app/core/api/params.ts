import { HttpParams } from '@angular/common/http';
import { PageQuery, SORT_FIELDS, SortEndpoint } from '../models';

export type ParamValue = string | number | boolean | null | undefined;

/** Builds query params, dropping empty values and rejecting sort fields the backend does not support. */
export function buildParams(
  endpoint: SortEndpoint | null, q: PageQuery = {}, extra: Record<string, ParamValue> = {}
): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(extra)) {
    if (v !== null && v !== undefined && v !== '') { p = p.set(k, String(v)); }
  }
  if (q.page !== undefined) { p = p.set('page', q.page); }
  if (q.size !== undefined) { p = p.set('size', Math.min(Math.max(q.size, 1), 100)); }
  if (q.sort && endpoint) {
    const allowed = SORT_FIELDS[endpoint] as readonly string[];
    if (allowed.includes(q.sort.field)) {
      p = p.set('sort', `${q.sort.field},${q.sort.dir}`);
    }
  }
  return p;
}
