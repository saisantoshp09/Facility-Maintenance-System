import { AccessResponse, WorkOrderResponse, WorkOrderStatus } from '../../core/models';
import { canManage, isTechnician } from '../../core/services/property-context';

/** Mirrors the backend state machine. REJECTED and CLOSED are terminal. */
export const ALLOWED_NEXT: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  OPEN: ['APPROVED', 'REJECTED'],
  APPROVED: ['ASSIGNED'],
  ASSIGNED: ['IN_PROGRESS'],
  IN_PROGRESS: ['ON_HOLD', 'RESOLVED'],
  ON_HOLD: ['IN_PROGRESS'],
  RESOLVED: ['CLOSED'],
  REJECTED: [],
  CLOSED: []
};

export interface ActionContext { userId: string | null; access: AccessResponse | null; }

const TECHNICIAN_TARGETS: WorkOrderStatus[] = ['IN_PROGRESS', 'ON_HOLD', 'RESOLVED'];

/** Targets the current user may request (UI hint only; the server is authoritative). */
export function allowedActions(wo: Pick<WorkOrderResponse, 'status' | 'assigneeId' | 'requesterId'>, ctx: ActionContext): WorkOrderStatus[] {
  const { access, userId } = ctx;
  if (!access || !userId) { return []; }
  return ALLOWED_NEXT[wo.status].filter(target => {
    if (canManage(access)) { return true; }
    if (TECHNICIAN_TARGETS.includes(target)) { return isTechnician(access) && wo.assigneeId === userId; }
    if (target === 'CLOSED') { return wo.requesterId === userId; }
    return false;
  });
}

export function actionLabel(from: WorkOrderStatus, target: WorkOrderStatus): string {
  switch (target) {
    case 'APPROVED': return 'Approve';
    case 'REJECTED': return 'Reject';
    case 'ASSIGNED': return 'Assign';
    case 'IN_PROGRESS': return from === 'ON_HOLD' ? 'Resume' : 'Start work';
    case 'ON_HOLD': return 'Put on hold';
    case 'RESOLVED': return 'Mark resolved';
    case 'CLOSED': return 'Close';
    default: return target;
  }
}
