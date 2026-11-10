import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Subscription, filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenStore } from '../auth/token-store';
import { PropertyContext } from '../services/property-context';
import { ErrorNotifier } from '../services/error-notifier';
import { UserDirectory } from '../services/user-directory';
import { errorMessage } from '../services/error-utils';

interface NavItem { label: string; icon: string; link: string; show: boolean; }

@Component({
  selector: 'app-shell',
  template: `
    <mat-toolbar class="top">
      <div class="brand-group">
        <button mat-icon-button type="button" aria-label="Toggle navigation" (click)="nav.toggle()">
          <mat-icon>menu</mat-icon>
        </button>
        <span class="brand-mark" aria-hidden="true"><mat-icon>domain</mat-icon></span>
        <span class="brand">Facility Maintenance</span>
      </div>
      <div class="top-actions">
        <div class="property-picker" *ngIf="(ctx.properties$ | async) as props">
          <mat-icon aria-hidden="true">apartment</mat-icon>
          <div class="property-picker-content">
            <span class="property-label">Property</span>
            <mat-select [value]="ctx.selectedId$ | async" (selectionChange)="ctx.select($event.value)"
                        [disabled]="!props.length" [placeholder]="props.length ? 'Select property' : 'No properties'"
                        aria-label="Select property">
              <mat-option *ngFor="let p of props" [value]="p.id">{{ p.name }}</mat-option>
            </mat-select>
          </div>
        </div>
        <app-notification-bell></app-notification-bell>
        <button mat-icon-button type="button" [matMenuTriggerFor]="userMenu" aria-label="Account menu">
          <mat-icon>account_circle</mat-icon>
        </button>
      </div>
      <mat-menu #userMenu="matMenu">
        <div class="menu-user" *ngIf="auth.user as u">
          <strong>{{ u.displayName }}</strong><br><span class="muted">{{ u.email }}</span>
        </div>
        <a mat-menu-item routerLink="/profile"><mat-icon>person</mat-icon>Profile</a>
        <button mat-menu-item type="button" (click)="signOut()"><mat-icon>logout</mat-icon>Sign out</button>
      </mat-menu>
    </mat-toolbar>
    <mat-sidenav-container class="container">
      <mat-sidenav #nav [mode]="mobile ? 'over' : 'side'" [opened]="!mobile">
        <div class="nav-heading">WORKSPACE</div>
        <mat-nav-list aria-label="Main navigation">
          <ng-container *ngFor="let item of items(); trackBy: trackByLink">
            <a mat-list-item *ngIf="item.show" [routerLink]="item.link" routerLinkActive="active"
               ariaCurrentWhenActive="page" (click)="mobile && nav.close()">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          </ng-container>
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <nav class="crumbs" aria-label="Breadcrumb" *ngIf="crumbs.length">
          <ng-container *ngFor="let c of crumbs; let last = last">
            <a *ngIf="!last" [routerLink]="c.link">{{ c.label }}</a>
            <span *ngIf="last" aria-current="page">{{ c.label }}</span>
            <mat-icon *ngIf="!last" class="sep">chevron_right</mat-icon>
          </ng-container>
        </nav>
        <div class="banner warn" *ngIf="expiring" role="alert">
          Your session expires in less than a minute. Save your work; you will need to sign in again.
          <button mat-button type="button" (click)="expiring = false">Dismiss</button>
        </div>
        <app-error-banner [message]="propsError" (retry)="loadProperties()"></app-error-banner>
        <router-outlet></router-outlet>
      </mat-sidenav-content>
    </mat-sidenav-container>`,
  styles: [`
    :host { display: flex; flex-direction: column; height: 100vh; height: 100dvh; color: #172b4d; }
    .top { position: relative; z-index: 2; flex: 0 0 auto; min-height: 72px; height: auto;
      padding: 10px 24px; gap: 16px; justify-content: space-between; background: #fff;
      color: #172b4d; border-bottom: 1px solid #e3eaf3; box-shadow: 0 2px 8px rgba(24, 49, 85, .04); }
    .brand-group, .top-actions { display: flex; align-items: center; min-width: 0; gap: 12px; }
    .brand-group { flex-shrink: 0; }
    .brand-mark { display: grid; place-items: center; width: 38px; height: 38px;
      border-radius: 10px; background: #e8f0ff; color: #2459b5; }
    .brand { font-size: 17px; font-weight: 600; letter-spacing: -.2px; white-space: nowrap; }
    .top-actions { justify-content: flex-end; }
    .property-picker { box-sizing: border-box; display: flex; align-items: center; gap: 10px;
      width: 220px; min-width: 0; padding: 6px 12px; border: 1px solid #dce5f0;
      border-radius: 9px; background: #f8fafc; line-height: 1.2; }
    .property-picker > mat-icon { color: #6b7f99; font-size: 20px; width: 20px; height: 20px; flex-shrink: 0; }
    .property-picker-content { min-width: 0; flex: 1; }
    .property-label { display: block; margin-bottom: 3px; color: #687a91; font-size: 11px; font-weight: 500; }
    .property-picker mat-select { display: block; font-size: 13px; font-weight: 500; }
    .container { flex: 1; min-height: 0; background: #f5f7fb; }
    mat-sidenav { width: 240px; border-right: 1px solid #e3eaf3; background: #fff; }
    mat-sidenav-content { min-width: 0; }
    .nav-heading { padding: 28px 24px 8px; color: #8290a4; font-size: 11px; font-weight: 700; letter-spacing: .09em; }
    mat-nav-list { padding: 4px 12px; }
    mat-nav-list a { margin-bottom: 4px; border-radius: 8px; color: #52657e; }
    mat-nav-list a mat-icon { color: #74869c; }
    mat-nav-list a:hover { background: #f3f6fb; }
    mat-nav-list a.active { background: #eaf1ff; color: #2459b5; font-weight: 600; }
    mat-nav-list a.active mat-icon { color: #2459b5; }
    .menu-user { padding: 12px 16px; }
    .crumbs { display: flex; align-items: center; padding: 16px 24px 0; color: #687a91; font-size: 13px; flex-wrap: wrap; }
    .crumbs .sep { font-size: 16px; height: 16px; width: 16px; }
    .banner.warn { background: #fff8e1; color: #8d6e00; padding: 8px 16px; }
    @media (max-width: 720px) {
      .top { padding: 8px 12px; gap: 6px; flex-wrap: wrap; }
      .brand-group { width: 100%; gap: 8px; }
      .brand { font-size: 16px; }
      .top-actions { width: 100%; gap: 4px; }
      .property-picker { flex: 1; width: auto; }
      .crumbs { padding: 12px 16px 0; }
    }
  `]
})
export class ShellComponent implements OnInit, OnDestroy {
  mobile = false;
  expiring = false;
  propsError: string | null = null;
  crumbs: { label: string; link: string }[] = [];
  private subs = new Subscription();

  constructor(public auth: AuthService, public ctx: PropertyContext, private tokens: TokenStore, private router: Router,
              private notifier: ErrorNotifier, private bp: BreakpointObserver, private dir: UserDirectory) {}

  ngOnInit(): void {
    this.subs.add(this.bp.observe('(max-width: 800px)').subscribe(r => this.mobile = r.matches));
    this.subs.add(this.tokens.expiringSoon$.subscribe(() => this.expiring = true));
    this.subs.add(this.tokens.expired$.subscribe(() => this.signOut(true)));
    this.subs.add(this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => this.buildCrumbs()));
    this.buildCrumbs();
    if (!this.auth.user) { this.auth.loadProfile().subscribe({ error: () => undefined }); }
    this.loadProperties();
  }

  loadProperties(): void {
    this.propsError = null;
    this.ctx.load().subscribe({ error: e => this.propsError = errorMessage(e) });
  }

  trackByLink(_i: number, item: NavItem): string { return item.link; }

  items(): NavItem[] {
    const a = this.ctx.access;
    const assets = this.auth.isAdmin || !a || this.ctx.canUseAssets;
    return [
      { label: 'Dashboard', icon: 'dashboard', link: '/dashboard', show: true },
      { label: 'Properties', icon: 'apartment', link: '/properties', show: true },
      { label: 'Work orders', icon: 'build', link: '/work-orders', show: true },
      { label: 'Assets', icon: 'precision_manufacturing', link: '/assets', show: assets },
      { label: 'Preventive tasks', icon: 'event_repeat', link: '/preventive-maintenance', show: assets },
      { label: 'Notifications', icon: 'notifications', link: '/notifications', show: true },
      { label: 'Users', icon: 'group', link: '/users', show: this.auth.isAdmin }
    ];
  }

  signOut(expired = false): void {
    this.dir.clear();
    this.ctx.reset();
    this.auth.logout(this.router.url, expired ? { expired: '1' } : {});
  }

  private buildCrumbs(): void {
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-/i;
    const segs = this.router.url.split('?')[0].split('/').filter(s => !!s);
    let link = '';
    this.crumbs = segs.map(s => {
      link += '/' + s;
      const label = uuid.test(s) ? 'Details' : s.replace(/-/g, ' ').replace(/^./, c => c.toUpperCase());
      return { label, link };
    });
    if (this.crumbs.length <= 1) { this.crumbs = []; }
  }

  ngOnDestroy(): void { this.subs.unsubscribe(); }
}
