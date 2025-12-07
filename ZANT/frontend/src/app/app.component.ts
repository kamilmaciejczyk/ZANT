import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ChatComponent } from './components/chat/chat.component';
import { EwypFormComponent } from './components/ewyp-form/ewyp-form.component';
import { CommonModule } from '@angular/common';
import { ContrastService } from './services/contrast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ChatComponent, EwypFormComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'ZANT Frontend';
  showEwypForm = false;
  isHighContrast = false;

  constructor(private contrastService: ContrastService) {
    this.contrastService.highContrast$.subscribe(
      highContrast => this.isHighContrast = highContrast
    );
  }

  toggleView(): void {
    this.showEwypForm = !this.showEwypForm;
  }

  toggleContrast(): void {
    this.contrastService.toggleContrast();
  }
}
