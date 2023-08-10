/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockTask } from '../../helpers/mocks/tasks';
import { GitHubConfigurationStatus, GitHubProvisioningStatus } from '../../types/provisioning';
import { Task, TaskStatuses, TaskTypes } from '../../types/tasks';
import {
  activateGithubProvisioning,
  activateScim,
  checkConfigurationValidity,
  deactivateGithubProvisioning,
  deactivateScim,
  fetchGithubProvisioningStatus,
  fetchIsScimEnabled,
} from '../provisioning';

jest.mock('../provisioning');

const defaultConfigurationStatus: GitHubConfigurationStatus = {
  application: {
    jit: {
      status: GitHubProvisioningStatus.Success,
    },
    autoProvisioning: {
      status: GitHubProvisioningStatus.Success,
    },
  },
  installations: [
    {
      organization: 'testOrg',
      autoProvisioning: {
        status: GitHubProvisioningStatus.Success,
      },
      jit: {
        status: GitHubProvisioningStatus.Success,
      },
    },
  ],
};

export default class AuthenticationServiceMock {
  scimStatus: boolean;
  githubProvisioningStatus: boolean;
  githubConfigurationStatus: GitHubConfigurationStatus;
  tasks: Task[];

  constructor() {
    this.scimStatus = false;
    this.githubProvisioningStatus = false;
    this.githubConfigurationStatus = cloneDeep(defaultConfigurationStatus);
    this.tasks = [];
    jest.mocked(activateScim).mockImplementation(this.handleActivateScim);
    jest.mocked(deactivateScim).mockImplementation(this.handleDeactivateScim);
    jest.mocked(fetchIsScimEnabled).mockImplementation(this.handleFetchIsScimEnabled);
    jest
      .mocked(activateGithubProvisioning)
      .mockImplementation(this.handleActivateGithubProvisioning);
    jest
      .mocked(deactivateGithubProvisioning)
      .mockImplementation(this.handleDeactivateGithubProvisioning);
    jest
      .mocked(fetchGithubProvisioningStatus)
      .mockImplementation(this.handleFetchGithubProvisioningStatus);
    jest
      .mocked(checkConfigurationValidity)
      .mockImplementation(this.handleCheckConfigurationValidity);
  }

  addProvisioningTask = (overrides: Partial<Omit<Task, 'type'>> = {}) => {
    this.tasks.push(
      mockTask({
        id: Math.random().toString(),
        type: TaskTypes.GithubProvisioning,
        ...overrides,
      })
    );
  };

  setConfigurationValidity = (overrides: Partial<GitHubConfigurationStatus> = {}) => {
    this.githubConfigurationStatus = {
      ...this.githubConfigurationStatus,
      ...overrides,
    };
  };

  handleActivateScim = () => {
    this.scimStatus = true;
    return Promise.resolve();
  };

  handleDeactivateScim = () => {
    this.scimStatus = false;
    return Promise.resolve();
  };

  handleFetchIsScimEnabled = () => {
    return Promise.resolve(this.scimStatus);
  };

  handleActivateGithubProvisioning = () => {
    this.githubProvisioningStatus = true;
    return Promise.resolve();
  };

  handleDeactivateGithubProvisioning = () => {
    this.githubProvisioningStatus = false;
    return Promise.resolve();
  };

  handleFetchGithubProvisioningStatus = () => {
    if (!this.githubProvisioningStatus) {
      return Promise.resolve({ enabled: false });
    }

    const nextSync = this.tasks.find((t: any) =>
      [TaskStatuses.InProgress, TaskStatuses.Pending].includes(t.status)
    );
    const lastSync = this.tasks.find(
      (t: any) => ![TaskStatuses.InProgress, TaskStatuses.Pending].includes(t.status)
    );

    return Promise.resolve({
      enabled: true,
      nextSync: nextSync ? { status: nextSync.status } : undefined,
      lastSync: lastSync
        ? {
            status: lastSync.status,
            finishedAt: lastSync.executedAt,
            startedAt: lastSync.startedAt,
            executionTimeMs: lastSync.executionTimeMs,
            summary: lastSync.status === TaskStatuses.Success ? 'Test summary' : undefined,
            errorMessage: lastSync.errorMessage,
          }
        : undefined,
    });
  };

  handleCheckConfigurationValidity = () => {
    return Promise.resolve(this.githubConfigurationStatus);
  };

  reset = () => {
    this.scimStatus = false;
    this.githubProvisioningStatus = false;
    this.githubConfigurationStatus = cloneDeep(defaultConfigurationStatus);
    this.tasks = [];
  };
}
