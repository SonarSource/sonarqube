/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getJSON, postJSON, post, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import { Paging, Visibility } from '../app/types';

export interface BaseSearchProjectsParameters {
  analyzedBefore?: string;
  onProvisionedOnly?: boolean;
  organization: string;
  projects?: string;
  q?: string;
  qualifiers?: string;
  visibility?: Visibility;
}

export interface SearchProjectsParameters extends BaseSearchProjectsParameters {
  p?: number;
  ps?: number;
}

export interface SearchProjectsResponseComponent {
  id: string;
  key: string;
  lastAnalysisDate?: string;
  name: string;
  organization: string;
  qualifier: string;
  visibility: Visibility;
}

export interface SearchProjectsResponse {
  components: SearchProjectsResponseComponent[];
  paging: Paging;
}

export function getComponents(
  parameters: SearchProjectsParameters
): Promise<SearchProjectsResponse> {
  return getJSON('/api/projects/search', parameters);
}

export function bulkDeleteProjects(parameters: BaseSearchProjectsParameters): Promise<void> {
  return post('/api/projects/bulk_delete', parameters);
}

export function deleteProject(project: string): Promise<void | Response> {
  return post('/api/projects/delete', { project }).catch(throwGlobalError);
}

export function deletePortfolio(portfolio: string): Promise<void | Response> {
  return post('/api/views/delete', { key: portfolio }).catch(throwGlobalError);
}

export function createProject(data: {
  branch?: string;
  name: string;
  project: string;
  organization?: string;
}): Promise<any> {
  return postJSON('/api/projects/create', data).catch(throwGlobalError);
}

export function searchProjectTags(data?: { ps?: number; q?: string }): Promise<any> {
  return getJSON('/api/project_tags/search', data).catch(throwGlobalError);
}

export function setProjectTags(data: { project: string; tags: string }): Promise<void> {
  return post('/api/project_tags/set', data);
}

export function getComponentTree(
  strategy: string,
  componentKey: string,
  metrics: string[] = [],
  additional: RequestData = {}
): Promise<any> {
  const url = '/api/measures/component_tree';
  const data = Object.assign({}, additional, {
    baseComponentKey: componentKey,
    metricKeys: metrics.join(','),
    strategy
  });
  return getJSON(url, data);
}

export function getChildren(
  componentKey: string,
  metrics: string[] = [],
  additional: RequestData = {}
): Promise<any> {
  return getComponentTree('children', componentKey, metrics, additional);
}

export function getComponentLeaves(
  componentKey: string,
  metrics: string[] = [],
  additional: RequestData = {}
): Promise<any> {
  return getComponentTree('leaves', componentKey, metrics, additional);
}

export function getComponent(
  componentKey: string,
  metrics: string[] = [],
  branch?: string
): Promise<any> {
  const data = { branch, componentKey, metricKeys: metrics.join(',') };
  return getJSON('/api/measures/component', data).then(r => r.component);
}

export function getTree(component: string, options: RequestData = {}): Promise<any> {
  return getJSON('/api/components/tree', { ...options, component });
}

export function getComponentShow(component: string, branch?: string): Promise<any> {
  return getJSON('/api/components/show', { component, branch });
}

export function getParents(component: string): Promise<any> {
  return getComponentShow(component).then(r => r.ancestors);
}

export function getBreadcrumbs(component: string, branch?: string): Promise<any> {
  return getComponentShow(component, branch).then(r => {
    const reversedAncestors = [...r.ancestors].reverse();
    return [...reversedAncestors, r.component];
  });
}

export function getComponentData(component: string, branch?: string): Promise<any> {
  return getComponentShow(component, branch).then(r => r.component);
}

export function getMyProjects(data: RequestData): Promise<any> {
  const url = '/api/projects/search_my_projects';
  return getJSON(url, data);
}

export interface Component {
  organization: string;
  id: string;
  key: string;
  name: string;
  isFavorite?: boolean;
  analysisDate?: string;
  tags: string[];
  visibility: string;
  leakPeriodDate?: string;
}

export interface Facet {
  property: string;
  values: Array<{ val: string; count: number }>;
}

export function searchProjects(
  data: RequestData
): Promise<{ components: Component[]; facets: Facet[]; paging: Paging }> {
  const url = '/api/components/search_projects';
  return getJSON(url, data);
}

export function searchComponents(data?: {
  q?: string;
  qualifiers?: string;
  ps?: number;
}): Promise<any> {
  return getJSON('/api/components/search', data);
}

/**
 * Change component's key
 */
export function changeKey(from: string, to: string): Promise<void> {
  const url = '/api/projects/update_key';
  const data = { from, to };
  return post(url, data);
}

/**
 * Bulk change component's key
 */
export function bulkChangeKey(
  project: string,
  from: string,
  to: string,
  dryRun: boolean = false
): Promise<any> {
  const url = '/api/projects/bulk_update_key';
  const data = { project, from, to, dryRun };
  return postJSON(url, data);
}

export interface SuggestionsResponse {
  organizations: Array<{ key: string; name: string }>;
  projects: Array<{ key: string; name: string }>;
  results: Array<{
    items: Array<{
      isFavorite: boolean;
      isRecentlyBrowsed: boolean;
      key: string;
      match: string;
      name: string;
      organization: string;
      project: string;
    }>;
    more: number;
    q: string;
  }>;
  warning?: string;
}

export function getSuggestions(
  query?: string,
  recentlyBrowsed?: string[],
  more?: string
): Promise<SuggestionsResponse> {
  const data: RequestData = {};
  if (query) {
    data.s = query;
  }
  if (recentlyBrowsed) {
    data.recentlyBrowsed = recentlyBrowsed.join();
  }
  if (more) {
    data.more = more;
  }
  return getJSON('/api/components/suggestions', data);
}

export function getComponentForSourceViewer(component: string, branch?: string): Promise<any> {
  return getJSON('/api/components/app', { component, branch });
}

export function getSources(
  component: string,
  from?: number,
  to?: number,
  branch?: string
): Promise<any> {
  const data: RequestData = { key: component, branch };
  if (from) {
    Object.assign(data, { from });
  }
  if (to) {
    Object.assign(data, { to });
  }
  return getJSON('/api/sources/lines', data).then(r => r.sources);
}

export function getDuplications(component: string, branch?: string): Promise<any> {
  return getJSON('/api/duplications/show', { key: component, branch });
}

export function getTests(component: string, line: number | string, branch?: string): Promise<any> {
  const data = { sourceFileKey: component, sourceFileLineNumber: line, branch };
  return getJSON('/api/tests/list', data).then(r => r.tests);
}
