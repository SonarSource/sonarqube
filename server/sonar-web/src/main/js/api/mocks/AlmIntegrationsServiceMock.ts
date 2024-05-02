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
import { cloneDeep, uniqueId } from 'lodash';
import { Visibility } from '~sonar-aligned/types/component';
import {
  mockAzureProject,
  mockAzureRepository,
  mockBitbucketCloudRepository,
  mockBitbucketProject,
  mockBitbucketRepository,
  mockGitHubRepository,
  mockGitlabProject,
} from '../../helpers/mocks/alm-integrations';
import {
  AzureProject,
  AzureRepository,
  BitbucketCloudRepository,
  BitbucketProject,
  BitbucketRepository,
  GithubRepository,
  GitlabProject,
} from '../../types/alm-integration';
import { Paging } from '../../types/types';
import {
  checkPersonalAccessTokenIsValid,
  getAzureProjects,
  getAzureRepositories,
  getBitbucketServerProjects,
  getBitbucketServerRepositories,
  getGithubClientId,
  getGithubOrganizations,
  getGithubRepositories,
  getGitlabProjects,
  importAzureRepository,
  importBitbucketCloudRepository,
  importBitbucketServerProject,
  importGithubRepository,
  importGitlabProject,
  searchAzureRepositories,
  searchForBitbucketCloudRepositories,
  searchForBitbucketServerRepositories,
  setAlmPersonalAccessToken,
  setupAzureProjectCreation,
  setupBitbucketCloudProjectCreation,
  setupBitbucketServerProjectCreation,
  setupGithubProjectCreation,
  setupGitlabProjectCreation,
} from '../alm-integrations';

export default class AlmIntegrationsServiceMock {
  almInstancePATMap: { [key: string]: boolean } = {};
  gitlabProjects: GitlabProject[];
  azureProjects: AzureProject[];
  azureRepositories: AzureRepository[];
  githubRepositories: GithubRepository[];
  pagination: Paging;
  bitbucketCloudRepositories: BitbucketCloudRepository[];
  bitbucketIsLastPage: boolean;
  bitbucketRepositories: BitbucketRepository[];
  bitbucketProjects: BitbucketProject[];
  defaultAlmInstancePATMap: { [key: string]: boolean } = {
    'conf-final-1': false,
    'conf-final-2': true,
    'conf-github-1': false,
    'conf-github-2': true,
    'conf-azure-1': false,
    'conf-azure-2': true,
    'conf-bitbucketcloud-1': false,
    'conf-bitbucketcloud-2': true,
    'conf-bitbucketserver-1': false,
    'conf-bitbucketserver-2': true,
  };

  defaultGitlabProjects: GitlabProject[] = [
    mockGitlabProject({
      name: 'Gitlab project 1',
      id: '1',
      sqProjectKey: 'key',
      sqProjectName: 'Gitlab project 1',
      slug: 'Gitlab_project_1',
    }),
    mockGitlabProject({ name: 'Gitlab project 2', id: '2', slug: 'Gitlab_project_2' }),
    mockGitlabProject({ name: 'Gitlab project 3', id: '3', slug: 'Gitlab_project_3' }),
  ];

  defaultPagination = {
    pageIndex: 1,
    pageSize: 30,
    total: 30,
  };

  defaultAzureProjects: AzureProject[] = [
    mockAzureProject({ name: 'Azure project', description: 'Description project 1' }),
    mockAzureProject({ name: 'Azure project 2', description: 'Description project 2' }),
  ];

  defaultBitbucketCloudRepositories: BitbucketCloudRepository[] = [
    mockBitbucketCloudRepository({
      uuid: 1000,
      name: 'BitbucketCloud Repo 1',
      slug: 'bitbucketcloud_repo_1',
      sqProjectKey: 'key',
    }),
    mockBitbucketCloudRepository({
      uuid: 10001,
      name: 'BitbucketCloud Repo 2',
      slug: 'bitbucketcloud_repo_2',
    }),
  ];

  defaultBitbucketRepositories: BitbucketRepository[] = [
    mockBitbucketRepository({
      name: 'Bitbucket Repo 1',
      slug: 'bitbucket_repo_1',
      projectKey: 'bitbucket_project_1',
      sqProjectKey: 'key',
    }),
    mockBitbucketRepository({
      id: 2,
      name: 'Bitbucket Repo 2',
      slug: 'bitbucket_repo_2',
      projectKey: 'bitbucket_project_1',
    }),
  ];

  defaultBitbucketProjects: BitbucketProject[] = [
    mockBitbucketProject({ name: 'Bitbucket Project 1', key: 'bitbucket_project_1' }),
    mockBitbucketProject({ name: 'Bitbucket Project 2', key: 'bitbucket_project_2' }),
  ];

  defaultAzureRepositories: AzureRepository[] = [
    mockAzureRepository({ sqProjectKey: 'random', projectName: 'Azure project' }),
    mockAzureRepository({ name: 'Azure repo 2', projectName: 'Azure project' }),
    mockAzureRepository({ name: 'Azure repo 3', projectName: 'Azure project 2' }),
  ];

  defaultGithubRepositories: GithubRepository[] = [
    mockGitHubRepository({ name: 'Github repo 1', sqProjectKey: 'key123' }),
    mockGitHubRepository({
      name: 'Github repo 2',
      id: 'id1231',
      key: 'key1231',
    }),
  ];

  defaultOrganizations = {
    paging: {
      pageIndex: 1,
      pageSize: 100,
      total: 1,
    },
    organizations: [
      {
        key: 'org-1',
        name: 'org-1',
      },
    ],
  };

  constructor() {
    this.almInstancePATMap = cloneDeep(this.defaultAlmInstancePATMap);
    this.azureProjects = cloneDeep(this.defaultAzureProjects);
    this.azureRepositories = cloneDeep(this.defaultAzureRepositories);
    this.bitbucketCloudRepositories = cloneDeep(this.defaultBitbucketCloudRepositories);
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
    this.pagination = cloneDeep(this.defaultPagination);
    this.githubRepositories = cloneDeep(this.defaultGithubRepositories);
    this.bitbucketRepositories = cloneDeep(this.defaultBitbucketRepositories);
    this.bitbucketProjects = cloneDeep(this.defaultBitbucketProjects);
    this.bitbucketIsLastPage = true;
    jest
      .mocked(checkPersonalAccessTokenIsValid)
      .mockImplementation(this.checkPersonalAccessTokenIsValid);
    jest.mocked(setAlmPersonalAccessToken).mockImplementation(this.setAlmPersonalAccessToken);
    jest.mocked(getGitlabProjects).mockImplementation(this.getGitlabProjects);
    jest.mocked(setupGitlabProjectCreation).mockReturnValue(() => this.importProject());
    jest.mocked(importGitlabProject).mockImplementation(this.importProject);
    jest.mocked(setupBitbucketCloudProjectCreation).mockReturnValue(() => this.importProject());
    jest.mocked(importBitbucketCloudRepository).mockImplementation(this.importProject);
    jest.mocked(getGithubClientId).mockImplementation(this.getGithubClientId);
    jest.mocked(getGithubOrganizations).mockImplementation(this.getGithubOrganizations);
    jest.mocked(getAzureProjects).mockImplementation(this.getAzureProjects);
    jest.mocked(getAzureRepositories).mockImplementation(this.getAzureRepositories);
    jest.mocked(getGithubRepositories).mockImplementation(this.getGithubRepositories);
    jest.mocked(searchAzureRepositories).mockImplementation(this.searchAzureRepositories);
    jest.mocked(setupAzureProjectCreation).mockReturnValue(() => this.importAzureRepository());
    jest.mocked(importAzureRepository).mockImplementation(this.importAzureRepository);
    jest.mocked(setupGithubProjectCreation).mockReturnValue(() => this.importGithubRepository());
    jest.mocked(importGithubRepository).mockImplementation(this.importGithubRepository);
    jest
      .mocked(searchForBitbucketCloudRepositories)
      .mockImplementation(this.searchForBitbucketCloudRepositories);
    jest.mocked(getBitbucketServerProjects).mockImplementation(this.getBitbucketServerProjects);
    jest
      .mocked(getBitbucketServerRepositories)
      .mockImplementation(this.getBitbucketServerRepositories);
    jest.mocked(importBitbucketServerProject).mockImplementation(this.importBitbucketServerProject);
    jest
      .mocked(setupBitbucketServerProjectCreation)
      .mockReturnValue(() => this.importBitbucketServerProject());
    jest
      .mocked(searchForBitbucketServerRepositories)
      .mockImplementation(this.searchForBitbucketServerRepositories);
  }

  checkPersonalAccessTokenIsValid = (conf: string) => {
    return Promise.resolve({ status: this.almInstancePATMap[conf] });
  };

  setAlmPersonalAccessToken = (conf: string) => {
    this.almInstancePATMap[conf] = true;
    return Promise.resolve();
  };

  getAzureProjects = () => {
    return Promise.resolve({ projects: this.azureProjects });
  };

  getAzureRepositories: typeof getAzureRepositories = (_, projectName) => {
    return Promise.resolve({
      repositories: this.azureRepositories.filter((repo) => repo.projectName === projectName),
    });
  };

  searchAzureRepositories: typeof searchAzureRepositories = (_, searchQuery) => {
    return Promise.resolve({
      repositories: this.azureRepositories.filter((repo) => repo.name.includes(searchQuery)),
    });
  };

  setSearchAzureRepositories = (azureRepositories: AzureRepository[]) => {
    this.azureRepositories = azureRepositories;
  };

  importAzureRepository = () => {
    return Promise.resolve({
      project: {
        key: 'key',
        name: 'name',
        qualifier: 'qualifier',
        visibility: Visibility.Private,
      },
    });
  };

  searchForBitbucketCloudRepositories = () => {
    return Promise.resolve({
      isLastPage: this.bitbucketIsLastPage,
      repositories: this.bitbucketCloudRepositories,
    });
  };

  setBitbucketCloudRepositories(bitbucketCloudRepositories: BitbucketCloudRepository[]) {
    this.bitbucketCloudRepositories = bitbucketCloudRepositories;
  }

  getGitlabProjects = () => {
    return Promise.resolve({
      projects: this.gitlabProjects,
      projectsPaging: this.pagination,
    });
  };

  importProject = () => {
    return Promise.resolve({
      project: {
        key: 'key',
        name: 'name',
        qualifier: 'qualifier',
        visibility: Visibility.Private,
      },
    });
  };

  createRandomGitlabProjectsWithLoadMore(quantity: number, total: number) {
    const generatedProjects = Array.from(Array(quantity).keys()).map((index) => {
      return mockGitlabProject({ name: `Gitlab project ${index}`, id: uniqueId() });
    });
    this.gitlabProjects = generatedProjects;
    this.pagination = { ...this.defaultPagination, total };
  }

  createRandomBitbucketCloudProjectsWithLoadMore(quantity: number, total: number) {
    const generatedRepositories = Array.from(Array(quantity).keys()).map((index) => {
      return mockBitbucketCloudRepository({
        name: `Gitlab project ${index}`,
        uuid: Math.floor(Math.random() * 100000),
      });
    });

    this.bitbucketCloudRepositories = generatedRepositories;
    this.bitbucketIsLastPage = quantity >= total;
  }

  createRandomGithubRepositoriessWithLoadMore(quantity: number, total: number) {
    const generatedProjects = Array.from(Array(quantity).keys()).map(() => {
      const id = uniqueId();
      return mockGitHubRepository({
        name: `Github repo ${id}`,
        key: `key_${id}`,
        id,
      });
    });
    this.githubRepositories = generatedProjects;
    this.pagination = { ...this.defaultPagination, total };
  }

  importGithubRepository = () => {
    return Promise.resolve({
      project: {
        key: 'key',
        name: 'name',
        qualifier: 'qualifier',
        visibility: Visibility.Private,
      },
    });
  };

  getGithubRepositories = () => {
    return Promise.resolve({
      repositories: this.githubRepositories,
      paging: this.pagination,
    });
  };

  setGithubRepositories(githubProjects: GithubRepository[]) {
    this.githubRepositories = githubProjects;
  }

  setGitlabProjects(gitlabProjects: GitlabProject[]) {
    this.gitlabProjects = gitlabProjects;
  }

  getGithubClientId = () => {
    return Promise.resolve({ clientId: 'clientId' });
  };

  getGithubOrganizations = () => {
    return Promise.resolve(this.defaultOrganizations);
  };

  getBitbucketServerProjects = () => {
    return Promise.resolve({ projects: this.bitbucketProjects });
  };

  getBitbucketServerRepositories = () => {
    return Promise.resolve({
      isLastPage: this.bitbucketIsLastPage,
      repositories: this.bitbucketRepositories,
    });
  };

  setBitbucketServerProjects = (bitbucketProjects: BitbucketProject[]) => {
    this.bitbucketProjects = bitbucketProjects;
  };

  importBitbucketServerProject = () => {
    return Promise.resolve({
      project: {
        key: 'key',
        name: 'name',
        qualifier: 'qualifier',
        visibility: Visibility.Private,
      },
    });
  };

  searchForBitbucketServerRepositories = () => {
    return Promise.resolve({
      isLastPage: this.bitbucketIsLastPage,
      repositories: this.bitbucketRepositories,
    });
  };

  reset = () => {
    this.almInstancePATMap = cloneDeep(this.defaultAlmInstancePATMap);
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
    this.azureRepositories = cloneDeep(this.defaultAzureRepositories);
    this.pagination = cloneDeep(this.defaultPagination);
    this.bitbucketCloudRepositories = cloneDeep(this.defaultBitbucketCloudRepositories);
    this.bitbucketRepositories = cloneDeep(this.defaultBitbucketRepositories);
    this.bitbucketProjects = cloneDeep(this.defaultBitbucketProjects);
    this.bitbucketIsLastPage = true;
  };
}
