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
import { getJSON, postJSON, post } from '../helpers/request.js';

export function getComponents (data) {
  const url = '/api/components/search';
  return getJSON(url, data);
}

export function getProvisioned (data) {
  const url = '/api/projects/provisioned';
  return getJSON(url, data);
}

export function getGhosts (data) {
  const url = '/api/projects/ghosts';
  return getJSON(url, data);
}

export function deleteComponents (data) {
  const url = '/api/projects/bulk_delete';
  return post(url, data);
}

export function deleteProject (key) {
  const url = '/api/projects/delete';
  const data = { key };
  return post(url, data);
}

export function createProject (data) {
  const url = '/api/projects/create';
  return postJSON(url, data);
}

export function getComponentTree (strategy, componentKey, metrics = [], additional = {}) {
  const url = '/api/measures/component_tree';
  const data = Object.assign({}, additional, {
    baseComponentKey: componentKey,
    metricKeys: metrics.join(','),
    strategy
  });
  return getJSON(url, data);
}

export function getChildren (componentKey, metrics, additional) {
  return getComponentTree('children', componentKey, metrics, additional);
}

export function getComponentLeaves (componentKey, metrics, additional) {
  return getComponentTree('leaves', componentKey, metrics, additional);
}

export function getComponent (componentKey, metrics = []) {
  const url = '/api/measures/component';
  const data = { componentKey, metricKeys: metrics.join(',') };
  return getJSON(url, data).then(r => r.component);
}

export function getTree (baseComponentKey, options = {}) {
  const url = '/api/components/tree';
  const data = Object.assign({}, options, { baseComponentKey });
  return getJSON(url, data);
}

export function getParents ({ id, key }) {
  const url = '/api/components/show';
  const data = id ? { id } : { key };
  return getJSON(url, data).then(r => r.ancestors);
}

export function getBreadcrumbs ({ id, key }) {
  const url = '/api/components/show';
  const data = id ? { id } : { key };
  return getJSON(url, data).then(r => {
    const reversedAncestors = [...r.ancestors].reverse();
    return [...reversedAncestors, r.component];
  });
}

export function getProjectsWithInternalId (query) {
  const url = '/api/resources/search';
  const data = {
    f: 's2',
    q: 'TRK',
    s: query
  };
  return getJSON(url, data).then(r => r.results);
}

export function getMyProjects (data) {
  const url = '/api/projects/search_my_projects';
  return getJSON(url, data);
}

export function searchProjects (data) {
  const url = '/api/components/search_projects';
  return getJSON(url, data);
}

/**
 * Change component's key
 * @param {string} key
 * @param {string} newKey
 * @returns {Promise}
 */
export function changeKey (key, newKey) {
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
export function bulkChangeKey (key, from, to, dryRun = false) {
  const url = '/api/components/bulk_update_key';
  const data = { key, from, to, dryRun };
  return postJSON(url, data);
}
