export interface AssistantTurn {
  botMessage: string;
  followUpQuestions: string[];
  missingFields: string[];
  completionPercentage: number;
}
