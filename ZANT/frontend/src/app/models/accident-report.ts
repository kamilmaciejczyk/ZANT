import { PersonData } from './person-data';
import { BusinessData } from './business-data';
import { AccidentData } from './accident-data';
import { Witness } from './witness';

export interface AccidentReport {
  id?: string;
  personData?: PersonData;
  businessData?: BusinessData;
  accidentData?: AccidentData;
  witnesses?: Witness[];
  proxyData?: PersonData;
  isFiledByProxy?: boolean;
  requiredDocuments?: string[];
}
