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

const save = (key: string, value: ?string) => {
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

export const saveFavorite = () => save(PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE);
export const isFavoriteSet = (): boolean => {
  const setting = window.localStorage.getItem(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_FAVORITE;
};

export const saveAll = () => save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
export const isAllSet = (): boolean => {
  const setting = window.localStorage.getItem(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_ALL;
};

export const saveView = (view: ?string) => save(PROJECTS_VIEW, view);
export const getView = () => window.localStorage.getItem(PROJECTS_VIEW);

export const saveVisualization = (visualization: ?string) =>
  save(PROJECTS_VISUALIZATION, visualization);
export const getVisualization = () => window.localStorage.getItem(PROJECTS_VISUALIZATION);

export const saveSort = (sort: ?string) => save(PROJECTS_SORT, sort);
export const getSort = () => window.localStorage.getItem(PROJECTS_SORT);

export const saveGraph = (graph: ?string) => save(PROJECT_ACTIVITY_GRAPH, graph);
export const getGraph = () => window.localStorage.getItem(PROJECT_ACTIVITY_GRAPH) || 'overview';
