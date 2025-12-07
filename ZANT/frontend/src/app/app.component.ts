import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ChatComponent } from './components/chat/chat.component';
import { EwypFormComponent } from './components/ewyp-form/ewyp-form.component';
import { CommonModule } from '@angular/common';
import { ContrastService } from './services/contrast.service';
import { AiConfigService } from './services/ai-config.service';

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
  showSettingsMenu = false;
  currentProvider = 'pllum';

  constructor(
    private contrastService: ContrastService,
    private aiConfigService: AiConfigService
  ) {
    this.contrastService.highContrast$.subscribe(
      highContrast => this.isHighContrast = highContrast
    );

    this.aiConfigService.currentProvider$.subscribe(
      provider => this.currentProvider = provider
    );
  }

  toggleView(): void {
    this.showEwypForm = !this.showEwypForm;
  }

  toggleContrast(): void {
    this.contrastService.toggleContrast();
  }

  toggleSettingsMenu(): void {
    this.showSettingsMenu = !this.showSettingsMenu;
  }

  closeSettingsMenu(): void {
    this.showSettingsMenu = false;
  }

  selectProvider(provider: string): void {
    this.aiConfigService.setProvider(provider).subscribe({
      next: (response) => {
        console.log('AI provider changed to:', response.provider);
      },
      error: (error) => {
        console.error('Error changing AI provider:', error);
      }
    });
  }
}
