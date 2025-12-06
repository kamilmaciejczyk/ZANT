export interface AssistantTurn {
  response: string;
  followUpQuestions: string[];
  missingFields: string[];
  completionProgress: number;
}
