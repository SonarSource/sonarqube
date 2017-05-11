/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { getJSON, postJSON, post } from '../helpers/request';

export function getComponents(data?: Object) {
  const url = '/api/projects/search';
  return getJSON(url, data);
}

export function getProvisioned(data?: Object) {
  const url = '/api/projects/provisioned';
  return getJSON(url, data);
}

export function getGhosts(data?: Object) {
  const url = '/api/projects/ghosts';
  return getJSON(url, data);
}

export function deleteComponents(data: { projects: string, organization?: string }) {
  const url = '/api/projects/bulk_delete';
  return post(url, data);
}

export function deleteProject(project: string) {
  const url = '/api/projects/delete';
  const data = { project };
  return post(url, data);
}

export function createProject(
  data: {
    branch?: string,
    name: string,
    project: string,
    organization?: string
  }
) {
  const url = '/api/projects/create';
  return postJSON(url, data);
}

export function searchProjectTags(data?: { ps?: number, q?: string }) {
  const url = '/api/project_tags/search';
  return getJSON(url, data);
}

export function setProjectTags(data: { project: string, tags: string }) {
  const url = '/api/project_tags/set';
  return post(url, data);
}

export function getComponentTree(
  strategy: string,
  componentKey: string,
  metrics: Array<string> = [],
  additional?: Object = {}
) {
  const url = '/api/measures/component_tree';
  const data = Object.assign({}, additional, {
    baseComponentKey: componentKey,
    metricKeys: metrics.join(','),
    strategy
  });
  return getJSON(url, data);
}

export function getChildren(componentKey: string, metrics?: Array<string>, additional?: Object) {
  return getComponentTree('children', componentKey, metrics, additional);
}

export function getComponentLeaves(
  componentKey: string,
  metrics?: Array<string>,
  additional?: Object
) {
  return getComponentTree('leaves', componentKey, metrics, additional);
}

export function getComponent(componentKey: string, metrics: Array<string> = []) {
  const url = '/api/measures/component';
  const data = { componentKey, metricKeys: metrics.join(',') };
  return getJSON(url, data).then(r => r.component);
}

export function getTree(component: string, options?: Object = {}) {
  const url = '/api/components/tree';
  const data = { ...options, component };
  return getJSON(url, data);
}

export function getComponentShow(component: string) {
  const url = '/api/components/show';
  return getJSON(url, { component });
}

export function getParents(component: string) {
  return getComponentShow(component).then(r => r.ancestors);
}

export function getBreadcrumbs(component: string) {
  return getComponentShow(component).then(r => {
    const reversedAncestors = [...r.ancestors].reverse();
    return [...reversedAncestors, r.component];
  });
}

export function getComponentTags(component: string) {
  return getComponentShow(component).then(r => r.component.tags || []);
}

export function getMyProjects(data?: Object) {
  const url = '/api/projects/search_my_projects';
  return getJSON(url, data);
}

export function searchProjects(data?: Object) {
  const url = '/api/components/search_projects';
  return getJSON(url, data);
}

export const searchComponents = (data?: { q?: string, qualifiers?: string, ps?: number }) =>
  getJSON('/api/components/search', data);

/**
 * Change component's key
 * @param {string} from
 * @param {string} to
 * @returns {Promise}
 */
export function changeKey(from: string, to: string) {
  const url = '/api/projects/update_key';
  const data = { from, to };
  return post(url, data);
}

/**
 * Bulk change component's key
 * @param {string} project
 * @param {string} from
 * @param {string} to
 * @param {boolean} dryRun
 * @returns {Promise}
 */
export function bulkChangeKey(project: string, from: string, to: string, dryRun?: boolean = false) {
  const url = '/api/projects/bulk_update_key';
  const data = { project, from, to, dryRun };
  return postJSON(url, data);
}

export type SuggestionsResponse = {
  organizations: Array<{
    key: string,
    name: string
  }>,
  projects: Array<{
    key: string,
    name: string
  }>,
  results: Array<{
    items: Array<{
      isFavorite: boolean,
      isRecentlyBrowsed: boolean,
      key: string,
      match: string,
      name: string,
      organization: string,
      project: string
    }>,
    more: number,
    q: string
  }>,
  warning?: string
};

export const getSuggestions = (
  query?: string,
  recentlyBrowsed?: Array<string>,
  more?: string
): Promise<SuggestionsResponse> => {
  const data: Object = {};
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
};

export const getComponentForSourceViewer = (component: string): Promise<*> =>
  getJSON('/api/components/app', { component });

export const getSources = (component: string, from?: number, to?: number): Promise<Array<*>> => {
  const data: Object = { key: component };
  if (from) {
    Object.assign(data, { from });
  }
  if (to) {
    Object.assign(data, { to });
  }
  return getJSON('/api/sources/lines', data).then(r => r.sources);
};

export const getDuplications = (component: string): Promise<*> =>
  getJSON('/api/duplications/show', { key: component });

export const getTests = (component: string, line: number | string): Promise<*> =>
  getJSON('/api/tests/list', { sourceFileKey: component, sourceFileLineNumber: line }).then(
    r => r.tests
  );
