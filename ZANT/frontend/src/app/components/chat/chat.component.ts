import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { AssistantService } from '../../services/assistant.service';
import { PdfService } from '../../services/pdf.service';
import { AssistantTurn } from '../../models/assistant-turn';

interface ChatMessage {
  text: string;
  isBot: boolean;
  timestamp: Date;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressBarModule,
    MatListModule,
    MatChipsModule
  ],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit {
  messages: ChatMessage[] = [];
  userMessage = '';
  conversationId = 'conv-' + Date.now();
  missingFields: string[] = [];
  completionPercentage = 0;
  followUpQuestions: string[] = [];
  isLoading = false;
  reportId: string | null = null;

  constructor(
    private assistantService: AssistantService,
    private pdfService: PdfService
  ) {}

  ngOnInit(): void {
    this.addBotMessage('Witaj! Jestem wirtualnym asystentem ZANT ZUS. Pomogę Ci zgłosić wypadek przy pracy. Opowiedz mi, co się wydarzyło.');
  }

  sendMessage(): void {
    if (!this.userMessage.trim() || this.isLoading) return;

    const message = this.userMessage.trim();
    this.addUserMessage(message);
    this.userMessage = '';
    this.isLoading = true;

    this.assistantService.sendMessage(this.conversationId, message).subscribe({
      next: (turn: AssistantTurn) => {
        this.addBotMessage(turn.botMessage);
        this.missingFields = turn.missingFields;
        this.completionPercentage = turn.completionPercentage;
        this.followUpQuestions = turn.followUpQuestions;
        this.isLoading = false;

        if (this.completionPercentage === 100) {
          this.reportId = this.conversationId;
        }
      },
      error: (err) => {
        console.error('Error:', err);
        this.addBotMessage('Przepraszam, wystąpił błąd. Spróbuj ponownie.');
        this.isLoading = false;
      }
    });
  }

  selectQuestion(question: string): void {
    this.userMessage = question;
    this.sendMessage();
  }

  downloadNotice(): void {
    if (!this.reportId) return;

    this.pdfService.downloadNotice(this.reportId).subscribe({
      next: (blob) => {
        this.downloadFile(blob, 'zawiadomienie_o_wypadku.pdf');
      },
      error: (err) => console.error('Error downloading notice:', err)
    });
  }

  downloadExplanation(): void {
    if (!this.reportId) return;

    this.pdfService.downloadExplanation(this.reportId).subscribe({
      next: (blob) => {
        this.downloadFile(blob, 'wyjasnienia_poszkodowanego.pdf');
      },
      error: (err) => console.error('Error downloading explanation:', err)
    });
  }

  private addUserMessage(text: string): void {
    this.messages.push({
      text,
      isBot: false,
      timestamp: new Date()
    });
  }

  private addBotMessage(text: string): void {
    this.messages.push({
      text,
      isBot: true,
      timestamp: new Date()
    });
  }

  private downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
}
