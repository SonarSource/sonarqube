/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { axiosToCatch } from '../helpers/request';
import { SuggestedFix } from '../types/fix-suggestions';

export interface FixParam {
  issueId: string;
}

export interface AiIssue {
  aiSuggestion: 'AVAILABLE' | 'NOT_AVAILABLE_FILE_LEVEL_ISSUE' | 'NOT_AVAILABLE_UNSUPPORTED_RULE';
  id: string;
}

export function getSuggestions(data: FixParam): Promise<SuggestedFix> {
  return axiosToCatch.post<SuggestedFix>('/api/v2/fix-suggestions/ai-suggestions', data);
}

export function getFixSuggestionsIssues(data: FixParam): Promise<AiIssue> {
  return axiosToCatch.get(`/api/v2/fix-suggestions/issues/${data.issueId}`);
}
