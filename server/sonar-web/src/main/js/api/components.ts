/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON, post, postJSON, RequestData } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface BaseSearchProjectsParameters {
  analyzedBefore?: string;
  onProvisionedOnly?: boolean;
  organization: string;
  projects?: string;
  q?: string;
  qualifiers?: string;
  visibility?: T.Visibility;
}

export interface ProjectBase {
  key: string;
  name: string;
  qualifier: string;
  visibility: T.Visibility;
}

export interface Project extends ProjectBase {
  id: string;
  lastAnalysisDate?: string;
  organization: string;
}

export interface SearchProjectsParameters extends BaseSearchProjectsParameters {
  p?: number;
  ps?: number;
}

export function getComponents(
  parameters: SearchProjectsParameters
): Promise<{
  components: Project[];
  paging: T.Paging;
}> {
  return getJSON('/api/projects/search', parameters);
}

export function bulkDeleteProjects(
  parameters: BaseSearchProjectsParameters
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
  name: string;
  project: string;
  organization?: string;
  visibility?: T.Visibility;
}): Promise<{ project: ProjectBase }> {
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
  component: string,
  metrics: string[] = [],
  additional: RequestData = {}
): Promise<{
  baseComponent: T.ComponentMeasure;
  components: T.ComponentMeasure[];
  metrics: T.Metric[];
  paging: T.Paging;
}> {
  const url = '/api/measures/component_tree';
  const data = { ...additional, component, metricKeys: metrics.join(','), strategy };
  return getJSON(url, data).catch(throwGlobalError);
}

export function getChildren(
  component: string,
  metrics: string[] = [],
  additional: RequestData = {}
) {
  return getComponentTree('children', component, metrics, additional);
}

export function getComponentLeaves(
  component: string,
  metrics: string[] = [],
  additional: RequestData = {}
) {
  return getComponentTree('leaves', component, metrics, additional);
}

export function getComponent(
  data: { component: string; metricKeys: string } & T.BranchParameters
): Promise<{ component: T.ComponentMeasure }> {
  return getJSON('/api/measures/component', data);
}

export interface TreeComponent extends T.LightComponent {
  id?: string;
  name: string;
  path?: string;
  refId?: string;
  refKey?: string;
  tags?: string[];
  visibility: T.Visibility;
}

export interface TreeComponentWithPath extends TreeComponent {
  path: string;
}

type GetTreeParams = {
  asc?: boolean;
  component: string;
  p?: number;
  ps?: number;
  q?: string;
  s?: string;
  strategy?: 'all' | 'leaves' | 'children';
} & T.BranchParameters;

export function getTree<T = TreeComponent>(
  data: GetTreeParams & { qualifiers?: string }
): Promise<{ baseComponent: TreeComponent; components: T[]; paging: T.Paging }> {
  return getJSON('/api/components/tree', data).catch(throwGlobalError);
}

export function getFiles(data: GetTreeParams) {
  return getTree<TreeComponentWithPath>({ ...data, qualifiers: 'FIL' });
}

export function getDirectories(data: GetTreeParams) {
  return getTree<TreeComponentWithPath>({ ...data, qualifiers: 'DIR' });
}

export function getComponentData(data: { component: string } & T.BranchParameters): Promise<any> {
  return getJSON('/api/components/show', data);
}

export function doesComponentExists(
  data: { component: string } & T.BranchParameters
): Promise<boolean> {
  return getComponentData(data).then(({ component }) => component !== undefined, () => false);
}

export function getComponentShow(data: { component: string } & T.BranchParameters): Promise<any> {
  return getComponentData(data).catch(throwGlobalError);
}

export function getParents(component: string): Promise<any> {
  return getComponentShow({ component }).then(r => r.ancestors);
}

export function getBreadcrumbs(data: { component: string } & T.BranchParameters): Promise<any> {
  return getComponentShow(data).then(r => {
    const reversedAncestors = [...r.ancestors].reverse();
    return [...reversedAncestors, r.component];
  });
}

export function getMyProjects(data: {
  p?: number;
  ps?: number;
}): Promise<{ paging: T.Paging; projects: T.MyProject[] }> {
  return getJSON('/api/projects/search_my_projects', data);
}

export interface Component {
  organization: string;
  id: string;
  key: string;
  name: string;
  isFavorite?: boolean;
  analysisDate?: string;
  tags: string[];
  visibility: T.Visibility;
  leakPeriodDate?: string;
}

export interface Facet {
  property: string;
  values: Array<{ val: string; count: number }>;
}

export function searchProjects(
  data: RequestData
): Promise<{
  components: Component[];
  facets: Facet[];
  organizations: Array<{ key: string; name: string }>;
  paging: T.Paging;
}> {
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

export function changeKey(data: { from: string; to: string }) {
  return post('/api/projects/update_key', data).catch(throwGlobalError);
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
  return getJSON('/api/components/suggestions', data).catch(throwGlobalError);
}

export function getComponentForSourceViewer(
  data: { component: string } & T.BranchParameters
): Promise<T.SourceViewerFile> {
  return getJSON('/api/components/app', data);
}

export function getSources(
  data: { key: string; from?: number; to?: number } & T.BranchParameters
): Promise<T.SourceLine[]> {
  return getJSON('/api/sources/lines', data).then(r => r.sources);
}

export function getDuplications(
  data: { key: string } & T.BranchParameters
): Promise<{ duplications: T.Duplication[]; files: T.Dict<T.DuplicatedFile> }> {
  return getJSON('/api/duplications/show', data).catch(throwGlobalError);
}

export function getTests(
  data: { sourceFileKey: string; sourceFileLineNumber: number | string } & T.BranchParameters
): Promise<any> {
  return getJSON('/api/tests/list', data).then(r => r.tests);
}
