import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { EwypFormComponent } from './components/ewyp-form/ewyp-form.component';
import { EwypSearchComponent } from './components/ewyp-search/ewyp-search.component';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'ewyp-search', component: EwypSearchComponent },
  { path: 'ewyp-form', component: EwypFormComponent },
  { path: 'ewyp-form/:id', component: EwypFormComponent }
];
