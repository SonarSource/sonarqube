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

import { cloneDeep } from 'lodash';
import {
  checkSuggestionServiceStatus,
  FixParam,
  getFixSuggestionsIssues,
  getSuggestions,
  SuggestionServiceStatus,
  SuggestionServiceStatusCheckResponse,
  updateFeatureEnablement,
  UpdateFeatureEnablementParams,
} from '../fix-suggestions';
import { ISSUE_101, ISSUE_1101 } from './data/ids';

jest.mock('../fix-suggestions');

export type MockSuggestionServiceStatus = SuggestionServiceStatus | 'WTF' | undefined;

export default class FixSuggestionsServiceMock {
  fixSuggestion = {
    id: '70b14d4c-d302-4979-9121-06ac7d563c5c',
    issueId: 'AYsVhClEbjXItrbcN71J',
    explanation:
      "Replaced 'require' statements with 'import' statements to comply with ECMAScript 2015 module management standards.",
    changes: [
      {
        startLine: 6,
        endLine: 7,
        newCode: "import { glob } from 'glob';\nimport fs from 'fs';",
      },
    ],
  };

  serviceStatus: MockSuggestionServiceStatus = 'SUCCESS';

  constructor() {
    jest.mocked(getSuggestions).mockImplementation(this.handleGetFixSuggestion);
    jest.mocked(getFixSuggestionsIssues).mockImplementation(this.handleGetFixSuggestionsIssues);
    jest.mocked(checkSuggestionServiceStatus).mockImplementation(this.handleCheckService);
    jest.mocked(updateFeatureEnablement).mockImplementation(this.handleUpdateFeatureEnablement);
  }

  handleGetFixSuggestionsIssues = (data: FixParam) => {
    if (data.issueId === ISSUE_1101) {
      return this.reply({ aiSuggestion: 'NOT_AVAILABLE_FILE_LEVEL_ISSUE', id: 'id1' } as const);
    }
    return this.reply({ aiSuggestion: 'AVAILABLE', id: 'id1' } as const);
  };

  handleGetFixSuggestion = (data: FixParam) => {
    if (data.issueId === ISSUE_101) {
      return Promise.reject({ error: { msg: 'Invalid issue' } });
    }
    return this.reply(this.fixSuggestion);
  };

  handleCheckService = () => {
    if (this.serviceStatus) {
      return this.reply({ status: this.serviceStatus } as SuggestionServiceStatusCheckResponse);
    }
    return Promise.reject({ error: { msg: 'Error' } });
  };

  handleUpdateFeatureEnablement = (_: UpdateFeatureEnablementParams) => {
    return Promise.resolve();
  };

  reply<T>(response: T): Promise<T> {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(cloneDeep(response));
      }, 10);
    });
  }

  setServiceStatus(status: MockSuggestionServiceStatus) {
    this.serviceStatus = status;
  }
}
