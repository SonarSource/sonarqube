/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import throwGlobalError from '../app/utils/throwGlobalError';
import { getJSON, post, postJSON } from '../sonar-ui-common/helpers/request';
import { Application, ApplicationPeriod, ApplicationProject } from '../types/application';
import { Visibility } from '../types/component';

export function getApplicationLeak(
  application: string,
  branch?: string
): Promise<ApplicationPeriod[]> {
  return getJSON('/api/applications/show_leak', { application, branch }).then(
    r => r.leaks,
    throwGlobalError
  );
}

export function getApplicationDetails(application: string, branch?: string): Promise<Application> {
  return getJSON('/api/applications/show', { application, branch }).then(
    r => r.application,
    throwGlobalError
  );
}

export function addApplicationBranch(data: {
  application: string;
  branch: string;
  project: string[];
  projectBranch: string[];
}) {
  return post('/api/applications/create_branch', data).catch(throwGlobalError);
}

export function updateApplicationBranch(data: {
  application: string;
  branch: string;
  name: string;
  project: string[];
  projectBranch: string[];
}) {
  return post('/api/applications/update_branch', data).catch(throwGlobalError);
}

export function deleteApplicationBranch(application: string, branch: string) {
  return post('/api/applications/delete_branch', { application, branch }).catch(throwGlobalError);
}

export function getApplicationProjects(data: {
  application: string;
  p?: number;
  ps?: number;
  q?: string;
  selected: string;
}): Promise<{ paging: T.Paging; projects: ApplicationProject[] }> {
  return getJSON('/api/applications/search_projects', data).catch(throwGlobalError);
}

export function addProjectToApplication(application: string, project: string) {
  return post('/api/applications/add_project', { application, project }).catch(throwGlobalError);
}

export function removeProjectFromApplication(application: string, project: string) {
  return post('/api/applications/remove_project', { application, project }).catch(throwGlobalError);
}

export function refreshApplication(key: string) {
  return post('/api/applications/refresh', { key }).catch(throwGlobalError);
}

export function createApplication(
  name: string,
  description: string,
  key: string | undefined,
  visibility: string
): Promise<{
  application: {
    description?: string;
    key: string;
    name: string;
    visibility: Visibility;
  };
}> {
  return postJSON('/api/applications/create', { description, key, name, visibility }).catch(
    throwGlobalError
  );
}

export function deleteApplication(application: string) {
  return post('/api/applications/delete', { application }).catch(throwGlobalError);
}

export function editApplication(application: string, name: string, description: string) {
  return post('/api/applications/update', { name, description, application }).catch(
    throwGlobalError
  );
}
