import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { NotificationApi } from '../api/notification.api';
import { SharedModule } from '../../shared/shared.module';
import { NotificationBellComponent, POLL_MS } from './notification-bell.component';

describe('NotificationBellComponent', () => {
  let fixture: ComponentFixture<NotificationBellComponent>;
  let api: jasmine.SpyObj<NotificationApi>;
  let router: Router;
  const note = (over: object = {}) => ({
    id: 'n1', eventId: 'e', type: 'WORK_ORDER_STATUS_CHANGED', subject: 'Status changed', message: 'm',
    referenceType: 'WORK_ORDER', referenceId: 'wo-9', status: 'SENT', readAt: null, createdAt: '2026-01-01T00:00:00Z', ...over
  });

  beforeEach(() => {
    api = jasmine.createSpyObj('NotificationApi', ['unreadCount', 'list', 'markRead']);
    api.unreadCount.and.returnValue(of({ count: 3 }));
    TestBed.configureTestingModule({
      declarations: [NotificationBellComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [{ provide: NotificationApi, useValue: api }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(NotificationBellComponent);
  });

  it('shows the unread count on init and polls periodically', fakeAsync(() => {
    fixture.detectChanges();
    expect(fixture.componentInstance.count).toBe(3);
    api.unreadCount.and.returnValue(of({ count: 5 }));
    tick(POLL_MS);
    expect(fixture.componentInstance.count).toBe(5);
    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('marks a notification read and deep-links to the work order', () => {
    api.markRead.and.returnValue(of(note({ readAt: 'x' }) as any));
    fixture.detectChanges();
    fixture.componentInstance.open(note() as any);
    expect(api.markRead).toHaveBeenCalledWith('n1');
    expect(fixture.componentInstance.count).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith(['/work-orders', 'wo-9']);
  });

  it('does not call markRead for an already-read notification', () => {
    fixture.detectChanges();
    fixture.componentInstance.open(note({ readAt: '2026-01-02T00:00:00Z' }) as any);
    expect(api.markRead).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalled();
  });

  it('loads the latest notifications when opened', () => {
    api.list.and.returnValue(of({ content: [note()], page: 0, size: 5, totalElements: 1, totalPages: 1 } as any));
    fixture.detectChanges();
    fixture.componentInstance.onOpen();
    expect(fixture.componentInstance.items.length).toBe(1);
  });
});
