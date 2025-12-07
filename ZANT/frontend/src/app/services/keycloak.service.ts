import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloak: Keycloak;
  private authenticated: boolean = false;

  constructor() {
    this.keycloak = new Keycloak({
      url: 'http://localhost:8080',
      realm: 'zant',
      clientId: 'zant-frontend'
    });
  }

  async init(): Promise<boolean> {
    try {
      this.authenticated = await this.keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        pkceMethod: 'S256'
      });

      // Auto-refresh token
      if (this.authenticated) {
        setInterval(() => {
          this.keycloak.updateToken(70).then((refreshed: boolean) => {
            if (refreshed) {
              console.log('Token refreshed');
            }
          }).catch(() => {
            console.error('Failed to refresh token');
          });
        }, 60000);
      }

      return this.authenticated;
    } catch (error) {
      console.error('Failed to initialize Keycloak', error);
      return false;
    }
  }

  isAuthenticated(): boolean {
    return this.authenticated;
  }

  getToken(): string | undefined {
    return this.keycloak.token;
  }

  getUserRoles(): string[] {
    return this.keycloak.tokenParsed?.['roles'] || [];
  }

  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  isUser(): boolean {
    return this.hasRole('ZANT_USER');
  }

  isController(): boolean {
    return this.hasRole('ZANT_CONTROLLER');
  }

  getUserInfo(): Record<string, any> {
    const t = this.keycloak.tokenParsed as any;

    return {
      username: t?.['preferred_username'],
      email: t?.['email'],
      firstName: t?.['given_name'],
      lastName: t?.['family_name'],
      roles: this.getUserRoles(),
    };
  }


  login(): void {
    this.keycloak.login();
  }

  logout(): void {
    this.keycloak.logout({
      redirectUri: window.location.origin
    });
  }

  getKeycloakInstance(): Keycloak {
    return this.keycloak;
  }
}
