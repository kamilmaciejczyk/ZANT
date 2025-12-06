import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EWYPReport } from '../models/ewyp-report';

export interface CircumstancesQuestion {
  id: number;
  text: string;
}

export interface CircumstancesAssistantResponse {
  questionsCount: number;
  questions: CircumstancesQuestion[];
}

@Injectable({
  providedIn: 'root'
})
export class EWYPReportService {
  private apiUrl = 'http://localhost:8080/api/ewyp-reports';
  private assistantUrl = 'http://localhost:8080/api/assistant';

  constructor(private http: HttpClient) { }

  submitReport(report: EWYPReport): Observable<EWYPReport> {
    return this.http.post<EWYPReport>(this.apiUrl, report);
  }

  saveDraft(report: EWYPReport): Observable<EWYPReport> {
    if (report.id) {
      // Update existing draft
      return this.http.put<EWYPReport>(`${this.apiUrl}/${report.id}`, report);
    } else {
      // Create new draft
      return this.http.post<EWYPReport>(`${this.apiUrl}/draft`, report);
    }
  }

  getReportById(id: string): Observable<EWYPReport> {
    return this.http.get<EWYPReport>(`${this.apiUrl}/${id}`);
  }

  getAllReports(search?: string, status?: string): Observable<EWYPReport[]> {
    let params: any = {};
    if (search) {
      params.search = search;
    }
    if (status) {
      params.status = status;
    }
    return this.http.get<EWYPReport[]>(this.apiUrl, { params });
  }

  generateCircumstancesQuestions(accidentDescription: string): Observable<CircumstancesAssistantResponse> {
    return this.http.post<CircumstancesAssistantResponse>(
      `${this.assistantUrl}/circumstances`,
      { accidentDescription }
    );
  }
}
