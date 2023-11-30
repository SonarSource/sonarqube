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
import { cloneDeep, omit } from 'lodash';
import { mockGitlabConfiguration } from '../../helpers/mocks/alm-integrations';
import { mockTask } from '../../helpers/mocks/tasks';
import { mockPaging } from '../../helpers/testMocks';
import {
  GitHubConfigurationStatus,
  GitHubMapping,
  GitHubProvisioningStatus,
  GitlabConfiguration,
} from '../../types/provisioning';
import { Task, TaskStatuses, TaskTypes } from '../../types/tasks';
import {
  activateGithubProvisioning,
  activateScim,
  addGithubRolesMapping,
  checkConfigurationValidity,
  createGitLabConfiguration,
  deactivateGithubProvisioning,
  deactivateScim,
  deleteGitLabConfiguration,
  deleteGithubRolesMapping,
  fetchGitLabConfiguration,
  fetchGitLabConfigurations,
  fetchGithubProvisioningStatus,
  fetchGithubRolesMapping,
  fetchIsScimEnabled,
  updateGitLabConfiguration,
  updateGithubRolesMapping,
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

const defaultGitlabConfiguration: GitlabConfiguration[] = [
  mockGitlabConfiguration({ id: '1', enabled: true }),
];

const githubMappingMock = (
  id: string,
  permissions: (keyof GitHubMapping['permissions'])[],
  isBaseRole = false,
) => ({
  id,
  githubRole: id,
  isBaseRole,
  permissions: {
    user: permissions.includes('user'),
    codeViewer: permissions.includes('codeViewer'),
    issueAdmin: permissions.includes('issueAdmin'),
    securityHotspotAdmin: permissions.includes('securityHotspotAdmin'),
    admin: permissions.includes('admin'),
    scan: permissions.includes('scan'),
  },
});

const defaultMapping: GitHubMapping[] = [
  githubMappingMock('read', ['user', 'codeViewer'], true),
  githubMappingMock(
    'write',
    ['user', 'codeViewer', 'issueAdmin', 'securityHotspotAdmin', 'scan'],
    true,
  ),
  githubMappingMock('triage', ['user', 'codeViewer'], true),
  githubMappingMock(
    'maintain',
    ['user', 'codeViewer', 'issueAdmin', 'securityHotspotAdmin', 'scan'],
    true,
  ),
  githubMappingMock(
    'admin',
    ['user', 'codeViewer', 'issueAdmin', 'securityHotspotAdmin', 'admin', 'scan'],
    true,
  ),
];

export default class AuthenticationServiceMock {
  scimStatus: boolean;
  githubProvisioningStatus: boolean;
  githubConfigurationStatus: GitHubConfigurationStatus;
  githubMapping: GitHubMapping[];
  tasks: Task[];
  gitlabConfigurations: GitlabConfiguration[];

  constructor() {
    this.scimStatus = false;
    this.githubProvisioningStatus = false;
    this.githubConfigurationStatus = cloneDeep(defaultConfigurationStatus);
    this.githubMapping = cloneDeep(defaultMapping);
    this.tasks = [];
    this.gitlabConfigurations = cloneDeep(defaultGitlabConfiguration);
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
    jest.mocked(fetchGithubRolesMapping).mockImplementation(this.handleFetchGithubRolesMapping);
    jest.mocked(updateGithubRolesMapping).mockImplementation(this.handleUpdateGithubRolesMapping);
    jest.mocked(addGithubRolesMapping).mockImplementation(this.handleAddGithubRolesMapping);
    jest.mocked(deleteGithubRolesMapping).mockImplementation(this.handleDeleteGithubRolesMapping);
    jest.mocked(fetchGitLabConfigurations).mockImplementation(this.handleFetchGitLabConfigurations);
    jest.mocked(fetchGitLabConfiguration).mockImplementation(this.handleFetchGitLabConfiguration);
    jest.mocked(createGitLabConfiguration).mockImplementation(this.handleCreateGitLabConfiguration);
    jest.mocked(updateGitLabConfiguration).mockImplementation(this.handleUpdateGitLabConfiguration);
    jest.mocked(deleteGitLabConfiguration).mockImplementation(this.handleDeleteGitLabConfiguration);
  }

  addProvisioningTask = (overrides: Partial<Omit<Task, 'type'>> = {}) => {
    this.tasks.push(
      mockTask({
        id: Math.random().toString(),
        type: TaskTypes.GithubProvisioning,
        ...overrides,
      }),
    );
  };

  setConfigurationValidity = (overrides: Partial<GitHubConfigurationStatus> = {}) => {
    this.githubConfigurationStatus = {
      ...this.githubConfigurationStatus,
      ...overrides,
    };
  };

  enableGithubProvisioning = () => {
    this.scimStatus = false;
    this.githubProvisioningStatus = true;
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

    const nextSync = this.tasks.find((t: Task) =>
      [TaskStatuses.InProgress, TaskStatuses.Pending].includes(t.status),
    );
    const lastSync = this.tasks.find(
      (t: Task) => ![TaskStatuses.InProgress, TaskStatuses.Pending].includes(t.status),
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
            warningMessage: lastSync.warnings?.join() ?? undefined,
          }
        : undefined,
    });
  };

  handleCheckConfigurationValidity = () => {
    return Promise.resolve(this.githubConfigurationStatus);
  };

  handleFetchGithubRolesMapping: typeof fetchGithubRolesMapping = () => {
    return Promise.resolve(this.githubMapping);
  };

  handleUpdateGithubRolesMapping: typeof updateGithubRolesMapping = (id, data) => {
    this.githubMapping = this.githubMapping.map((mapping) =>
      mapping.id === id ? { ...mapping, ...data } : mapping,
    );

    return Promise.resolve(
      this.githubMapping.find((mapping) => mapping.id === id) as GitHubMapping,
    );
  };

  handleAddGithubRolesMapping: typeof addGithubRolesMapping = (data) => {
    const newRole = { ...data, id: data.githubRole };
    this.githubMapping = [...this.githubMapping, newRole];

    return Promise.resolve(newRole);
  };

  handleDeleteGithubRolesMapping: typeof deleteGithubRolesMapping = (id) => {
    this.githubMapping = this.githubMapping.filter((el) => el.id !== id);
    return Promise.resolve();
  };

  addGitHubCustomRole = (id: string, permissions: (keyof GitHubMapping['permissions'])[]) => {
    this.githubMapping = [...this.githubMapping, githubMappingMock(id, permissions)];
  };

  handleFetchGitLabConfigurations: typeof fetchGitLabConfigurations = () => {
    return Promise.resolve({
      configurations: this.gitlabConfigurations,
      page: mockPaging({ total: this.gitlabConfigurations.length }),
    });
  };

  handleFetchGitLabConfiguration: typeof fetchGitLabConfiguration = (id: string) => {
    const configuration = this.gitlabConfigurations.find((c) => c.id === id);
    if (!configuration) {
      return Promise.reject();
    }
    return Promise.resolve(configuration);
  };

  handleCreateGitLabConfiguration: typeof createGitLabConfiguration = (data) => {
    const newConfig = mockGitlabConfiguration({
      ...omit(data, 'applicationId', 'clientSecret'),
      id: '1',
      enabled: true,
    });
    this.gitlabConfigurations = [...this.gitlabConfigurations, newConfig];
    return Promise.resolve(newConfig);
  };

  handleUpdateGitLabConfiguration: typeof updateGitLabConfiguration = (id, data) => {
    const index = this.gitlabConfigurations.findIndex((c) => c.id === id);
    this.gitlabConfigurations[index] = { ...this.gitlabConfigurations[index], ...data };
    return Promise.resolve(this.gitlabConfigurations[index]);
  };

  handleDeleteGitLabConfiguration: typeof deleteGitLabConfiguration = (id) => {
    this.gitlabConfigurations = this.gitlabConfigurations.filter((c) => c.id !== id);
    return Promise.resolve();
  };

  setGitlabConfigurations = (gitlabConfigurations: GitlabConfiguration[]) => {
    this.gitlabConfigurations = gitlabConfigurations;
  };

  reset = () => {
    this.scimStatus = false;
    this.githubProvisioningStatus = false;
    this.githubConfigurationStatus = cloneDeep(defaultConfigurationStatus);
    this.githubMapping = cloneDeep(defaultMapping);
    this.tasks = [];
    this.gitlabConfigurations = cloneDeep(defaultGitlabConfiguration);
  };
}
