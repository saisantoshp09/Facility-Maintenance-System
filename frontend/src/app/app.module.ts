import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { AuthInterceptor } from './core/auth/auth.interceptor';
import { ErrorInterceptor } from './core/services/error.interceptor';
import { ShellComponent } from './core/layout/shell.component';
import { NotificationBellComponent } from './core/layout/notification-bell.component';
import { ErrorPageComponent } from './core/layout/error-page.component';

@NgModule({
  declarations: [AppComponent, ShellComponent, NotificationBellComponent, ErrorPageComponent],
  imports: [BrowserModule, BrowserAnimationsModule, HttpClientModule, SharedModule, AppRoutingModule],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
