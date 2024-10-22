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

import { cloneDeep, omit } from 'lodash';
import { mockGitHubConfiguration } from '../../helpers/mocks/dop-translation';
import { mockPaging } from '../../helpers/testMocks';
import { AlmKeys } from '../../types/alm-settings';
import {
  DopSetting,
  GitHubConfigurationResponse,
  ProjectBinding,
} from '../../types/dop-translation';
import {
  createBoundProject,
  createGitHubConfiguration,
  deleteGitHubConfiguration,
  fetchGitHubConfiguration,
  getDopSettings,
  getProjectBindings,
  searchGitHubConfigurations,
  updateGitHubConfiguration,
} from '../dop-translation';
import { mockDopSetting, mockProjectBinding } from './data/dop-translation';

jest.mock('../dop-translation');

const defaultDopSettings = [
  mockDopSetting({ key: 'conf-final-1', type: AlmKeys.GitLab }),
  mockDopSetting({ key: 'conf-final-2', type: AlmKeys.GitLab }),
  mockDopSetting({ key: 'conf-github-1', type: AlmKeys.GitHub, url: 'http://url' }),
  mockDopSetting({ key: 'conf-github-2', type: AlmKeys.GitHub, url: 'http://url' }),
  mockDopSetting({ key: 'conf-github-3', type: AlmKeys.GitHub, url: 'javascript://url' }),
  mockDopSetting({ key: 'conf-azure-1', type: AlmKeys.Azure, url: 'url' }),
  mockDopSetting({ key: 'conf-azure-2', type: AlmKeys.Azure, url: 'url' }),
  mockDopSetting({
    key: 'conf-bitbucketcloud-1',
    type: AlmKeys.BitbucketCloud,
    url: 'url',
  }),
  mockDopSetting({
    key: 'conf-bitbucketcloud-2',
    type: AlmKeys.BitbucketCloud,
    url: 'url',
  }),
  mockDopSetting({
    key: 'conf-bitbucketserver-1',
    type: AlmKeys.BitbucketServer,
    url: 'url',
  }),
  mockDopSetting({
    key: 'conf-bitbucketserver-2',
    type: AlmKeys.BitbucketServer,
    url: 'url',
  }),
  mockDopSetting(),
  mockDopSetting({ id: 'dop-setting-test-id-2', key: 'Test/DopSetting2' }),
];
const defaultProjectBindings = [
  mockProjectBinding({
    dopSetting: 'conf-github-1',
    id: 'project-binding-1',
    projectId: 'key123',
    projectKey: 'key123',
    repository: 'Github repo 1',
    slug: 'Slug/Repository-1',
  }),
];

export default class DopTranslationServiceMock {
  projectBindings: ProjectBinding[] = [];
  dopSettings: DopSetting[] = [];
  gitHubConfigurations: GitHubConfigurationResponse[] = [];

  constructor() {
    this.reset();
    jest.mocked(createBoundProject).mockImplementation(this.createBoundProject);
    jest.mocked(getDopSettings).mockImplementation(this.getDopSettings);
    jest.mocked(getProjectBindings).mockImplementation(this.getProjectBindings);
    jest
      .mocked(searchGitHubConfigurations)
      .mockImplementation(this.handleSearchGitHubConfigurations);
    jest.mocked(fetchGitHubConfiguration).mockImplementation(this.handleFetchGitHubConfiguration);
    jest.mocked(createGitHubConfiguration).mockImplementation(this.handleCreateGitHubConfiguration);
    jest.mocked(updateGitHubConfiguration).mockImplementation(this.handleUpdateGitHubConfiguration);
    jest.mocked(deleteGitHubConfiguration).mockImplementation(this.handleDeleteGitHubConfiguration);
  }

  /*
   * Project bindings
   */
  createBoundProject: typeof createBoundProject = (data) => {
    this.projectBindings.push(
      mockProjectBinding({
        dopSetting: data.devOpsPlatformSettingId,
        id: `${data.devOpsPlatformSettingId}-${data.repositoryIdentifier}-${data.projectKey}`,
        projectId: data.projectKey,
        repository: data.repositoryIdentifier,
      }),
    );
    return Promise.resolve({});
  };

  getDopSettings = () => {
    const total = this.getDopSettings.length;
    return Promise.resolve({
      dopSettings: this.dopSettings,
      page: mockPaging({ pageSize: total, total }),
    });
  };

  getProjectBindings: typeof getProjectBindings = (params) => {
    const pageIndex = params.pageIndex ?? 1;
    const pageSize = params.pageSize ?? 50;

    return this.reply({
      page: {
        pageIndex,
        pageSize,
        total: this.projectBindings.length,
      },
      projectBindings: this.projectBindings.slice((pageIndex - 1) * pageSize, pageIndex * pageSize),
    });
  };

  removeDopTypeFromSettings = (type: AlmKeys) => {
    this.dopSettings = cloneDeep(defaultDopSettings).filter(
      (dopSetting) => dopSetting.type !== type,
    );
  };

  /*
   * GitHub configurations
   */
  handleSearchGitHubConfigurations: typeof searchGitHubConfigurations = () => {
    return Promise.resolve({
      githubConfigurations: this.gitHubConfigurations,
      page: mockPaging({ total: this.gitHubConfigurations.length }),
    });
  };

  handleFetchGitHubConfiguration: typeof fetchGitHubConfiguration = (id: string) => {
    const configuration = this.gitHubConfigurations.find((c) => c.id === id);
    if (!configuration) {
      return Promise.reject();
    }
    return Promise.resolve(configuration);
  };

  handleCreateGitHubConfiguration: typeof createGitHubConfiguration = (gitHubConfiguration) => {
    const newConfig = mockGitHubConfiguration({
      ...omit(gitHubConfiguration, 'clientId', 'clientSecret', 'privateKey'),
      id: '1',
      enabled: false,
    });
    this.gitHubConfigurations = [...this.gitHubConfigurations, newConfig];
    return Promise.resolve(newConfig);
  };

  handleUpdateGitHubConfiguration: typeof updateGitHubConfiguration = (id, gitHubConfiguration) => {
    const index = this.gitHubConfigurations.findIndex((c) => c.id === id);
    this.gitHubConfigurations[index] = {
      ...this.gitHubConfigurations[index],
      ...gitHubConfiguration,
    };
    return Promise.resolve(this.gitHubConfigurations[index]);
  };

  handleDeleteGitHubConfiguration: typeof deleteGitHubConfiguration = (id) => {
    this.gitHubConfigurations = this.gitHubConfigurations.filter((c) => c.id !== id);
    return Promise.resolve();
  };

  reset() {
    this.projectBindings = cloneDeep(defaultProjectBindings);
    this.dopSettings = cloneDeep(defaultDopSettings);
    this.gitHubConfigurations = [];
  }

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
