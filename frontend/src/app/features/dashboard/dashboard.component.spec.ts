import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of } from 'rxjs';
import { AssetApi } from '../../core/api/asset.api';
import { NotificationApi } from '../../core/api/notification.api';
import { WorkOrderApi } from '../../core/api/work-order.api';
import { AccessResponse, PropertyResponse } from '../../core/models';
import { PropertyContext } from '../../core/services/property-context';
import { SharedModule } from '../../shared/shared.module';
import { DashboardComponent } from './dashboard.module';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  const properties$ = new BehaviorSubject<PropertyResponse[]>([]);
  const selectedId$ = new BehaviorSubject<string | null>(null);
  const access$ = new BehaviorSubject<AccessResponse | null>(null);
  const loaded$ = new BehaviorSubject(false);
  const ctx = {
    properties$, selectedId$, access$, loaded$,
    get selectedId(): string | null { return selectedId$.value; },
    get access(): AccessResponse | null { return access$.value; }
  };
  const wo = jasmine.createSpyObj('WorkOrderApi', ['list']);
  const assets = jasmine.createSpyObj('AssetApi', ['tasksDue', 'tasks']);
  const notifications = jasmine.createSpyObj('NotificationApi', ['unreadCount']);

  beforeEach(() => {
    properties$.next([]);
    selectedId$.next(null);
    access$.next(null);
    loaded$.next(false);
    wo.list.and.returnValue(of({ content: [], totalElements: 4 }));
    assets.tasksDue.and.returnValue(of([{}, {}]));
    assets.tasks.and.returnValue(of({ content: [], totalElements: 1 }));
    notifications.unreadCount.and.returnValue(of({ count: 3 }));
    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: PropertyContext, useValue: ctx },
        { provide: WorkOrderApi, useValue: wo },
        { provide: AssetApi, useValue: assets },
        { provide: NotificationApi, useValue: notifications }
      ]
    });
    fixture = TestBed.createComponent(DashboardComponent);
  });

  it('only shows the membership empty state after properties have loaded', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.empty-card')).toBeNull();
    loaded$.next(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.empty-card').textContent)
      .toContain('You are not a member of any property yet.');
    expect(fixture.nativeElement.querySelector('.empty-card').textContent).toContain('administrator');
  });

  it('renders actual property counts and their existing filtered links', () => {
    selectedId$.next('property-1');
    access$.next({ propertyId: 'property-1', systemAdmin: false, roles: ['TECHNICIAN'], unitIds: [] });
    fixture.detectChanges();
    const cards: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.stat'));
    expect(cards.length).toBe(8);
    expect(cards[0].textContent).toContain('4');
    expect(cards[0].querySelector('a')?.getAttribute('href')).toContain('status=OPEN');
    expect(cards[5].textContent).toContain('2');
    expect(cards[7].textContent).toContain('3');
    expect(wo.list).toHaveBeenCalledWith(
      { propertyId: 'property-1', status: 'OPEN' }, { page: 0, size: 1 }, true
    );
  });

  it('does not request asset statistics for tenants', () => {
    assets.tasksDue.calls.reset();
    assets.tasks.calls.reset();
    selectedId$.next('property-1');
    access$.next({ propertyId: 'property-1', systemAdmin: false, roles: ['TENANT'], unitIds: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.stat').length).toBe(6);
    expect(assets.tasksDue).not.toHaveBeenCalled();
    expect(assets.tasks).not.toHaveBeenCalled();
  });

  it('clears the previous property statistics until the new access is loaded', () => {
    selectedId$.next('property-1');
    access$.next({ propertyId: 'property-1', systemAdmin: false, roles: ['TENANT'], unitIds: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.stat').length).toBe(6);
    wo.list.calls.reset();
    selectedId$.next('property-2');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.stat').length).toBe(0);
    expect(wo.list).not.toHaveBeenCalled();
    access$.next({ propertyId: 'property-2', systemAdmin: false, roles: ['TENANT'], unitIds: [] });
    fixture.detectChanges();
    expect(wo.list).toHaveBeenCalledWith(
      { propertyId: 'property-2', status: 'OPEN' }, { page: 0, size: 1 }, true
    );
  });
});
