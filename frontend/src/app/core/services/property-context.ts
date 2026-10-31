import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs';
import { PropertyApi } from '../api/property.api';
import { AccessResponse, PropertyResponse } from '../models';

/** Holds the property list, the selected property and the caller's access in it. */
@Injectable({ providedIn: 'root' })
export class PropertyContext {
  readonly properties$ = new BehaviorSubject<PropertyResponse[]>([]);
  readonly selectedId$ = new BehaviorSubject<string | null>(null);
  readonly access$ = new BehaviorSubject<AccessResponse | null>(null);
  readonly loaded$ = new BehaviorSubject<boolean>(false);

  constructor(private api: PropertyApi) {}

  get selectedId(): string | null { return this.selectedId$.value; }
  get access(): AccessResponse | null { return this.access$.value; }
  get canManage(): boolean { return canManage(this.access); }
  get canUseAssets(): boolean { return canUseAssets(this.access); }

  /** Loads the caller's properties (the server already scopes them) and picks a selection. */
  load(): Observable<PropertyResponse[]> {
    return this.api.list({ page: 0, size: 100, sort: { field: 'name', dir: 'asc' } }).pipe(
      map(p => p.content),
      tap(list => {
        this.properties$.next(list);
        this.loaded$.next(true);
        const current = this.selectedId;
        if (!current || !list.some(p => p.id === current)) {
          this.select(list.length ? list[0].id : null);
        }
      })
    );
  }

  select(id: string | null): void {
    if (id === this.selectedId && this.access) { return; }
    this.selectedId$.next(id);
    this.access$.next(null);
    if (!id) { return; }
    this.api.access(id).pipe(catchError(() => of(null))).subscribe(a => {
      if (this.selectedId === id) { this.access$.next(a); }
    });
  }

  /** Used when deep-linking to a record that belongs to another property. */
  ensure(id: string): void {
    if (id !== this.selectedId) { this.select(id); }
  }

  reset(): void {
    this.properties$.next([]);
    this.selectedId$.next(null);
    this.access$.next(null);
    this.loaded$.next(false);
  }
}

export function canManage(a: AccessResponse | null | undefined): boolean {
  return !!a && (a.systemAdmin || a.roles.includes('PROPERTY_MANAGER'));
}
export function isTenant(a: AccessResponse | null | undefined): boolean {
  return !!a && a.roles.includes('TENANT');
}
export function isTechnician(a: AccessResponse | null | undefined): boolean {
  return !!a && a.roles.includes('TECHNICIAN');
}
/** Tenants get 403 on all asset endpoints. */
export function canUseAssets(a: AccessResponse | null | undefined): boolean {
  return canManage(a) || isTechnician(a);
}
