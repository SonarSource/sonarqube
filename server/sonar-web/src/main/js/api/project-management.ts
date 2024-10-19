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
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import { post, postJSON } from '../helpers/request';
import { Paging } from '../types/types';

export interface BaseSearchProjectsParameters {
  analyzedBefore?: string;
  onProvisionedOnly?: boolean;
  projects?: string;
  q?: string;
  qualifiers?: string;
  visibility?: Visibility;
}

export interface ProjectBase {
  key: string;
  name: string;
  qualifier:
    | ComponentQualifier.Application
    | ComponentQualifier.Portfolio
    | ComponentQualifier.Project;
  visibility: Visibility;
}

export interface Project extends ProjectBase {
  lastAnalysisDate?: string;
  managed?: boolean;
}

export interface SearchProjectsParameters extends BaseSearchProjectsParameters {
  p?: number;
  ps?: number;
}

export function getComponents(parameters: SearchProjectsParameters): Promise<{
  components: Project[];
  paging: Paging;
}> {
  return getJSON('/api/projects/search', parameters);
}

export function bulkDeleteProjects(
  parameters: BaseSearchProjectsParameters,
): Promise<void | Response> {
  return post('/api/projects/bulk_delete', parameters).catch(throwGlobalError);
}

export function deleteProject(project: string): Promise<void | Response> {
  return post('/api/projects/delete', { project }).catch(throwGlobalError);
}

export function deletePortfolio(portfolio: string): Promise<void | Response> {
  return post('/api/views/delete', { key: portfolio }).catch(throwGlobalError);
}

export function createProject(data: {
  mainBranch: string;
  name: string;
  newCodeDefinitionType?: string;
  newCodeDefinitionValue?: string;
  project: string;
  visibility?: Visibility;
}): Promise<{ project: ProjectBase }> {
  return postJSON('/api/projects/create', data).catch(throwGlobalError);
}

export function changeProjectDefaultVisibility(
  projectVisibility: Visibility,
): Promise<void | Response> {
  return post('/api/projects/update_default_visibility', { projectVisibility }).catch(
    throwGlobalError,
  );
}
