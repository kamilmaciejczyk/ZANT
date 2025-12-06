import { Component } from '@angular/core';
import { ChatComponent } from './components/chat/chat.component';
import { EwypFormComponent } from './components/ewyp-form/ewyp-form.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatComponent, EwypFormComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'ZANT Frontend';
  showEwypForm = false;

  toggleView(): void {
    this.showEwypForm = !this.showEwypForm;
  }
}
