import { Routes } from '@angular/router';
import { EwypFormComponent } from './components/ewyp-form/ewyp-form.component';

export const routes: Routes = [
  { path: '', redirectTo: '/ewyp-form', pathMatch: 'full' },
  { path: 'ewyp-form', component: EwypFormComponent },
  { path: 'ewyp-form/:id', component: EwypFormComponent }
];
