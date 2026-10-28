export type Role = 'SYSTEM_ADMIN' | 'PROPERTY_MANAGER' | 'TENANT' | 'TECHNICIAN';
export const ALL_ROLES: Role[] = ['SYSTEM_ADMIN', 'PROPERTY_MANAGER', 'TENANT', 'TECHNICIAN'];
export type UserStatus = 'ACTIVE' | 'DISABLED';
export type MembershipRole = 'PROPERTY_MANAGER' | 'TENANT' | 'TECHNICIAN';
export const MEMBERSHIP_ROLES: MembershipRole[] = ['PROPERTY_MANAGER', 'TENANT', 'TECHNICIAN'];

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export type WorkOrderStatus =
  | 'OPEN' | 'APPROVED' | 'REJECTED' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'RESOLVED' | 'CLOSED';
export const WORK_ORDER_STATUSES: WorkOrderStatus[] = [
  'OPEN', 'APPROVED', 'REJECTED', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'RESOLVED', 'CLOSED'
];

export type AssetStatus = 'ACTIVE' | 'UNDER_MAINTENANCE' | 'RETIRED';
export const ASSET_STATUSES: AssetStatus[] = ['ACTIVE', 'UNDER_MAINTENANCE', 'RETIRED'];
export type TaskStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';
export const TASK_STATUSES: TaskStatus[] = ['PENDING', 'COMPLETED', 'CANCELLED'];
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';
export const NOTIFICATION_STATUSES: NotificationStatus[] = ['PENDING', 'SENT', 'FAILED'];

export function humanize(value: string | null | undefined): string {
  if (!value) { return ''; }
  const s = value.replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SortSpec { field: string; dir: 'asc' | 'desc'; }
export interface PageQuery { page?: number; size?: number; sort?: SortSpec | null; }

/** Sort fields accepted by each backend list endpoint. Anything else yields HTTP 400. */
export const SORT_FIELDS = {
  users: ['email', 'displayName', 'status', 'createdAt'],
  properties: ['name', 'city', 'createdAt'],
  buildings: ['name', 'createdAt'],
  units: ['unitNumber', 'createdAt'],
  memberships: ['createdAt', 'role'],
  workOrders: ['createdAt', 'updatedAt', 'dueAt', 'priority', 'status'],
  comments: ['createdAt'],
  assets: ['name', 'category', 'status', 'createdAt', 'updatedAt'],
  tasks: ['dueDate'],
  serviceRecords: ['dueDate', 'performedOn', 'createdAt'],
  notifications: ['createdAt', 'status', 'readAt']
} as const;
export type SortEndpoint = keyof typeof SORT_FIELDS;

export interface ApiFieldError { field: string; message: string; }
/** Normalised error thrown by the HTTP error interceptor. */
export interface AppError {
  status: number;
  code: string;
  message: string;
  path?: string;
  correlationId?: string;
  fieldErrors: ApiFieldError[];
}

// ---- Identity
export interface LoginResponse { access_token: string; token_type: string; expires_in: number; }
export interface UserResponse {
  id: string; email: string; displayName: string; status: UserStatus; roles: Role[];
  createdAt: string; updatedAt: string; lastLoginAt: string | null;
}
export interface CreateUserRequest { email: string; displayName: string; password: string; roles: Role[]; }

// ---- Property
export interface PropertyRequest {
  name: string; addressLine: string; city: string; postalCode: string; country: string;
}
export interface PropertyResponse extends PropertyRequest {
  id: string; active: boolean; createdAt: string; updatedAt: string;
}
export interface BuildingResponse { id: string; propertyId: string; name: string; }
export interface FloorRequest { levelNumber: number; label?: string | null; }
export interface FloorResponse { id: string; propertyId: string; buildingId: string; levelNumber: number; label: string | null; }
export interface UnitRequest { buildingId?: string | null; floorId?: string | null; unitNumber: string; }
export interface UnitResponse { id: string; propertyId: string; buildingId: string | null; floorId: string | null; unitNumber: string; }
export interface MembershipRequest { userId: string; role: MembershipRole; unitId?: string | null; }
export interface MembershipResponse {
  id: string; propertyId: string; userId: string; role: MembershipRole; unitId: string | null; createdAt: string;
}
export interface AccessResponse { propertyId: string; systemAdmin: boolean; roles: MembershipRole[]; unitIds: string[]; }

// ---- Work orders
export interface CreateWorkOrderRequest {
  propertyId: string; unitId?: string | null; title: string; description: string; priority: Priority; dueAt?: string | null;
}
export interface WorkOrderResponse {
  id: string; propertyId: string; unitId: string | null; title: string; description: string;
  priority: Priority; status: WorkOrderStatus; requesterId: string; assigneeId: string | null;
  dueAt: string | null; overdue: boolean; createdAt: string; updatedAt: string;
}
export interface TransitionRequest {
  targetStatus: WorkOrderStatus; note?: string | null; assigneeId?: string | null; dueAt?: string | null;
}
export interface CommentResponse { id: string; workOrderId: string; authorId: string; body: string; createdAt: string; }
export interface HistoryResponse {
  id: string; fromStatus: WorkOrderStatus | null; toStatus: WorkOrderStatus; actorId: string; note: string | null; createdAt: string;
}
export interface WorkOrderFilter {
  propertyId: string; status?: WorkOrderStatus | null; priority?: Priority | null; assigneeId?: string | null;
  overdue?: boolean | null; dueBefore?: string | null;
}

// ---- Assets
export interface AssetRequest {
  propertyId: string; unitId?: string | null; name: string; category: string; serialNumber?: string | null;
  location: string; installedOn?: string | null;
}
export type AssetUpdateRequest = Omit<AssetRequest, 'propertyId'>;
export interface AssetView {
  id: string; propertyId: string; unitId: string | null; name: string; category: string; serialNumber: string | null;
  location: string; status: AssetStatus; installedOn: string | null; createdAt: string; updatedAt: string; version: number;
}
export interface ScheduleRequest { title: string; description: string; intervalDays: number; startDate: string; }
export interface ScheduleView {
  id: string; assetId: string; title: string; description: string; intervalDays: number; startDate: string;
  nextDueDate: string | null; lastCompletedDate: string | null; active: boolean; version: number;
}
export interface TaskView {
  id: string; scheduleId: string; assetId: string; propertyId: string; dueDate: string; status: TaskStatus; completedAt: string | null;
}
export interface ServiceRecordRequest { taskId?: string | null; performedOn: string; summary: string; cost?: number | null; }
export interface ServiceRecordView {
  id: string; assetId: string; taskId: string | null; performedBy: string; performedOn: string; summary: string;
  cost: number | null; createdAt: string;
}
export interface TaskFilter {
  propertyId: string; status?: TaskStatus | null; overdue?: boolean | null; dueBefore?: string | null;
}
export interface AssetFilter { propertyId: string; status?: AssetStatus | null; category?: string | null; }

// ---- Notifications
export interface NotificationResponse {
  id: string; eventId: string; type: string; subject: string; message: string; referenceType: string;
  referenceId: string; status: NotificationStatus; readAt: string | null; createdAt: string;
}
export interface NotificationAttempt {
  id: string; attemptNo: number; outcome: 'SUCCESS' | 'FAILURE'; error: string | null; attemptedAt: string;
}
