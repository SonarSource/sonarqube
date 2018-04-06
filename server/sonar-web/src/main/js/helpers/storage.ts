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
const PROJECTS_DEFAULT_FILTER = 'sonarqube.projects.default';
const PROJECTS_FAVORITE = 'favorite';
const PROJECTS_ALL = 'all';

const PROJECTS_VIEW = 'sonarqube.projects.view';
const PROJECTS_VISUALIZATION = 'sonarqube.projects.visualization';
const PROJECTS_SORT = 'sonarqube.projects.sort';

const PROJECT_ACTIVITY_GRAPH = 'sonarqube.project_activity.graph';
const PROJECT_ACTIVITY_GRAPH_CUSTOM = 'sonarqube.project_activity.graph.custom';

function save(key: string, value?: string, suffix?: string): void {
  try {
    const finalKey = suffix ? `${key}.${suffix}` : key;
    if (value) {
      window.localStorage.setItem(finalKey, value);
    } else {
      window.localStorage.removeItem(finalKey);
    }
  } catch (e) {
    // usually that means the storage is full
    // just do nothing in this case
  }
}

function get(key: string, suffix?: string): string | null {
  return window.localStorage.getItem(suffix ? `${key}.${suffix}` : key);
}

export function saveFavorite(): void {
  save(PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE);
}

export function isFavoriteSet(): boolean {
  const setting = get(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_FAVORITE;
}

export function saveAll(): void {
  save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
}

export function isAllSet(): boolean {
  const setting = get(PROJECTS_DEFAULT_FILTER);
  return setting === PROJECTS_ALL;
}

export function saveView(view?: string, suffix?: string): void {
  save(PROJECTS_VIEW, view, suffix);
}

export function getView(suffix?: string): string | null {
  return get(PROJECTS_VIEW, suffix);
}

export function saveVisualization(visualization?: string, suffix?: string): void {
  save(PROJECTS_VISUALIZATION, visualization, suffix);
}

export function getVisualization(suffix?: string): string | null {
  return get(PROJECTS_VISUALIZATION, suffix);
}

export function saveSort(sort?: string, suffix?: string): void {
  save(PROJECTS_SORT, sort, suffix);
}

export function getSort(suffix?: string): string | null {
  return get(PROJECTS_SORT, suffix);
}

export function saveCustomGraph(metrics?: string[]): void {
  save(PROJECT_ACTIVITY_GRAPH_CUSTOM, metrics ? metrics.join(',') : '');
}

export function getCustomGraph(): string[] {
  const customGraphs = get(PROJECT_ACTIVITY_GRAPH_CUSTOM);
  return customGraphs ? customGraphs.split(',') : [];
}

export function saveGraph(graph?: string): void {
  save(PROJECT_ACTIVITY_GRAPH, graph);
}

export function getGraph(): string {
  return get(PROJECT_ACTIVITY_GRAPH) || 'issues';
}
