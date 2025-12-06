import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EWYPReport } from '../models/ewyp-report';

@Injectable({
  providedIn: 'root'
})
export class EWYPReportService {
  private apiUrl = 'http://localhost:8080/api/ewyp-reports';

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
}
