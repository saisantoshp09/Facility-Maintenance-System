import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccessResponse, BuildingResponse, FloorRequest, FloorResponse, MembershipRequest, MembershipResponse, Page,
  PageQuery, PropertyRequest, PropertyResponse, UnitRequest, UnitResponse
} from '../models';
import { SKIP_ERROR_TOAST } from '../services/error-utils';
import { buildParams } from './params';

@Injectable({ providedIn: 'root' })
export class PropertyApi {
  private readonly base = `${environment.apiBaseUrl}/api/v1/properties`;
  constructor(private http: HttpClient) {}

  list(q: PageQuery): Observable<Page<PropertyResponse>> {
    return this.http.get<Page<PropertyResponse>>(this.base, { params: buildParams('properties', q) });
  }
  get(id: string): Observable<PropertyResponse> { return this.http.get<PropertyResponse>(`${this.base}/${id}`); }
  create(req: PropertyRequest): Observable<PropertyResponse> { return this.http.post<PropertyResponse>(this.base, req); }
  update(id: string, req: PropertyRequest): Observable<PropertyResponse> {
    return this.http.put<PropertyResponse>(`${this.base}/${id}`, req);
  }
  access(id: string): Observable<AccessResponse> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, true);
    return this.http.get<AccessResponse>(`${this.base}/${id}/access`, { context });
  }

  buildings(id: string, q: PageQuery): Observable<Page<BuildingResponse>> {
    return this.http.get<Page<BuildingResponse>>(`${this.base}/${id}/buildings`, { params: buildParams('buildings', q) });
  }
  createBuilding(id: string, name: string): Observable<BuildingResponse> {
    return this.http.post<BuildingResponse>(`${this.base}/${id}/buildings`, { name });
  }
  floors(id: string, buildingId: string): Observable<FloorResponse[]> {
    return this.http.get<FloorResponse[]>(`${this.base}/${id}/buildings/${buildingId}/floors`);
  }
  createFloor(id: string, buildingId: string, req: FloorRequest): Observable<FloorResponse> {
    return this.http.post<FloorResponse>(`${this.base}/${id}/buildings/${buildingId}/floors`, req);
  }

  units(id: string, q: PageQuery): Observable<Page<UnitResponse>> {
    return this.http.get<Page<UnitResponse>>(`${this.base}/${id}/units`, { params: buildParams('units', q) });
  }
  createUnit(id: string, req: UnitRequest): Observable<UnitResponse> {
    return this.http.post<UnitResponse>(`${this.base}/${id}/units`, req);
  }

  memberships(id: string, q: PageQuery): Observable<Page<MembershipResponse>> {
    return this.http.get<Page<MembershipResponse>>(`${this.base}/${id}/memberships`, { params: buildParams('memberships', q) });
  }
  addMembership(id: string, req: MembershipRequest): Observable<MembershipResponse> {
    return this.http.post<MembershipResponse>(`${this.base}/${id}/memberships`, req);
  }
  removeMembership(id: string, membershipId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/memberships/${membershipId}`);
  }
}
