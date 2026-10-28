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
    <mat-toolbar color="primary" class="top">
      <button mat-icon-button type="button" aria-label="Toggle navigation" (click)="nav.toggle()">
        <mat-icon>menu</mat-icon>
      </button>
      <span class="brand">Facility Maintenance</span>
      <span class="spacer"></span>
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="prop-select" *ngIf="(ctx.properties$ | async) as props">
        <mat-label>Property</mat-label>
        <mat-select [value]="ctx.selectedId$ | async" (selectionChange)="ctx.select($event.value)" aria-label="Select property">
          <mat-option *ngFor="let p of props" [value]="p.id">{{ p.name }}</mat-option>
        </mat-select>
      </mat-form-field>
      <app-notification-bell></app-notification-bell>
      <button mat-icon-button type="button" [matMenuTriggerFor]="userMenu" aria-label="Account menu">
        <mat-icon>account_circle</mat-icon>
      </button>
      <mat-menu #userMenu="matMenu">
        <div class="menu-user" *ngIf="auth.user as u">
          <strong>{{ u.displayName }}</strong><br><span class="muted">{{ u.email }}</span>
        </div>
        <a mat-menu-item routerLink="/profile"><mat-icon>person</mat-icon>Profile</a>
        <button mat-menu-item type="button" (click)="signOut()"><mat-icon>logout</mat-icon>Sign out</button>
      </mat-menu>
    </mat-toolbar>
    <mat-sidenav-container class="container">
      <mat-sidenav #nav [mode]="mobile ? 'over' : 'side'" [opened]="!mobile" (click)="mobile && nav.close()">
        <mat-nav-list>
          <ng-container *ngFor="let item of items()">
            <a mat-list-item *ngIf="item.show" [routerLink]="item.link" routerLinkActive="active">
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
    :host { display: flex; flex-direction: column; height: 100vh; }
    .top { position: relative; z-index: 2; gap: 8px; }
    .spacer { flex: 1; }
    .container { flex: 1; }
    mat-sidenav { width: 230px; }
    .prop-select { width: 200px; font-size: 13px; margin-right: 8px; margin-top: 6px; }
    .menu-user { padding: 8px 16px; }
    .active { background: rgba(63,81,181,.12); }
    .crumbs { display: flex; align-items: center; padding: 8px 16px 0; font-size: 13px; flex-wrap: wrap; }
    .crumbs .sep { font-size: 16px; height: 16px; width: 16px; }
    .banner.warn { background: #fff8e1; color: #8d6e00; padding: 8px 16px; }
    @media (max-width: 600px) { .brand { display: none; } .prop-select { width: 130px; } }
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
