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
import { Visibility } from '../../types/component';
import { getApplicationDetails, getApplicationLeak } from '../application';

jest.mock('../application');

export default class ApplicationServiceMock {
  constructor() {
    jest.mocked(getApplicationLeak).mockImplementation(this.handleGetApplicationLeak);
    jest.mocked(getApplicationDetails).mockImplementation(this.handleGetApplicationDetails);
  }

  handleGetApplicationLeak = () => {
    return this.reply([
      {
        project: 'org.sonarsource.scanner.cli:sonar-scanner-cli',
        projectName: 'SonarScanner CLI',
        date: '2022-12-23T11:02:26+0100',
      },
      {
        project: 'org.sonarsource.scanner.maven:sonar-maven-plugin',
        projectName: 'SonarQube Scanner for Maven',
        date: '2021-11-09T13:59:13+0100',
      },
    ]);
  };

  handleGetApplicationDetails = () => {
    return this.reply({
      branches: [],
      key: 'key-1',
      name: 'app',
      projects: [
        {
          branch: 'foo',
          key: 'KEY-P1',
          name: 'P1',
          isMain: true,
        },
      ],
      visibility: Visibility.Private,
    });
  };

  reset = () => {};

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
