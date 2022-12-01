/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
  mockAzureProject,
  mockAzureRepository,
  mockGitlabProject,
} from '../../helpers/mocks/alm-integrations';
import { AzureProject, AzureRepository, GitlabProject } from '../../types/alm-integration';
import {
  checkPersonalAccessTokenIsValid,
  getAzureProjects,
  getAzureRepositories,
  getGithubClientId,
  getGithubOrganizations,
  getGitlabProjects,
  importAzureRepository,
  searchAzureRepositories,
  setAlmPersonalAccessToken,
} from '../alm-integrations';
import { ProjectBase } from '../components';

export default class AlmIntegrationsServiceMock {
  almInstancePATMap: { [key: string]: boolean } = {};
  gitlabProjects: GitlabProject[];
  azureProjects: AzureProject[];
  azureRepositories: AzureRepository[];
  defaultAlmInstancePATMap: { [key: string]: boolean } = {
    'conf-final-1': false,
    'conf-final-2': true,
    'conf-github-1': false,
    'conf-github-2': true,
    'conf-azure-1': false,
    'conf-azure-2': true,
    'config-reject': false,
  };

  defaultGitlabProjects: GitlabProject[] = [
    mockGitlabProject({
      name: 'Gitlab project 1',
      id: '1',
      sqProjectKey: 'key',
      sqProjectName: 'Gitlab project 1',
    }),
    mockGitlabProject({ name: 'Gitlab project 2', id: '2' }),
    mockGitlabProject({ name: 'Gitlab project 3', id: '3' }),
  ];

  defaultAzureProjects: AzureProject[] = [
    mockAzureProject({ name: 'Azure project', description: 'Description project 1' }),
    mockAzureProject({ name: 'Azure project 2', description: 'Description project 2' }),
  ];

  defaultAzureRepositories: AzureRepository[] = [
    mockAzureRepository({ sqProjectKey: 'random' }),
    mockAzureRepository({ name: 'Azure repo 2' }),
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
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
    this.azureProjects = cloneDeep(this.defaultAzureProjects);
    this.azureRepositories = cloneDeep(this.defaultAzureRepositories);
    (checkPersonalAccessTokenIsValid as jest.Mock).mockImplementation(
      this.checkPersonalAccessTokenIsValid
    );
    (setAlmPersonalAccessToken as jest.Mock).mockImplementation(this.setAlmPersonalAccessToken);
    (getGitlabProjects as jest.Mock).mockImplementation(this.getGitlabProjects);
    (getGithubClientId as jest.Mock).mockImplementation(this.getGithubClientId);
    (getGithubOrganizations as jest.Mock).mockImplementation(this.getGithubOrganizations);
    (getAzureProjects as jest.Mock).mockImplementation(this.getAzureProjects);
    (getAzureRepositories as jest.Mock).mockImplementation(this.getAzureRepositories);
    (searchAzureRepositories as jest.Mock).mockImplementation(this.searchAzureRepositories);
    (importAzureRepository as jest.Mock).mockImplementation(this.importAzureRepository);
  }

  checkPersonalAccessTokenIsValid = (conf: string) => {
    return Promise.resolve({ status: this.almInstancePATMap[conf] });
  };

  setAlmPersonalAccessToken = (conf: string) => {
    this.almInstancePATMap[conf] = true;
    return Promise.resolve();
  };

  getAzureProjects = (): Promise<{ projects: AzureProject[] }> => {
    return Promise.resolve({ projects: this.azureProjects });
  };

  getAzureRepositories = (): Promise<{ repositories: AzureRepository[] }> => {
    return Promise.resolve({
      repositories: this.azureRepositories,
    });
  };

  searchAzureRepositories = (): Promise<{ repositories: AzureRepository[] }> => {
    return Promise.resolve({
      repositories: this.azureRepositories,
    });
  };

  setSearchAzureRepositories = (azureRepositories: AzureRepository[]) => {
    this.azureRepositories = azureRepositories;
  };

  importAzureRepository = (): Promise<{ project: ProjectBase }> => {
    return Promise.resolve({
      project: {
        key: 'key',
        name: 'name',
        qualifier: 'qualifier',
        visibility: 'private',
      },
    });
  };

  setAzureProjects = (azureProjects: AzureProject[]) => {
    this.azureProjects = azureProjects;
  };

  getGitlabProjects = () => {
    return Promise.resolve({
      projects: this.gitlabProjects,
      projectsPaging: {
        pageIndex: 1,
        pageSize: 30,
        total: this.gitlabProjects.length,
      },
    });
  };

  setGitlabProjects(gitlabProjects: GitlabProject[]) {
    this.gitlabProjects = gitlabProjects;
  }

  getGithubClientId = () => {
    return Promise.resolve({ clientId: 'clientId' });
  };

  getGithubOrganizations = () => {
    return Promise.resolve(this.defaultOrganizations);
  };

  reset = () => {
    this.almInstancePATMap = cloneDeep(this.defaultAlmInstancePATMap);
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
    this.azureRepositories = cloneDeep(this.defaultAzureRepositories);
  };
}
