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

  getReportById(id: number): Observable<EWYPReport> {
    return this.http.get<EWYPReport>(`${this.apiUrl}/${id}`);
  }
}
