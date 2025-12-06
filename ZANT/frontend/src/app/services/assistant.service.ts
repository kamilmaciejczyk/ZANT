import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssistantTurn } from '../models/assistant-turn';

@Injectable({
  providedIn: 'root'
})
export class AssistantService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  sendMessage(conversationId: string, userMessage: string): Observable<AssistantTurn> {
    return this.http.post<AssistantTurn>(
      `${this.apiUrl}/assistant/${conversationId}/message`,
      { message: userMessage }
    );
  }
}
