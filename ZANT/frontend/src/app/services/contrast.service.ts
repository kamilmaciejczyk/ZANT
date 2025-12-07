import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ContrastService {
  private highContrastKey = 'zant-high-contrast';
  private highContrastSubject = new BehaviorSubject<boolean>(this.getInitialState());
  public highContrast$ = this.highContrastSubject.asObservable();

  constructor() {
    this.applyContrast(this.highContrastSubject.value);
  }

  private getInitialState(): boolean {
    const stored = localStorage.getItem(this.highContrastKey);
    return stored === 'true';
  }

  toggleContrast(): void {
    const newValue = !this.highContrastSubject.value;
    this.highContrastSubject.next(newValue);
    localStorage.setItem(this.highContrastKey, String(newValue));
    this.applyContrast(newValue);
  }

  private applyContrast(highContrast: boolean): void {
    if (highContrast) {
      document.body.classList.add('high-contrast');
    } else {
      document.body.classList.remove('high-contrast');
    }
  }

  isHighContrast(): boolean {
    return this.highContrastSubject.value;
  }
}
