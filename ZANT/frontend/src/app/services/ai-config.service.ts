import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AiProvider {
  provider: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiConfigService {
  private apiUrl = 'http://localhost:8080/api/ai-config';
  private currentProviderSubject = new BehaviorSubject<string>('pllum');
  public currentProvider$ = this.currentProviderSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadCurrentProvider();
  }

  loadCurrentProvider(): void {
    this.http.get<AiProvider>(`${this.apiUrl}/provider`).subscribe({
      next: (response) => {
        this.currentProviderSubject.next(response.provider);
      },
      error: (error) => {
        console.error('Error loading AI provider:', error);
      }
    });
  }

  getCurrentProvider(): Observable<AiProvider> {
    return this.http.get<AiProvider>(`${this.apiUrl}/provider`).pipe(
      tap(response => this.currentProviderSubject.next(response.provider))
    );
  }

  setProvider(provider: string): Observable<AiProvider> {
    return this.http.post<AiProvider>(`${this.apiUrl}/provider`, { provider }).pipe(
      tap(response => this.currentProviderSubject.next(response.provider))
    );
  }

  get currentProviderValue(): string {
    return this.currentProviderSubject.value;
  }
}
