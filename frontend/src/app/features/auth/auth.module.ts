import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login.component';
import { RegisterComponent } from './register.component';

@NgModule({
  declarations: [LoginComponent, RegisterComponent],
  imports: [SharedModule, RouterModule.forChild([
    { path: '', pathMatch: 'full', redirectTo: 'login' },
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent }
  ])]
})
export class AuthModule {}
