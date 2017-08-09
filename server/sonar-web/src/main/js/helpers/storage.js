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
const PROJECTS_DEFAULT_FILTER = 'sonarqube.projects.default';
const PROJECTS_FAVORITE = 'favorite';
const PROJECTS_ALL = 'all';

const PROJECTS_VIEW = 'sonarqube.projects.view';
const PROJECTS_VISUALIZATION = 'sonarqube.projects.visualization';
const PROJECTS_SORT = 'sonarqube.projects.sort';

const PROJECT_ACTIVITY_GRAPH = 'sonarqube.project_activity.graph';
const PROJECT_ACTIVITY_GRAPH_CUSTOM = 'sonarqube.project_activity.graph.custom';

const save = (key /*: string */, value /*: ?string */) => {
  try {
    if (value) {
      window.localStorage.setItem(key, value);
    } else {
      window.localStorage.removeItem(key);
    }
  } catch (e) {
    // usually that means the storage is full
    // just do nothing in this case
  }
};

export function saveFavorite() {
  save(PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE);
}
export function isFavoriteSet() /*: boolean */ {
  const setting = window.localStorage.getItem(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_FAVORITE;
}

export function saveAll() {
  save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
}
export function isAllSet() /*: boolean */ {
  const setting = window.localStorage.getItem(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_ALL;
}

export function saveView(view /*: ?string */) {
  save(PROJECTS_VIEW, view);
}
export function getView() {
  return window.localStorage.getItem(PROJECTS_VIEW);
}

export function saveVisualization(visualization /*: ?string */) {
  save(PROJECTS_VISUALIZATION, visualization);
}
export function getVisualization() {
  window.localStorage.getItem(PROJECTS_VISUALIZATION);
}

export function saveSort(sort /*: ?string */) {
  save(PROJECTS_SORT, sort);
}
export function getSort() {
  window.localStorage.getItem(PROJECTS_SORT);
}

export function saveCustomGraph(metrics /*: ?Array<string> */) {
  save(PROJECT_ACTIVITY_GRAPH_CUSTOM, metrics ? metrics.join(',') : '');
}
export function getCustomGraph() /*: Array<string> */ {
  const customGraphs = window.localStorage.getItem(PROJECT_ACTIVITY_GRAPH_CUSTOM);
  return customGraphs ? customGraphs.split(',') : [];
}

export function saveGraph(graph /*: ?string */) {
  save(PROJECT_ACTIVITY_GRAPH, graph);
}
export function getGraph() /*: string */ {
  window.localStorage.getItem(PROJECT_ACTIVITY_GRAPH) || 'issues';
}
