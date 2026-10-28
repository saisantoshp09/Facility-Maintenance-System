import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, of } from 'rxjs';
import { WorkOrderApi } from '../../core/api/work-order.api';
import { AuthService } from '../../core/auth/auth.service';
import { AccessResponse, WorkOrderResponse } from '../../core/models';
import { PropertyContext } from '../../core/services/property-context';
import { UserDirectory } from '../../core/services/user-directory';
import { SharedModule } from '../../shared/shared.module';
import { DueDateDialogComponent, TransitionDialogComponent, WorkOrderDetailComponent } from './work-orders.module';

describe('WorkOrderDetailComponent actions', () => {
  let fixture: ComponentFixture<WorkOrderDetailComponent>;
  const ME = 'me-id';

  const wo = (over: Partial<WorkOrderResponse> = {}): WorkOrderResponse => ({
    id: 'wo1', propertyId: 'p1', unitId: null, title: 'Leak', description: 'd', priority: 'HIGH', status: 'OPEN',
    requesterId: 'someone', assigneeId: null, dueAt: null, overdue: false, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  function setup(order: WorkOrderResponse, access: AccessResponse) {
    const api = jasmine.createSpyObj('WorkOrderApi', ['get', 'history', 'comments', 'transition', 'setDueDate', 'addComment']);
    api.get.and.returnValue(of(order));
    api.history.and.returnValue(of([]));
    api.comments.and.returnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    const ctx = { access, access$: new BehaviorSubject(access), selectedId: 'p1', ensure: () => undefined, canManage: false };
    TestBed.configureTestingModule({
      declarations: [WorkOrderDetailComponent, TransitionDialogComponent, DueDateDialogComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: WorkOrderApi, useValue: api },
        { provide: PropertyContext, useValue: ctx },
        { provide: AuthService, useValue: { userId: ME, isAdmin: false, user: null } },
        { provide: UserDirectory, useValue: { name$: (id: string) => of(id) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'wo1' } } } }
      ]
    });
    fixture = TestBed.createComponent(WorkOrderDetailComponent);
    fixture.detectChanges();
  }

  const buttons = () => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.actions button'))
    .map(b => b.textContent!.trim());

  it('manager sees approve and reject on an OPEN order', () => {
    setup(wo(), { propertyId: 'p1', systemAdmin: false, roles: ['PROPERTY_MANAGER'], unitIds: [] });
    expect(buttons()).toEqual(['Approve', 'Reject', 'Change due date']);
  });

  it('assigned technician sees only technician actions', () => {
    setup(wo({ status: 'IN_PROGRESS', assigneeId: ME }), { propertyId: 'p1', systemAdmin: false, roles: ['TECHNICIAN'], unitIds: [] });
    expect(buttons()).toEqual(['Put on hold', 'Mark resolved']);
  });

  it('tenant requester can close a resolved request, nothing else', () => {
    setup(wo({ status: 'RESOLVED', requesterId: ME }), { propertyId: 'p1', systemAdmin: false, roles: ['TENANT'], unitIds: ['u1'] });
    expect(buttons()).toEqual(['Close']);
  });

  it('tenant who is not the requester gets no actions', () => {
    setup(wo({ status: 'RESOLVED' }), { propertyId: 'p1', systemAdmin: false, roles: ['TENANT'], unitIds: ['u1'] });
    expect(buttons()).toEqual([]);
  });

  it('terminal orders have no actions', () => {
    setup(wo({ status: 'CLOSED' }), { propertyId: 'p1', systemAdmin: true, roles: [], unitIds: [] });
    expect(buttons()).toEqual([]);
  });
});
