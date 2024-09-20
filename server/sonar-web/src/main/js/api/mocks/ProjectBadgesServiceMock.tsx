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
import { isProjectAiCodeAssured } from '../ai-code-assurance';
import { getProjectBadgesToken, renewProjectBadgesToken } from '../project-badges';

jest.mock('../project-badges');
jest.mock('../project-badges');
jest.mock('../ai-code-assurance');

const defaultToken = 'sqb_2b5052cef8eac91a921ac71be9227a27f6b6b38b';

export class ProjectBadgesServiceMock {
  token: string;

  constructor() {
    this.token = defaultToken;

    jest.mocked(getProjectBadgesToken).mockImplementation(this.handleGetProjectBadgesToken);
    jest.mocked(renewProjectBadgesToken).mockImplementation(this.handleRenewProjectBadgesToken);
    jest.mocked(isProjectAiCodeAssured).mockImplementation(this.handleProjectAiGeneratedCode);
  }

  handleGetProjectBadgesToken = () => {
    return Promise.resolve(this.token);
  };

  handleProjectAiGeneratedCode = (project: string) => {
    if (project === 'no-ai') {
      return Promise.resolve(false);
    }
    return Promise.resolve(true);
  };

  handleRenewProjectBadgesToken = () => {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    this.token =
      'sqb-' +
      new Array(40)
        .fill(null)
        .map(() => chars.charAt(Math.floor(Math.random() * chars.length)))
        .join('');

    return Promise.resolve(this.token);
  };

  reset = () => {
    this.token = defaultToken;
  };
}
