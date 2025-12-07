import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { KeycloakService } from '../../services/keycloak.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  constructor(
    private router: Router,
    public keycloakService: KeycloakService
  ) {}

  navigateToNewReport(): void {
    this.router.navigate(['/ewyp-form']);
  }

  navigateToSearch(): void {
    if (this.keycloakService.isController()) {
      this.router.navigate(['/ewyp-search']);
    }
  }
}
