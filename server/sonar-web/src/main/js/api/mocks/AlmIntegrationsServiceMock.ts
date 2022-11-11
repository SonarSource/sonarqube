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
import { mockGitlabProject } from '../../helpers/mocks/alm-integrations';
import { GitlabProject } from '../../types/alm-integration';
import {
  checkPersonalAccessTokenIsValid,
  getGitlabProjects,
  setAlmPersonalAccessToken,
} from '../alm-integrations';

export default class AlmIntegrationsServiceMock {
  almInstancePATMap: { [key: string]: boolean } = {};
  gitlabProjects: GitlabProject[];
  defaultAlmInstancePATMap: { [key: string]: boolean } = {
    'conf-final-1': false,
    'conf-final-2': true,
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

  constructor() {
    this.almInstancePATMap = cloneDeep(this.defaultAlmInstancePATMap);
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
    (checkPersonalAccessTokenIsValid as jest.Mock).mockImplementation(
      this.checkPersonalAccessTokenIsValid
    );
    (setAlmPersonalAccessToken as jest.Mock).mockImplementation(this.setAlmPersonalAccessToken);
    (getGitlabProjects as jest.Mock).mockImplementation(this.getGitlabProjects);
  }

  checkPersonalAccessTokenIsValid = (conf: string) => {
    return Promise.resolve({ status: this.almInstancePATMap[conf] });
  };

  setAlmPersonalAccessToken = (conf: string) => {
    this.almInstancePATMap[conf] = true;
    return Promise.resolve();
  };

  getGitlabProjects = () => {
    return Promise.resolve({
      projects: this.gitlabProjects,
      projectsPaging: {
        pageIndex: 1,
        pageSize: 30,
        total: 3,
      },
    });
  };

  setGitlabProjects(gitlabProjects: GitlabProject[]) {
    this.gitlabProjects = gitlabProjects;
  }

  reset = () => {
    this.almInstancePATMap = cloneDeep(this.defaultAlmInstancePATMap);
    this.gitlabProjects = cloneDeep(this.defaultGitlabProjects);
  };
}
