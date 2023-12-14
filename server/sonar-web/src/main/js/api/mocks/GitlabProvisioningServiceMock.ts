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
import { mockPaging } from '../../helpers/testMocks';
import { GitlabConfiguration } from '../../types/provisioning';
import {
  createGitLabConfiguration,
  deleteGitLabConfiguration,
  fetchGitLabConfiguration,
  fetchGitLabConfigurations,
  updateGitLabConfiguration,
} from '../gitlab-provisioning';

jest.mock('../gitlab-provisioning');

const defaultGitlabConfiguration: GitlabConfiguration[] = [
  mockGitlabConfiguration({ id: '1', enabled: true }),
];

export default class GitlabProvisioningServiceMock {
  gitlabConfigurations: GitlabConfiguration[];

  constructor() {
    this.gitlabConfigurations = cloneDeep(defaultGitlabConfiguration);
    jest.mocked(fetchGitLabConfigurations).mockImplementation(this.handleFetchGitLabConfigurations);
    jest.mocked(fetchGitLabConfiguration).mockImplementation(this.handleFetchGitLabConfiguration);
    jest.mocked(createGitLabConfiguration).mockImplementation(this.handleCreateGitLabConfiguration);
    jest.mocked(updateGitLabConfiguration).mockImplementation(this.handleUpdateGitLabConfiguration);
    jest.mocked(deleteGitLabConfiguration).mockImplementation(this.handleDeleteGitLabConfiguration);
  }

  handleFetchGitLabConfigurations: typeof fetchGitLabConfigurations = () => {
    return Promise.resolve({
      gitlabConfigurations: this.gitlabConfigurations,
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
    this.gitlabConfigurations = cloneDeep(defaultGitlabConfiguration);
  };
}
