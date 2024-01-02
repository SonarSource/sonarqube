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
import { TaskStatuses, TaskTypes } from '../../types/tasks';
import { doExport, doImport, getStatus } from '../project-dump';
import ComputeEngineServiceMock from './ComputeEngineServiceMock';

jest.mock('../project-dump');

export class ProjectDumpServiceMock {
  #ceService: ComputeEngineServiceMock;
  canBeExported = true;
  canBeImported = false;
  #customTasks = false;
  exportedDump: string | undefined = undefined;
  dumpToImport: string | undefined = undefined;

  constructor(ceService: ComputeEngineServiceMock) {
    this.#ceService = ceService;

    jest.mocked(doExport).mockImplementation(this.doExportHandler);
    jest.mocked(doImport).mockImplementation(this.doImportHandler);
    jest.mocked(getStatus).mockImplementation(this.getStatusHandler);
  }

  reset = () => {
    this.canBeImported = false;
    this.canBeExported = true;
    this.#customTasks = false;
    this.exportedDump = undefined;
    this.dumpToImport = undefined;
  };

  setImportState = () => {
    this.canBeImported = true;
    this.canBeExported = false;
    this.dumpToImport = 'tmp/test.zip';
  };

  useCustomTasks = () => {
    this.#customTasks = true;
  };

  doExportHandler = (componentKey: string) => {
    if (!this.#customTasks) {
      this.#ceService.addTask({
        componentKey,
        type: TaskTypes.ProjectExport,
        status: TaskStatuses.Success,
        executedAt: '2023-06-08T12:00:00Z',
      });
    }
    this.exportedDump = `/tmp/${componentKey}.zip`;
    return this.reply({});
  };

  doImportHandler = (componentKey: string) => {
    if (!this.#customTasks) {
      this.#ceService.addTask({
        componentKey,
        type: TaskTypes.ProjectImport,
        status: TaskStatuses.Success,
        executedAt: '2023-06-08T12:00:00Z',
      });
    }
    return this.reply({});
  };

  getStatusHandler = () => {
    return this.reply({
      canBeExported: this.canBeExported,
      canBeImported: this.canBeImported,
      exportedDump: this.exportedDump,
      dumpToImport: this.dumpToImport,
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
