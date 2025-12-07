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
  error: string;
}

@Injectable({
  providedIn: 'root'
})
export class EWYPReportService {
  private apiUrl = 'http://localhost:8081/api/ewyp-reports';
  private assistantUrl = 'http://localhost:8081/api/assistant';

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

  uploadAttachment(reportId: string, file: File): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment`, formData);
  }

  downloadAttachment(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/attachment`, {
      responseType: 'blob'
    });
  }

  deleteAttachment(reportId: string): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment`);
  }

  // Hospital Card Copy
  uploadHospitalCardCopy(reportId: string, file: File): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/hospital-card`, formData);
  }

  downloadHospitalCardCopy(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/attachment/hospital-card`, {
      responseType: 'blob'
    });
  }

  deleteHospitalCardCopy(reportId: string): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/hospital-card`);
  }

  // Prosecutor Decision Copy
  uploadProsecutorDecisionCopy(reportId: string, file: File): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/prosecutor-decision`, formData);
  }

  downloadProsecutorDecisionCopy(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/attachment/prosecutor-decision`, {
      responseType: 'blob'
    });
  }

  deleteProsecutorDecisionCopy(reportId: string): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/prosecutor-decision`);
  }

  // Power of Attorney Copy
  uploadPowerOfAttorneyCopy(reportId: string, file: File): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/power-of-attorney`, formData);
  }

  downloadPowerOfAttorneyCopy(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/attachment/power-of-attorney`, {
      responseType: 'blob'
    });
  }

  deletePowerOfAttorneyCopy(reportId: string): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/power-of-attorney`);
  }

  // Death Docs Copy
  uploadDeathDocsCopy(reportId: string, file: File): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/death-docs`, formData);
  }

  downloadDeathDocsCopy(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/attachment/death-docs`, {
      responseType: 'blob'
    });
  }

  deleteDeathDocsCopy(reportId: string): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/death-docs`);
  }

  // Other Documents
  uploadOtherDocument(reportId: string, file: File, documentName: string): Observable<EWYPReport> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentName', documentName);
    return this.http.post<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/other-document`, formData);
  }

  deleteOtherDocument(reportId: string, index: number): Observable<EWYPReport> {
    return this.http.delete<EWYPReport>(`${this.apiUrl}/${reportId}/attachment/other-document/${index}`);
  }

  // Document Generation
  generateDocument(reportId: string, format: 'docx' | 'pdf'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/generate-document`, {
      params: { format },
      responseType: 'blob'
    });
  }
}
