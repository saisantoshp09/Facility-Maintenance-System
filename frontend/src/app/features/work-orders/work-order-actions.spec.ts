import { AccessResponse, WorkOrderStatus, WORK_ORDER_STATUSES } from '../../core/models';
import { ALLOWED_NEXT, allowedActions } from './work-order-actions';

describe('allowedActions (status x role matrix)', () => {
  const ME = 'me';
  const access = (roles: AccessResponse['roles'], systemAdmin = false): AccessResponse =>
    ({ propertyId: 'p', systemAdmin, roles, unitIds: [] });
  const wo = (status: WorkOrderStatus, over: Partial<{ assigneeId: string | null; requesterId: string }> = {}) =>
    ({ status, assigneeId: null, requesterId: 'someone', ...over });

  it('managers and admins get every transition the state machine allows', () => {
    for (const s of WORK_ORDER_STATUSES) {
      expect(allowedActions(wo(s), { userId: ME, access: access(['PROPERTY_MANAGER']) })).toEqual(ALLOWED_NEXT[s]);
      expect(allowedActions(wo(s), { userId: ME, access: access([], true) })).toEqual(ALLOWED_NEXT[s]);
    }
  });

  it('terminal states have no actions for anyone', () => {
    for (const s of ['REJECTED', 'CLOSED'] as WorkOrderStatus[]) {
      expect(allowedActions(wo(s, { requesterId: ME, assigneeId: ME }), { userId: ME, access: access(['PROPERTY_MANAGER', 'TENANT', 'TECHNICIAN']) })).toEqual([]);
    }
  });

  it('the assigned technician may start, hold, resume and resolve only', () => {
    const ctx = { userId: ME, access: access(['TECHNICIAN']) };
    expect(allowedActions(wo('ASSIGNED', { assigneeId: ME }), ctx)).toEqual(['IN_PROGRESS']);
    expect(allowedActions(wo('IN_PROGRESS', { assigneeId: ME }), ctx)).toEqual(['ON_HOLD', 'RESOLVED']);
    expect(allowedActions(wo('ON_HOLD', { assigneeId: ME }), ctx)).toEqual(['IN_PROGRESS']);
    expect(allowedActions(wo('RESOLVED', { assigneeId: ME }), ctx)).toEqual([]);
    expect(allowedActions(wo('OPEN', { assigneeId: ME }), ctx)).toEqual([]);
  });

  it('a technician who is not the assignee gets nothing', () => {
    expect(allowedActions(wo('IN_PROGRESS', { assigneeId: 'other' }), { userId: ME, access: access(['TECHNICIAN']) })).toEqual([]);
  });

  it('a tenant may only close their own resolved request', () => {
    const ctx = { userId: ME, access: access(['TENANT']) };
    expect(allowedActions(wo('RESOLVED', { requesterId: ME }), ctx)).toEqual(['CLOSED']);
    expect(allowedActions(wo('RESOLVED', { requesterId: 'x' }), ctx)).toEqual([]);
    expect(allowedActions(wo('OPEN', { requesterId: ME }), ctx)).toEqual([]);
  });

  it('returns nothing without access info', () => {
    expect(allowedActions(wo('OPEN'), { userId: ME, access: null })).toEqual([]);
  });
});
