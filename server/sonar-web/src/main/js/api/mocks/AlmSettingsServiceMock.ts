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
import { mockAlmSettingsInstance } from '../../helpers/mocks/alm-settings';
import {
  AlmKeys,
  AlmSettingsBindingDefinitions,
  AlmSettingsInstance,
  AzureBindingDefinition,
  AzureProjectAlmBindingParams,
  BitbucketCloudBindingDefinition,
  BitbucketCloudProjectAlmBindingParams,
  BitbucketProjectAlmBindingParams,
  BitbucketServerBindingDefinition,
  GithubBindingDefinition,
  GithubProjectAlmBindingParams,
  GitlabBindingDefinition,
  GitlabProjectAlmBindingParams,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingParams,
  ProjectAlmBindingResponse,
} from '../../types/alm-settings';
import {
  countBoundProjects,
  createAzureConfiguration,
  createBitbucketCloudConfiguration,
  createBitbucketServerConfiguration,
  createGithubConfiguration,
  createGitlabConfiguration,
  deleteConfiguration,
  deleteProjectAlmBinding,
  getAlmDefinitions,
  getAlmSettings,
  getAlmSettingsNoCatch,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectBitbucketCloudBinding,
  setProjectGithubBinding,
  setProjectGitlabBinding,
  updateAzureConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketServerConfiguration,
  updateGithubConfiguration,
  updateGitlabConfiguration,
  validateAlmSettings,
  validateProjectAlmBinding,
} from '../alm-settings';

const defaultAlmDefinitions = {
  [AlmKeys.Azure]: [],
  [AlmKeys.BitbucketServer]: [],
  [AlmKeys.BitbucketCloud]: [],
  [AlmKeys.GitHub]: [],
  [AlmKeys.GitLab]: [],
};

const defaultAlmSettings = [
  mockAlmSettingsInstance({ key: 'conf-final-1', alm: AlmKeys.GitLab }),
  mockAlmSettingsInstance({ key: 'conf-final-2', alm: AlmKeys.GitLab }),
  mockAlmSettingsInstance({ key: 'conf-github-1', alm: AlmKeys.GitHub, url: 'http://url' }),
  mockAlmSettingsInstance({ key: 'conf-github-2', alm: AlmKeys.GitHub, url: 'http://url' }),
  mockAlmSettingsInstance({ key: 'conf-github-3', alm: AlmKeys.GitHub, url: 'javascript://url' }),
  mockAlmSettingsInstance({ key: 'conf-azure-1', alm: AlmKeys.Azure, url: 'url' }),
  mockAlmSettingsInstance({ key: 'conf-azure-2', alm: AlmKeys.Azure, url: 'url' }),
  mockAlmSettingsInstance({
    key: 'conf-bitbucketcloud-1',
    alm: AlmKeys.BitbucketCloud,
    url: 'url',
  }),
  mockAlmSettingsInstance({
    key: 'conf-bitbucketcloud-2',
    alm: AlmKeys.BitbucketCloud,
    url: 'url',
  }),
  mockAlmSettingsInstance({
    key: 'conf-bitbucketserver-1',
    alm: AlmKeys.BitbucketServer,
    url: 'url',
  }),
  mockAlmSettingsInstance({
    key: 'conf-bitbucketserver-2',
    alm: AlmKeys.BitbucketServer,
    url: 'url',
  }),
];

interface EnhancedProjectAlmBindingParam extends ProjectAlmBindingParams {
  projectName?: string;
  repository?: string;
  repositoryName?: string;
  slug?: string;
  summaryCommentEnabled?: boolean;
}

jest.mock('../alm-settings');

export default class AlmSettingsServiceMock {
  #almDefinitions: AlmSettingsBindingDefinitions;
  #almSettings: AlmSettingsInstance[];
  #definitionError = '';
  #projectsBindings: { [key: string]: ProjectAlmBindingResponse | undefined } = {};
  #projectBindingConfigurationErrors: ProjectAlmBindingConfigurationErrors | undefined = undefined;

  constructor() {
    this.#almSettings = cloneDeep(defaultAlmSettings);
    this.#almDefinitions = cloneDeep(defaultAlmDefinitions);
    jest.mocked(getAlmSettings).mockImplementation(this.handleGetAlmSettings);
    jest.mocked(getAlmSettingsNoCatch).mockImplementation(this.handleGetAlmSettings);
    jest.mocked(getAlmDefinitions).mockImplementation(this.handleGetAlmDefinitions);
    jest.mocked(countBoundProjects).mockImplementation(this.handleCountBoundProjects);
    jest.mocked(validateAlmSettings).mockImplementation(this.handleValidateAlmSettings);
    jest.mocked(deleteConfiguration).mockImplementation(this.handleDeleteConfiguration);
    jest.mocked(createGithubConfiguration).mockImplementation(this.handleCreateGithubConfiguration);
    jest.mocked(createGitlabConfiguration).mockImplementation(this.handleCreateGitlabConfiguration);
    jest.mocked(createAzureConfiguration).mockImplementation(this.handleCreateAzureConfiguration);
    jest
      .mocked(createBitbucketServerConfiguration)
      .mockImplementation(this.handleCreateBitbucketServerConfiguration);
    jest
      .mocked(createBitbucketCloudConfiguration)
      .mockImplementation(this.handleCreateBitbucketCloudConfiguration);
    jest.mocked(updateGithubConfiguration).mockImplementation(this.handleUpdateGithubConfiguration);
    jest.mocked(updateGitlabConfiguration).mockImplementation(this.handleUpdateGitlabConfiguration);
    jest.mocked(updateAzureConfiguration).mockImplementation(this.handleUpdateAzureConfiguration);
    jest
      .mocked(updateBitbucketServerConfiguration)
      .mockImplementation(this.handleUpdateBitbucketServerConfiguration);
    jest
      .mocked(updateBitbucketCloudConfiguration)
      .mockImplementation(this.handleUpdateBitbucketCloudConfiguration);
    jest.mocked(getProjectAlmBinding).mockImplementation(this.handleGetProjectBinding);
    jest.mocked(deleteProjectAlmBinding).mockImplementation(this.handleDeleteProjectAlmBinding);
    jest.mocked(setProjectAzureBinding).mockImplementation(this.handleSetProjectAzureBinding);
    jest
      .mocked(setProjectBitbucketBinding)
      .mockImplementation(this.handleSetProjectBitbucketBinding);
    jest
      .mocked(setProjectBitbucketCloudBinding)
      .mockImplementation(this.handleSetProjectBitbucketCloudBinding);
    jest.mocked(setProjectGithubBinding).mockImplementation(this.handleSetProjectGithubBinding);
    jest.mocked(setProjectGitlabBinding).mockImplementation(this.handleSetProjectGitlabBinding);
    jest.mocked(validateProjectAlmBinding).mockImplementation(this.handleValidateProjectAlmBinding);
  }

  handleGetAlmDefinitions = () => {
    return this.reply(this.#almDefinitions);
  };

  handleGetAlmSettings = () => {
    return this.reply(this.#almSettings);
  };

  handleValidateAlmSettings = () => {
    return this.reply(this.#definitionError);
  };

  handleCountBoundProjects = () => {
    return this.reply({ projects: 5 });
  };

  setDefinitionErrorMessage = (message: string) => {
    this.#definitionError = message;
  };

  handleDeleteConfiguration = (key: string) => {
    for (const definitionsGroup of Object.values(this.#almDefinitions) as [
      GithubBindingDefinition[],
      GitlabBindingDefinition[],
      AzureBindingDefinition[],
      BitbucketCloudBindingDefinition[],
      BitbucketServerBindingDefinition[],
    ]) {
      const foundIndex = definitionsGroup.findIndex((definition) => definition.key === key);
      if (foundIndex !== -1) {
        definitionsGroup.splice(foundIndex, 1);
        break;
      }
    }
    return this.reply(undefined);
  };

  removeFromAlmSettings = (almKey: string) => {
    this.#almSettings = cloneDeep(defaultAlmSettings).filter(
      (almSetting) => almSetting.alm !== almKey,
    );
  };

  handleCreateGithubConfiguration = (data: GithubBindingDefinition) => {
    this.#almDefinitions[AlmKeys.GitHub].push(data);

    return this.reply(undefined);
  };

  handleCreateGitlabConfiguration = (data: GitlabBindingDefinition) => {
    this.#almDefinitions[AlmKeys.GitLab].push(data);

    return this.reply(undefined);
  };

  handleCreateAzureConfiguration = (data: AzureBindingDefinition) => {
    this.#almDefinitions[AlmKeys.Azure].push(data);

    return this.reply(undefined);
  };

  handleCreateBitbucketServerConfiguration = (data: BitbucketServerBindingDefinition) => {
    this.#almDefinitions[AlmKeys.BitbucketServer].push(data);

    return this.reply(undefined);
  };

  handleCreateBitbucketCloudConfiguration = (data: BitbucketCloudBindingDefinition) => {
    this.#almDefinitions[AlmKeys.BitbucketCloud].push(data);

    return this.reply(undefined);
  };

  handleUpdateGithubConfiguration = (data: GithubBindingDefinition & { newKey: string }) => {
    const definition = this.#almDefinitions[AlmKeys.GitHub].find(
      (item) => item.key === data.key,
    ) as GithubBindingDefinition;
    Object.assign(definition, { ...data, key: data.newKey });

    return this.reply(undefined);
  };

  handleUpdateGitlabConfiguration = (data: GitlabBindingDefinition & { newKey: string }) => {
    const definition = this.#almDefinitions[AlmKeys.GitLab].find(
      (item) => item.key === data.key,
    ) as GitlabBindingDefinition;
    Object.assign(definition, { ...data, key: data.newKey });

    return this.reply(undefined);
  };

  handleUpdateAzureConfiguration = (data: AzureBindingDefinition & { newKey: string }) => {
    const definition = this.#almDefinitions[AlmKeys.Azure].find(
      (item) => item.key === data.key,
    ) as AzureBindingDefinition;
    Object.assign(definition, { ...data, key: data.newKey });

    return this.reply(undefined);
  };

  handleUpdateBitbucketServerConfiguration = (
    data: BitbucketServerBindingDefinition & { newKey: string },
  ) => {
    const definition = this.#almDefinitions[AlmKeys.BitbucketServer].find(
      (item) => item.key === data.key,
    ) as BitbucketServerBindingDefinition;
    Object.assign(definition, { ...data, key: data.newKey });

    return this.reply(undefined);
  };

  handleUpdateBitbucketCloudConfiguration = (
    data: BitbucketCloudBindingDefinition & { newKey: string },
  ) => {
    const definition = this.#almDefinitions[AlmKeys.BitbucketCloud].find(
      (item) => item.key === data.key,
    ) as BitbucketCloudBindingDefinition;
    Object.assign(definition, { ...data, key: data.newKey });

    return this.reply(undefined);
  };

  handleGetProjectBinding = (project: string) => {
    const projectBinding = this.#projectsBindings[project];

    if (projectBinding === undefined) {
      return Promise.reject(
        new Response('', {
          status: 404,
        }),
      );
    }

    return this.reply(projectBinding);
  };

  handleSetProjectBinding = (alm: AlmKeys, data: EnhancedProjectAlmBindingParam) => {
    this.#projectsBindings[data.project] = {
      alm,
      key: data.almSetting,
      repository: data.repositoryName ?? (data.repository as string),
      monorepo: data.monorepo,
      slug: data.projectName ?? data.slug,
      summaryCommentEnabled: data.summaryCommentEnabled ?? false,
      url: 'https://company.com/project',
    };
    return this.reply(undefined);
  };

  handleSetProjectAzureBinding = (data: AzureProjectAlmBindingParams) => {
    return this.handleSetProjectBinding(AlmKeys.Azure, data);
  };

  handleSetProjectBitbucketBinding = (data: BitbucketProjectAlmBindingParams) => {
    return this.handleSetProjectBinding(AlmKeys.BitbucketServer, data);
  };

  handleSetProjectBitbucketCloudBinding = (data: BitbucketCloudProjectAlmBindingParams) => {
    return this.handleSetProjectBinding(AlmKeys.BitbucketCloud, data);
  };

  handleSetProjectGithubBinding = (data: GithubProjectAlmBindingParams) => {
    return this.handleSetProjectBinding(AlmKeys.GitHub, data);
  };

  handleSetProjectGitlabBinding = (data: GitlabProjectAlmBindingParams) => {
    return this.handleSetProjectBinding(AlmKeys.GitLab, data);
  };

  handleValidateProjectAlmBinding = () => {
    return this.reply(this.#projectBindingConfigurationErrors);
  };

  setProjectBindingConfigurationErrors = (errors?: ProjectAlmBindingConfigurationErrors) => {
    this.#projectBindingConfigurationErrors = errors;
  };

  handleDeleteProjectAlmBinding = (project: string) => {
    this.#projectsBindings[project] = undefined;
    return this.reply(undefined);
  };

  reset = () => {
    this.#almSettings = cloneDeep(defaultAlmSettings);
    this.#almDefinitions = cloneDeep(defaultAlmDefinitions);
    this.#projectsBindings = {};
    this.#projectBindingConfigurationErrors = undefined;
    this.setDefinitionErrorMessage('');
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
