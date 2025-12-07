import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PdfService {
  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  downloadNotice(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/${reportId}/pdf?type=NOTICE`, {
      responseType: 'blob'
    });
  }

  downloadExplanation(reportId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reports/${reportId}/pdf?type=EXPLANATION`, {
      responseType: 'blob'
    });
  }
}
