import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EWYPReportService } from '../../services/ewyp-report.service';
import { EWYPReport } from '../../models/ewyp-report';

@Component({
  selector: 'app-ewyp-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ewyp-search.component.html',
  styleUrls: ['./ewyp-search.component.scss']
})
export class EwypSearchComponent implements OnInit {
  reports: EWYPReport[] = [];
  filteredReports: EWYPReport[] = [];
  searchTerm: string = '';
  statusFilter: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(
    private ewypReportService: EWYPReportService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.ewypReportService.getAllReports(this.searchTerm, this.statusFilter).subscribe({
      next: (reports) => {
        this.reports = reports;
        this.filteredReports = reports;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading reports:', error);
        this.errorMessage = 'Błąd podczas ładowania wniosków';
        this.isLoading = false;
      }
    });
  }

  onSearch(): void {
    this.loadReports();
  }

  onStatusFilterChange(): void {
    this.loadReports();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.statusFilter = '';
    this.loadReports();
  }

  viewReport(id: string): void {
    this.router.navigate(['/ewyp-form', id]);
  }

  createNewReport(): void {
    this.router.navigate(['/ewyp-form']);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'Szkic';
      case 'SUBMITTED':
        return 'Zgłoszony';
      default:
        return status || 'Nieznany';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'status-draft';
      case 'SUBMITTED':
        return 'status-submitted';
      default:
        return 'status-unknown';
    }
  }

  formatDate(dateString?: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('pl-PL');
  }
}
