import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EWYPReportService, CircumstancesQuestion } from '../../services/ewyp-report.service';
import { EWYPReport } from '../../models/ewyp-report';

@Component({
  selector: 'app-ewyp-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './ewyp-form.component.html',
  styleUrls: ['./ewyp-form.component.scss']
})
export class EwypFormComponent implements OnInit {
  reportForm!: FormGroup;
  currentStep = 0;
  totalSteps = 7;
  submittedReportId: string | null = null;
  savedDraftId: string | null = null;
  isSubmitting = false;
  isSavingDraft = false;
  errorMessage: string | null = null;
  draftSaveMessage: string | null = null;

  // NIP/REGON search properties
  nipRegonSearch: string = '';
  isSearching = false;
  searchError: string | null = null;
  companyData: any = null;

  // Circumstances AI Assistant properties
  circumstancesQuestions: CircumstancesQuestion[] = [];
  circumstancesQuestionsError: string | null = null;
  isLoadingQuestions = false;
  showCircumstancesAssistant = false;

  // File upload properties
  selectedFile: File | null = null;
  uploadedFileName: string | null = null;
  isUploadingFile = false;
  fileUploadError: string | null = null;

  // Document generation properties
  isGeneratingDocument = false;
  showFormatSelection = false;
  selectedFormat: 'docx' | 'pdf' = 'pdf';

  // Document-specific file upload properties
  selectedHospitalCardFile: File | null = null;
  isUploadingHospitalCard = false;
  hospitalCardUploadError: string | null = null;

  selectedProsecutorDecisionFile: File | null = null;
  isUploadingProsecutorDecision = false;
  prosecutorDecisionUploadError: string | null = null;

  selectedPowerOfAttorneyFile: File | null = null;
  isUploadingPowerOfAttorney = false;
  powerOfAttorneyUploadError: string | null = null;

  selectedDeathDocsFile: File | null = null;
  isUploadingDeathDocs = false;
  deathDocsUploadError: string | null = null;

  selectedOtherDocFile: File | null = null;
  otherDocumentName: string = '';
  isUploadingOtherDoc = false;
  otherDocUploadError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private reportService: EWYPReportService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.setupConditionalValidation();

    // Check if there's a draft ID in the route
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.loadDraft(id);
        // Restore current step from localStorage for this draft
        const savedStep = localStorage.getItem(`ewyp-form-step-${id}`);
        if (savedStep) {
          this.currentStep = parseInt(savedStep, 10);
        }
      }
    });
  }

  setupConditionalValidation(): void {
    // Krok 1 - Opis wypadku: conditionally enable/disable fields based on checkboxes
    const attachmentsGroup = this.reportForm.get('attachments') as FormGroup;

    // First aid conditional validation
    this.reportForm.get('accidentInfo.firstAidGiven')?.valueChanges.subscribe(value => {
      const firstAidFacilityControl = this.reportForm.get('accidentInfo.firstAidFacility');
      const investigatingAuthorityControl = this.reportForm.get('accidentInfo.investigatingAuthority');

      if (value === true) {
        firstAidFacilityControl?.setValidators([Validators.required]);
        investigatingAuthorityControl?.setValidators([Validators.required]);
        firstAidFacilityControl?.enable();
        investigatingAuthorityControl?.enable();
      } else {
        firstAidFacilityControl?.clearValidators();
        investigatingAuthorityControl?.clearValidators();
        firstAidFacilityControl?.disable();
        investigatingAuthorityControl?.disable();
      }
      firstAidFacilityControl?.updateValueAndValidity();
      investigatingAuthorityControl?.updateValueAndValidity();
    });

    // Machine operation conditional validation
    this.reportForm.get('accidentInfo.accidentDuringMachineOperation')?.valueChanges.subscribe(value => {
      const machineConditionControl = this.reportForm.get('accidentInfo.machineConditionDescription');

      if (value === true) {
        machineConditionControl?.setValidators([Validators.required]);
        machineConditionControl?.enable();
      } else {
        machineConditionControl?.clearValidators();
        machineConditionControl?.disable();
      }
      machineConditionControl?.updateValueAndValidity();
    });

    // Krok 3 - Zgłaszający: conditionally enable/disable all reporter fields
    this.reportForm.get('reporter.isDifferentFromInjuredPerson')?.valueChanges.subscribe(value => {
      const reporterGroup = this.reportForm.get('reporter') as FormGroup;
      const powerOfAttorneyControl = attachmentsGroup?.get('hasPowerOfAttorneyCopy');
      const powerOfAttorneyFilenameControl = attachmentsGroup?.get('powerOfAttorneyCopyFilename');

      if (value === true) {
        // Enable and require all reporter fields except the checkbox itself
        reporterGroup.get('firstName')?.setValidators([Validators.required]);
        reporterGroup.get('lastName')?.setValidators([Validators.required]);
        reporterGroup.get('pesel')?.setValidators([Validators.required, Validators.pattern(/^\d{11}$/)]);
        reporterGroup.get('idDocumentType')?.setValidators([Validators.required]);
        reporterGroup.get('idDocumentNumber')?.setValidators([Validators.required]);
        reporterGroup.get('birthDate')?.setValidators([Validators.required]);
        reporterGroup.get('phoneNumber')?.setValidators([Validators.required]);

        // Enable all controls
        Object.keys(reporterGroup.controls).forEach(key => {
          if (key !== 'isDifferentFromInjuredPerson') {
            reporterGroup.get(key)?.enable();
          }
        });

        // Require pełnomocnictwo attachment
        powerOfAttorneyControl?.setValidators([Validators.requiredTrue]);
        powerOfAttorneyControl?.setValue(true, { emitEvent: false });
        powerOfAttorneyFilenameControl?.setValidators([Validators.required]);
      } else {
        // Disable and remove validators
        Object.keys(reporterGroup.controls).forEach(key => {
          if (key !== 'isDifferentFromInjuredPerson') {
            reporterGroup.get(key)?.clearValidators();
            reporterGroup.get(key)?.disable();
          }
        });

        powerOfAttorneyControl?.clearValidators();
        powerOfAttorneyControl?.setValue(false, { emitEvent: false });
        powerOfAttorneyFilenameControl?.clearValidators();
      }

      // Update validity of all controls
      Object.keys(reporterGroup.controls).forEach(key => {
        if (key !== 'isDifferentFromInjuredPerson') {
          reporterGroup.get(key)?.updateValueAndValidity();
        }
      });
      powerOfAttorneyControl?.updateValueAndValidity();
      powerOfAttorneyFilenameControl?.updateValueAndValidity();
    });

    attachmentsGroup.get('hasPowerOfAttorneyCopy')?.valueChanges.subscribe(flag => {
      if (this.reportForm.get('reporter.isDifferentFromInjuredPerson')?.value && flag !== true) {
        attachmentsGroup.get('hasPowerOfAttorneyCopy')?.setValue(true, { emitEvent: false });
      }
    });

    // Trigger initial state
    this.reportForm.get('accidentInfo.firstAidGiven')?.setValue(false);
    this.reportForm.get('accidentInfo.accidentDuringMachineOperation')?.setValue(false);
    this.reportForm.get('reporter.isDifferentFromInjuredPerson')?.setValue(false);
  }

  searchCompanyData(): void {
    if (!this.nipRegonSearch || this.nipRegonSearch.trim().length === 0) {
      this.searchError = 'Proszę wprowadzić NIP lub REGON';
      return;
    }

    this.isSearching = true;
    this.searchError = null;
    this.companyData = null;

    // Simulate API call - replace with actual API call later
    setTimeout(() => {
      this.isSearching = false;

      // Mock company data
      this.companyData = {
        name: 'Jan Kowalski - Usługi Remontowo-Budowlane',
        nip: '1234567890',
        regon: '123456789',
        pkd: '43.99.Z - Pozostałe specjalistyczne roboty budowlane',
        address: 'ul. Budowlana 15, 00-001 Warszawa'
      };
    }, 1000);
  }

  continueWithCompanyData(): void {
    if (this.companyData) {
      // Pre-fill form with company data
      // This can be expanded based on what data is available
      this.nextStep();
    }
  }

  loadDraft(id: string): void {
    this.reportService.getReportById(id).subscribe({
      next: (report) => {
        this.savedDraftId = report.id || null;
        this.reportForm.patchValue(report);
        this.enforcePowerOfAttorneyRequirement();

        // Load uploaded file name if exists
        if (report.attachmentFilename) {
          this.uploadedFileName = report.attachmentFilename;
        }

        // Load witnesses if any
        if (report.witnesses && report.witnesses.length > 0) {
          report.witnesses.forEach(witness => {
            const witnessGroup = this.fb.group({
              firstName: [witness.firstName],
              lastName: [witness.lastName],
              street: [witness.street],
              houseNumber: [witness.houseNumber],
              apartmentNumber: [witness.apartmentNumber],
              postalCode: [witness.postalCode],
              city: [witness.city],
              country: [witness.country]
            });
            this.witnesses.push(witnessGroup);
          });
        }
      },
      error: (error) => {
        this.errorMessage = 'Nie udało się załadować wersji roboczej.';
        console.error('Error loading draft:', error);
      }
    });
  }

  initializeForm(): void {
    this.reportForm = this.fb.group({
      injuredPerson: this.fb.group({
        pesel: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
        idDocumentType: ['', Validators.required],
        idDocumentNumber: ['', Validators.required],
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        birthDate: ['', Validators.required],
        birthPlace: ['', Validators.required],
        phoneNumber: ['', Validators.required],
        address: this.fb.group({
          street: ['', Validators.required],
          houseNumber: ['', Validators.required],
          apartmentNumber: [''], // NOT required - może być dom
          postalCode: ['', Validators.required],
          city: ['', Validators.required],
          country: ['', Validators.required]
        }),
        lastPolishAddressOrStay: this.fb.group({
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: ['']
        }),
        correspondenceAddress: this.fb.group({
          mode: [''],
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: [''],
          country: ['']
        }),
        nonAgriculturalBusinessAddress: this.fb.group({
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: [''],
          phoneNumber: ['']
        }),
        childCareAddress: this.fb.group({
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: [''],
          phoneNumber: ['']
        })
      }),
      reporter: this.fb.group({
        isDifferentFromInjuredPerson: [false],
        pesel: [{value: '', disabled: true}],
        idDocumentType: [{value: '', disabled: true}],
        idDocumentNumber: [{value: '', disabled: true}],
        firstName: [{value: '', disabled: true}],
        lastName: [{value: '', disabled: true}],
        birthDate: [{value: '', disabled: true}],
        phoneNumber: [{value: '', disabled: true}],
        address: this.fb.group({
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: [''],
          country: ['']
        }),
        lastPolishAddressOrStay: this.fb.group({
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: ['']
        }),
        correspondenceAddress: this.fb.group({
          mode: [''],
          street: [''],
          houseNumber: [''],
          apartmentNumber: [''],
          postalCode: [''],
          city: [''],
          country: ['']
        })
      }),
      accidentInfo: this.fb.group({
        accidentDate: ['', Validators.required],
        accidentTime: ['', Validators.required],
        plannedWorkStartTime: ['', Validators.required],
        plannedWorkEndTime: ['', Validators.required],
        placeOfAccident: ['', Validators.required],
        injuriesDescription: ['', Validators.required],
        circumstancesAndCauses: ['', Validators.required],
        firstAidGiven: [false],
        firstAidFacility: [{value: '', disabled: true}], // Conditional
        investigatingAuthority: [{value: '', disabled: true}], // Conditional
        accidentDuringMachineOperation: [false],
        machineConditionDescription: [{value: '', disabled: true}], // Conditional
        machineHasCertificate: [null],
        machineInFixedAssetsRegister: [null]
      }),
      witnesses: this.fb.array([]),
      attachments: this.fb.group({
        hasHospitalCardCopy: [false],
        hospitalCardCopyFilename: [''],
        hasProsecutorDecisionCopy: [false],
        prosecutorDecisionCopyFilename: [''],
        hasPowerOfAttorneyCopy: [false],
        powerOfAttorneyCopyFilename: [''],
        hasDeathDocsCopy: [false],
        deathDocsCopyFilename: [''],
        hasOtherDocuments: [false],
        otherDocuments: [[]]
      }),
      documentsToDeliverLater: this.fb.group({
        toDate: [''],
        documents: [[]]
      }),
      responseDeliveryMethod: ['', Validators.required],
      signature: this.fb.group({
        declarationDate: ['', Validators.required],
        signatureName: ['', Validators.required]
      })
    });
  }

  get witnesses(): FormArray {
    return this.reportForm.get('witnesses') as FormArray;
  }

  addWitness(): void {
    const witnessGroup = this.fb.group({
      firstName: [''],
      lastName: [''],
      street: [''],
      houseNumber: [''],
      apartmentNumber: [''],
      postalCode: [''],
      city: [''],
      country: ['']
    });
    this.witnesses.push(witnessGroup);
  }

  removeWitness(index: number): void {
    this.witnesses.removeAt(index);
  }

  nextStep(): void {
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
      // Save current step to localStorage if we have a draft ID
      if (this.savedDraftId) {
        localStorage.setItem(`ewyp-form-step-${this.savedDraftId}`, this.currentStep.toString());
      }
      // Scroll to top
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
      // Save current step to localStorage if we have a draft ID
      if (this.savedDraftId) {
        localStorage.setItem(`ewyp-form-step-${this.savedDraftId}`, this.currentStep.toString());
      }
      // Scroll to top
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  onSubmit(): void {
    if (this.reportForm.valid) {
      this.isSubmitting = true;
      this.errorMessage = null;

      const report: EWYPReport = {
        ...this.reportForm.value,
        id: this.savedDraftId || undefined
      };

      console.log(report);
      this.reportService.submitReport(report).subscribe({
        next: (response) => {
          this.isSubmitting = false;
          this.submittedReportId = response.id || null;

          // Redirect to the URL with UUID
          if (response.id) {
            this.router.navigate(['/ewyp-form', response.id]).then(() => {
              alert(`Zgłoszenie zapisane pomyślnie! ID zgłoszenia: ${response.id}`);
            });
          } else {
            alert(`Zgłoszenie zapisane pomyślnie!`);
          }
        },
        error: (error) => {
          this.isSubmitting = false;
          this.errorMessage = 'Nie udało się zapisać zgłoszenia. Spróbuj ponownie.';
          console.error('Error submitting report:', error);
        }
      });
    } else {
      alert('Proszę wypełnić wszystkie wymagane pola');
    }
  }

  saveDraft(): void {
    this.isSavingDraft = true;
    this.draftSaveMessage = null;
    this.errorMessage = null;

    const report: EWYPReport = {
      ...this.reportForm.value,
      id: this.savedDraftId || undefined
    };

    console.log(report);

    this.reportService.saveDraft(report).subscribe({
      next: (response) => {
        this.isSavingDraft = false;
        this.savedDraftId = response.id || null;

        // Always redirect to the URL with UUID after saving
        if (response.id) {
          this.router.navigate(['/ewyp-form', response.id]).then(() => {
            this.draftSaveMessage = `Wersja robocza zapisana pomyślnie! ID: ${response.id}`;

            // Clear the message after 3 seconds
            setTimeout(() => {
              this.draftSaveMessage = null;
            }, 3000);
          });
        } else {
          this.draftSaveMessage = `Wersja robocza zapisana pomyślnie!`;

          // Clear the message after 3 seconds
          setTimeout(() => {
            this.draftSaveMessage = null;
          }, 3000);
        }
      },
      error: (error) => {
        this.isSavingDraft = false;
        this.errorMessage = 'Nie udało się zapisać wersji roboczej. Spróbuj ponownie.';
        console.error('Error saving draft:', error);

        // Clear the error message after 5 seconds
        setTimeout(() => {
          this.errorMessage = null;
        }, 5000);
      }
    });
  }

  getStepTitle(): string {
    switch (this.currentStep) {
      case 0: return 'Identyfikacja';
      case 1: return 'Opis wypadku';
      case 2: return 'Dane osoby poszkodowanej';
      case 3: return 'Dane osoby zgłaszającej';
      case 4: return 'Świadkowie';
      case 5: return 'Dokumenty i załączniki';
      case 6: return 'Walidacja';
      case 7: return 'Podpis i wysłanie';
      default: return '';
    }
  }

  getStepSubtitle(): string {
    switch (this.currentStep) {
      case 0: return 'Dane firmy';
      case 1: return 'Okoliczności';
      case 2: return 'Informacje';
      case 3: return 'Dane zgłaszającego';
      case 4: return 'Informacje o świadkach';
      case 5: return 'Załączniki';
      case 6: return 'Weryfikacja';
      case 7: return 'Pobierz PDF';
      default: return '';
    }
  }

  getStepTitleForProgress(step: number): string {
    switch (step) {
      case 1: return 'Identyfikacja';
      case 2: return 'Opis wypadku';
      case 3: return 'Osoba poszkodowana';
      case 4: return 'Zgłaszający';
      case 5: return 'Świadkowie';
      case 6: return 'Dokumenty';
      case 7: return 'Podsumowanie';
      default: return '';
    }
  }

  getStepSubtitleForProgress(step: number): string {
    switch (step) {
      case 1: return 'Dane firmy';
      case 2: return 'Okoliczności';
      case 3: return 'Dane poszkodowanego';
      case 4: return 'Dane zgłaszającego';
      case 5: return 'Dane świadków';
      case 6: return 'Załączniki';
      case 7: return 'Weryfikacja';
      default: return '';
    }
  }

  // Circumstances AI Assistant method
  checkCircumstancesDescription(): void {
    const circumstancesControl = this.reportForm.get('accidentInfo.circumstancesAndCauses');
    const description = circumstancesControl?.value;

    // if (!description || description.trim().length < 50) {
    //   // Description too short, don't call AI
    //   this.circumstancesQuestions = [];
    //   this.showCircumstancesAssistant = true;
    //   return;
    // }

    this.isLoadingQuestions = true;
    this.showCircumstancesAssistant = true;

    this.reportService.generateCircumstancesQuestions(description).subscribe({
      next: (response) => {
        this.isLoadingQuestions = false;
        this.circumstancesQuestions = response.questions;
        this.circumstancesQuestionsError = response.error;
      },
      error: (error) => {
        this.isLoadingQuestions = false;
        console.error('Error generating circumstances questions:', error);
        this.circumstancesQuestions = [];
      }
    });
  }

  // File upload methods
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (file.type !== 'application/pdf') {
        this.fileUploadError = 'Tylko pliki PDF są dozwolone';
        this.selectedFile = null;
        return;
      }

      // Validate file size (max 10MB)
      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        this.fileUploadError = 'Plik jest za duży (maksymalnie 10MB)';
        this.selectedFile = null;
        return;
      }

      this.selectedFile = file;
      this.fileUploadError = null;
    }
  }

  uploadFile(): void {
    if (!this.selectedFile) {
      this.fileUploadError = 'Proszę wybrać plik';
      return;
    }

    if (!this.savedDraftId) {
      this.fileUploadError = 'Najpierw zapisz wersję roboczą wniosku';
      return;
    }

    this.isUploadingFile = true;
    this.fileUploadError = null;

    this.reportService.uploadAttachment(this.savedDraftId, this.selectedFile).subscribe({
      next: (response) => {
        this.isUploadingFile = false;
        this.uploadedFileName = response.attachmentFilename || null;
        this.selectedFile = null;
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingFile = false;
        this.fileUploadError = 'Nie udało się przesłać pliku. Spróbuj ponownie.';
        console.error('Error uploading file:', error);
      }
    });
  }

  downloadFile(): void {
    if (!this.savedDraftId) {
      return;
    }

    this.reportService.downloadAttachment(this.savedDraftId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.uploadedFileName || 'attachment.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        alert('Nie udało się pobrać pliku.');
        console.error('Error downloading file:', error);
      }
    });
  }

  deleteFile(): void {
    if (!this.savedDraftId) {
      return;
    }

    if (!confirm('Czy na pewno chcesz usunąć załączony plik?')) {
      return;
    }

    this.reportService.deleteAttachment(this.savedDraftId).subscribe({
      next: () => {
        this.uploadedFileName = null;
        this.selectedFile = null;
        alert('Plik został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć pliku.');
        console.error('Error deleting file:', error);
      }
    });
  }

  // Hospital Card Copy methods
  onHospitalCardFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.validatePDFFile(file)) {
      this.selectedHospitalCardFile = file;
      this.hospitalCardUploadError = null;
    } else if (file) {
      this.hospitalCardUploadError = 'Nieprawidłowy plik';
      this.selectedHospitalCardFile = null;
    }
  }

  uploadHospitalCardFile(): void {
    if (!this.selectedHospitalCardFile || !this.savedDraftId) return;

    this.isUploadingHospitalCard = true;
    this.hospitalCardUploadError = null;

    this.reportService.uploadHospitalCardCopy(this.savedDraftId, this.selectedHospitalCardFile).subscribe({
      next: (response) => {
        this.isUploadingHospitalCard = false;
        this.reportForm.patchValue({ attachments: response.attachments });
        this.selectedHospitalCardFile = null;
        this.saveDraft();
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingHospitalCard = false;
        this.hospitalCardUploadError = 'Nie udało się przesłać pliku.';
        console.error('Error uploading hospital card file:', error);
      }
    });
  }

  deleteHospitalCardFile(): void {
    if (!this.savedDraftId || !confirm('Czy na pewno chcesz usunąć plik?')) return;

    this.reportService.deleteHospitalCardCopy(this.savedDraftId).subscribe({
      next: (response) => {
        this.reportForm.patchValue({ attachments: response.attachments });
        this.saveDraft();
        alert('Plik został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć pliku.');
        console.error('Error deleting hospital card file:', error);
      }
    });
  }

  downloadHospitalCardFile(): void {
    if (!this.savedDraftId) return;

    this.reportService.downloadHospitalCardCopy(this.savedDraftId).subscribe({
      next: (blob) => {
        const filename = this.reportForm.get('attachments.hospitalCardCopyFilename')?.value || 'hospital-card.pdf';
        this.downloadBlob(blob, filename);
      },
      error: (error) => {
        alert('Nie udało się pobrać pliku.');
        console.error('Error downloading hospital card file:', error);
      }
    });
  }

  // Prosecutor Decision Copy methods
  onProsecutorDecisionFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.validatePDFFile(file)) {
      this.selectedProsecutorDecisionFile = file;
      this.prosecutorDecisionUploadError = null;
    } else if (file) {
      this.prosecutorDecisionUploadError = 'Nieprawidłowy plik';
      this.selectedProsecutorDecisionFile = null;
    }
  }

  uploadProsecutorDecisionFile(): void {
    if (!this.selectedProsecutorDecisionFile || !this.savedDraftId) return;

    this.isUploadingProsecutorDecision = true;
    this.prosecutorDecisionUploadError = null;

    this.reportService.uploadProsecutorDecisionCopy(this.savedDraftId, this.selectedProsecutorDecisionFile).subscribe({
      next: (response) => {
        this.isUploadingProsecutorDecision = false;
        this.reportForm.patchValue({ attachments: response.attachments });
        this.selectedProsecutorDecisionFile = null;
        this.saveDraft();
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingProsecutorDecision = false;
        this.prosecutorDecisionUploadError = 'Nie udało się przesłać pliku.';
        console.error('Error uploading prosecutor decision file:', error);
      }
    });
  }

  deleteProsecutorDecisionFile(): void {
    if (!this.savedDraftId || !confirm('Czy na pewno chcesz usunąć plik?')) return;

    this.reportService.deleteProsecutorDecisionCopy(this.savedDraftId).subscribe({
      next: (response) => {
        this.reportForm.patchValue({ attachments: response.attachments });
        alert('Plik został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć pliku.');
        console.error('Error deleting prosecutor decision file:', error);
      }
    });
  }

  downloadProsecutorDecisionFile(): void {
    if (!this.savedDraftId) return;

    this.reportService.downloadProsecutorDecisionCopy(this.savedDraftId).subscribe({
      next: (blob) => {
        const filename = this.reportForm.get('attachments.prosecutorDecisionCopyFilename')?.value || 'prosecutor-decision.pdf';
        this.downloadBlob(blob, filename);
      },
      error: (error) => {
        alert('Nie udało się pobrać pliku.');
        console.error('Error downloading prosecutor decision file:', error);
      }
    });
  }

  // Power of Attorney Copy methods
  onPowerOfAttorneyFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.validatePDFFile(file)) {
      this.selectedPowerOfAttorneyFile = file;
      this.powerOfAttorneyUploadError = null;
    } else if (file) {
      this.powerOfAttorneyUploadError = 'Nieprawidłowy plik';
      this.selectedPowerOfAttorneyFile = null;
    }
  }

  uploadPowerOfAttorneyFile(): void {
    if (!this.selectedPowerOfAttorneyFile || !this.savedDraftId) return;

    this.isUploadingPowerOfAttorney = true;
    this.powerOfAttorneyUploadError = null;

    this.reportService.uploadPowerOfAttorneyCopy(this.savedDraftId, this.selectedPowerOfAttorneyFile).subscribe({
      next: (response) => {
        this.isUploadingPowerOfAttorney = false;
        this.reportForm.patchValue({ attachments: response.attachments });
        this.enforcePowerOfAttorneyRequirement();
        this.selectedPowerOfAttorneyFile = null;
        this.saveDraft();
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingPowerOfAttorney = false;
        this.powerOfAttorneyUploadError = 'Nie udało się przesłać pliku.';
        console.error('Error uploading power of attorney file:', error);
      }
    });
  }

  deletePowerOfAttorneyFile(): void {
    if (!this.savedDraftId || !confirm('Czy na pewno chcesz usunąć plik?')) return;

    this.reportService.deletePowerOfAttorneyCopy(this.savedDraftId).subscribe({
      next: (response) => {
        this.reportForm.patchValue({ attachments: response.attachments });
        this.enforcePowerOfAttorneyRequirement();
        alert('Plik został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć pliku.');
        console.error('Error deleting power of attorney file:', error);
      }
    });
  }

  downloadPowerOfAttorneyFile(): void {
    if (!this.savedDraftId) return;

    this.reportService.downloadPowerOfAttorneyCopy(this.savedDraftId).subscribe({
      next: (blob) => {
        const filename = this.reportForm.get('attachments.powerOfAttorneyCopyFilename')?.value || 'pelnomocnictwo.pdf';
        this.downloadBlob(blob, filename);
      },
      error: (error) => {
        alert('Nie udało się pobrać pliku.');
        console.error('Error downloading power of attorney file:', error);
      }
    });
  }

  // Death Docs Copy methods
  onDeathDocsFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.validatePDFFile(file)) {
      this.selectedDeathDocsFile = file;
      this.deathDocsUploadError = null;
    } else if (file) {
      this.deathDocsUploadError = 'Nieprawidłowy plik';
      this.selectedDeathDocsFile = null;
    }
  }

  uploadDeathDocsFile(): void {
    if (!this.selectedDeathDocsFile || !this.savedDraftId) return;

    this.isUploadingDeathDocs = true;
    this.deathDocsUploadError = null;

    this.reportService.uploadDeathDocsCopy(this.savedDraftId, this.selectedDeathDocsFile).subscribe({
      next: (response) => {
        this.isUploadingDeathDocs = false;
        this.reportForm.patchValue({ attachments: response.attachments });
        this.selectedDeathDocsFile = null;
        this.saveDraft();
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingDeathDocs = false;
        this.deathDocsUploadError = 'Nie udało się przesłać pliku.';
        console.error('Error uploading death docs file:', error);
      }
    });
  }

  deleteDeathDocsFile(): void {
    if (!this.savedDraftId || !confirm('Czy na pewno chcesz usunąć plik?')) return;

    this.reportService.deleteDeathDocsCopy(this.savedDraftId).subscribe({
      next: (response) => {
        this.reportForm.patchValue({ attachments: response.attachments });
        alert('Plik został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć pliku.');
        console.error('Error deleting death docs file:', error);
      }
    });
  }

  downloadDeathDocsFile(): void {
    if (!this.savedDraftId) return;

    this.reportService.downloadDeathDocsCopy(this.savedDraftId).subscribe({
      next: (blob) => {
        const filename = this.reportForm.get('attachments.deathDocsCopyFilename')?.value || 'death-docs.pdf';
        this.downloadBlob(blob, filename);
      },
      error: (error) => {
        alert('Nie udało się pobrać pliku.');
        console.error('Error downloading death docs file:', error);
      }
    });
  }

  // Other Documents methods
  onOtherDocFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.validatePDFFile(file)) {
      this.selectedOtherDocFile = file;
      this.otherDocUploadError = null;
    } else if (file) {
      this.otherDocUploadError = 'Nieprawidłowy plik';
      this.selectedOtherDocFile = null;
    }
  }

  uploadOtherDocFile(): void {
    if (!this.selectedOtherDocFile || !this.savedDraftId || !this.otherDocumentName.trim()) {
      this.otherDocUploadError = 'Proszę wybrać plik i podać nazwę dokumentu';
      return;
    }

    this.isUploadingOtherDoc = true;
    this.otherDocUploadError = null;

    this.reportService.uploadOtherDocument(this.savedDraftId, this.selectedOtherDocFile, this.otherDocumentName).subscribe({
      next: (response) => {
        this.isUploadingOtherDoc = false;
        this.reportForm.patchValue({ attachments: response.attachments });
        this.selectedOtherDocFile = null;
        this.otherDocumentName = '';
        alert('Plik został przesłany pomyślnie!');
      },
      error: (error) => {
        this.isUploadingOtherDoc = false;
        this.otherDocUploadError = 'Nie udało się przesłać pliku.';
        console.error('Error uploading other document file:', error);
      }
    });
  }

  deleteOtherDocFile(index: number): void {
    if (!this.savedDraftId || !confirm('Czy na pewno chcesz usunąć ten dokument?')) return;

    this.reportService.deleteOtherDocument(this.savedDraftId, index).subscribe({
      next: (response) => {
        this.reportForm.patchValue({ attachments: response.attachments });
        alert('Dokument został usunięty.');
      },
      error: (error) => {
        alert('Nie udało się usunąć dokumentu.');
        console.error('Error deleting other document:', error);
      }
    });
  }

  get otherDocuments(): any[] {
    return this.reportForm.get('attachments.otherDocuments')?.value || [];
  }

  // Document generation methods
  toggleFormatSelection(): void {
    this.showFormatSelection = !this.showFormatSelection;
  }

  generateAndDownloadDocument(format: 'docx' | 'pdf'): void {
    if (!this.savedDraftId) {
      alert('Najpierw zapisz wersję roboczą zgłoszenia');
      return;
    }

    this.isGeneratingDocument = true;
    this.showFormatSelection = false;

    this.reportService.generateDocument(this.savedDraftId, format).subscribe({
      next: (blob) => {
        this.isGeneratingDocument = false;
        const extension = format === 'docx' ? 'docx' : 'pdf';
        const filename = `zawiadomienie_wypadku_${this.savedDraftId}.${extension}`;
        this.downloadBlob(blob, filename);
      },
      error: (error) => {
        this.isGeneratingDocument = false;
        alert('Nie udało się wygenerować dokumentu. Spróbuj ponownie.');
        console.error('Error generating document:', error);
      }
    });
  }

  // Helper methods
  private validatePDFFile(file: File): boolean {
    if (file.type !== 'application/pdf') {
      return false;
    }
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      return false;
    }
    return true;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  private enforcePowerOfAttorneyRequirement(): void {
    const isDifferent = this.reportForm.get('reporter.isDifferentFromInjuredPerson')?.value;
    const poaControl = this.reportForm.get('attachments.hasPowerOfAttorneyCopy');
    const poaFilenameControl = this.reportForm.get('attachments.powerOfAttorneyCopyFilename');
    if (isDifferent) {
      poaControl?.setValue(true, { emitEvent: false });
    }
    poaControl?.updateValueAndValidity();
    poaFilenameControl?.updateValueAndValidity();
  }
}
