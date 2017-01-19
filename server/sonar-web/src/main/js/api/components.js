/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

export function getComponents (data?: Object) {
  const url = '/api/components/search';
  return getJSON(url, data);
}

export function getProvisioned (data?: Object) {
  const url = '/api/projects/provisioned';
  return getJSON(url, data);
}

export function getGhosts (data?: Object) {
  const url = '/api/projects/ghosts';
  return getJSON(url, data);
}

export function deleteComponents (data?: Object) {
  const url = '/api/projects/bulk_delete';
  return post(url, data);
}

export function deleteProject (key: string) {
  const url = '/api/projects/delete';
  const data = { key };
  return post(url, data);
}

export function createProject (data?: Object) {
  const url = '/api/projects/create';
  return postJSON(url, data);
}

export function getComponentTree (
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

export function getChildren (componentKey: string, metrics?: Array<string>, additional?: Object) {
  return getComponentTree('children', componentKey, metrics, additional);
}

export function getComponentLeaves (componentKey: string, metrics?: Array<string>, additional?: Object) {
  return getComponentTree('leaves', componentKey, metrics, additional);
}

export function getComponent (componentKey: string, metrics: Array<string> = []) {
  const url = '/api/measures/component';
  const data = { componentKey, metricKeys: metrics.join(',') };
  return getJSON(url, data).then(r => r.component);
}

export function getTree (baseComponentKey: string, options?: Object = {}) {
  const url = '/api/components/tree';
  const data = Object.assign({}, options, { baseComponentKey });
  return getJSON(url, data);
}

export function getParents ({ id, key }: { id: string, key: string }) {
  const url = '/api/components/show';
  const data = id ? { id } : { key };
  return getJSON(url, data).then(r => r.ancestors);
}

export function getBreadcrumbs ({ id, key }: { id: string, key: string }) {
  const url = '/api/components/show';
  const data = id ? { id } : { key };
  return getJSON(url, data).then(r => {
    const reversedAncestors = [...r.ancestors].reverse();
    return [...reversedAncestors, r.component];
  });
}

export function getMyProjects (data?: Object) {
  const url = '/api/projects/search_my_projects';
  return getJSON(url, data);
}

export function searchProjects (data?: Object) {
  const url = '/api/components/search_projects';
  return getJSON(url, data);
}

/**
 * Change component's key
 * @param {string} key
 * @param {string} newKey
 * @returns {Promise}
 */
export function changeKey (key: string, newKey: string) {
  const url = '/api/components/update_key';
  const data = { key, newKey };
  return post(url, data);
}

/**
 * Bulk change component's key
 * @param {string} key
 * @param {string} from
 * @param {string} to
 * @param {boolean} dryRun
 * @returns {Promise}
 */
export function bulkChangeKey (key: string, from: string, to: string, dryRun?: boolean = false) {
  const url = '/api/components/bulk_update_key';
  const data = { key, from, to, dryRun };
  return postJSON(url, data);
}

export const getSuggestions = (query: string): Promise<Object> => (
    getJSON('/api/components/suggestions', { s: query })
);
