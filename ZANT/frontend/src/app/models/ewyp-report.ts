export interface EWYPReport {
  id?: string;
  injuredPerson: InjuredPerson;
  reporter: Reporter;
  accidentInfo: AccidentInfo;
  witnesses: WitnessInfo[];
  attachments: Attachments;
  documentsToDeliverLater: DocumentsToDeliverLater;
  responseDeliveryMethod: string;
  signature: Signature;
  status?: string;
}

export interface InjuredPerson {
  pesel: string;
  idDocumentType: string;
  idDocumentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  birthPlace: string;
  phoneNumber: string;
  address: Address;
  lastPolishAddressOrStay: PolishAddress;
  correspondenceAddress: CorrespondenceAddress;
  nonAgriculturalBusinessAddress: NonAgriculturalBusinessAddress;
  childCareAddress: ChildCareAddress;
}

export interface Reporter {
  isDifferentFromInjuredPerson: boolean;
  pesel: string;
  idDocumentType: string;
  idDocumentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  phoneNumber: string;
  address: Address;
  lastPolishAddressOrStay: PolishAddress;
  correspondenceAddress: CorrespondenceAddress;
}

export interface Address {
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface PolishAddress {
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
}

export interface CorrespondenceAddress {
  mode: string;
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface NonAgriculturalBusinessAddress {
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
  phoneNumber: string;
}

export interface ChildCareAddress {
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
  phoneNumber: string;
}

export interface AccidentInfo {
  accidentDate: string;
  accidentTime: string;
  plannedWorkStartTime: string;
  plannedWorkEndTime: string;
  placeOfAccident: string;
  injuriesDescription: string;
  circumstancesAndCauses: string;
  firstAidGiven: boolean;
  firstAidFacility: string;
  investigatingAuthority: string;
  accidentDuringMachineOperation: boolean;
  machineConditionDescription: string;
  machineHasCertificate: boolean | null;
  machineInFixedAssetsRegister: boolean | null;
}

export interface WitnessInfo {
  firstName: string;
  lastName: string;
  street: string;
  houseNumber: string;
  apartmentNumber: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface Attachments {
  hasHospitalCardCopy: boolean;
  hasProsecutorDecisionCopy: boolean;
  hasDeathDocsCopy: boolean;
  hasOtherDocuments: boolean;
  otherDocuments: string[];
}

export interface DocumentsToDeliverLater {
  toDate: string;
  documents: string[];
}

export interface Signature {
  declarationDate: string;
  signatureName: string;
}
