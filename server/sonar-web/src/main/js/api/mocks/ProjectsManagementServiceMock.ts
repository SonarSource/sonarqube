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
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import { mockProject } from '../../helpers/mocks/projects';
import { SettingsKey } from '../../types/settings';
import {
  Project,
  bulkDeleteProjects,
  changeProjectDefaultVisibility,
  createProject,
  deletePortfolio,
  deleteProject,
  getComponents,
} from '../project-management';
import SettingsServiceMock from './SettingsServiceMock';

jest.mock('../project-management');

const defaultProject = [
  mockProject({ key: 'project1', name: 'Project 1' }),
  mockProject({ key: 'project2', name: 'Project 2', visibility: Visibility.Private }),
  mockProject({ key: 'project3', name: 'Project 3', lastAnalysisDate: undefined }),
  mockProject({ key: 'projectProvisioned', name: 'Project 4', managed: true }),
  mockProject({ key: 'portfolio1', name: 'Portfolio 1', qualifier: ComponentQualifier.Portfolio }),
  mockProject({
    key: 'portfolio2',
    name: 'Portfolio 2',
    qualifier: ComponentQualifier.Portfolio,
    visibility: Visibility.Private,
  }),
  mockProject({
    key: 'portfolio3',
    name: 'Portfolio 3',
    qualifier: ComponentQualifier.Portfolio,
    lastAnalysisDate: undefined,
  }),
  mockProject({
    key: 'application1',
    name: 'Application 1',
    qualifier: ComponentQualifier.Application,
  }),
  mockProject({
    key: 'application2',
    name: 'Application 2',
    qualifier: ComponentQualifier.Application,
    visibility: Visibility.Private,
  }),
  mockProject({
    key: 'application3',
    name: 'Application 3',
    qualifier: ComponentQualifier.Application,
    lastAnalysisDate: undefined,
  }),
];

export default class ProjectManagementServiceMock {
  #projects: Project[];
  #settingsService: SettingsServiceMock;

  constructor(settingsService: SettingsServiceMock) {
    this.#projects = cloneDeep(defaultProject);
    this.#settingsService = settingsService;
    jest.mocked(getComponents).mockImplementation(this.handleGetComponents);
    jest.mocked(createProject).mockImplementation(this.handleCreateProject);
    jest.mocked(bulkDeleteProjects).mockImplementation(this.handleBulkDeleteProjects);
    jest.mocked(deleteProject).mockImplementation(this.handleDeleteProject);
    jest.mocked(deletePortfolio).mockImplementation(this.handleDeletePortfolio);
    jest
      .mocked(changeProjectDefaultVisibility)
      .mockImplementation(this.handleChangeProjectDefaultVisibility);
  }

  setProjects = (projects: Project[]) => {
    this.#projects = cloneDeep(projects);
  };

  handleGetComponents: typeof getComponents = (params) => {
    const pageIndex = params.p || 1;
    const pageSize = params.ps || 50;
    const components = this.#projects.filter((item) => {
      if (params.qualifiers && item.qualifier !== params.qualifiers) {
        return false;
      }
      if (params.visibility && item.visibility !== params.visibility) {
        return false;
      }
      if (
        params.analyzedBefore &&
        (!item.lastAnalysisDate ||
          new Date(item.lastAnalysisDate) > new Date(params.analyzedBefore))
      ) {
        return false;
      }
      if (params.onProvisionedOnly && !item.key.includes('Provisioned')) {
        return false;
      }
      if (
        params.q &&
        !item.key.toLowerCase().includes(params.q.toLowerCase()) &&
        !item.name.toLowerCase().includes(params.q.toLowerCase())
      ) {
        return false;
      }
      if (params.projects !== undefined && !params.projects.split(',').includes(item.key)) {
        return false;
      }
      return true;
    });

    return this.reply({
      components: components.slice((pageIndex - 1) * pageSize, pageSize * pageIndex),
      paging: { pageIndex, pageSize, total: components.length },
    });
  };

  handleCreateProject: typeof createProject = ({ project, name, visibility }) => {
    this.#projects.unshift(
      mockProject({
        key: project,
        name,
        visibility,
        lastAnalysisDate: undefined,
      }),
    );
    return this.reply({ project: this.#projects[0] });
  };

  handleBulkDeleteProjects: typeof bulkDeleteProjects = ({ projects }) => {
    if (projects === undefined) {
      return Promise.reject();
    }

    this.#projects = this.#projects.filter((item) => !projects.split(',').includes(item.key));
    return this.reply();
  };

  handleDeleteProject: typeof deleteProject = (key) => {
    const projectToDelete = this.#projects.find((item) => key === item.key);
    if (projectToDelete?.qualifier !== ComponentQualifier.Project) {
      return Promise.reject();
    }
    this.#projects = this.#projects.filter((p) => p.key !== key);
    return this.reply();
  };

  handleDeletePortfolio: typeof deletePortfolio = (key) => {
    const portfolioToDelete = this.#projects.find((item) => key === item.key);
    if (portfolioToDelete?.qualifier !== ComponentQualifier.Portfolio) {
      return Promise.reject();
    }
    this.#projects = this.#projects.filter((p) => p.key !== key);
    return this.reply();
  };

  handleChangeProjectDefaultVisibility: typeof changeProjectDefaultVisibility = (visibility) => {
    this.#settingsService.set(SettingsKey.DefaultProjectVisibility, visibility);
    return this.reply();
  };

  reset = () => {
    this.#projects = cloneDeep(defaultProject);
  };

  reply<T>(): Promise<void>;
  reply<T>(response: T): Promise<T>;
  reply<T>(response?: T): Promise<T | void> {
    return Promise.resolve(response ? cloneDeep(response) : undefined);
  }
}
