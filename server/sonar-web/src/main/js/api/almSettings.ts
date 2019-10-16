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
import { getJSON, post } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getAlmDefinitions(): Promise<T.AlmSettingsDefinitions> {
  return getJSON('/api/alm_settings/list_definitions').catch(throwGlobalError);
}

export function createGithubConfiguration(data: T.GithubDefinition) {
  return post('/api/alm_settings/create_github', data).catch(throwGlobalError);
}

export function updateGithubConfiguration(data: T.GithubDefinition & { newKey: string }) {
  return post('/api/alm_settings/update_github', data).catch(throwGlobalError);
}

export function deleteConfiguration(key: string) {
  return post('/api/alm_settings/delete', { key }).catch(throwGlobalError);
}

export function countBindedProjects(instance: string) {
  return getJSON('/api/alm_settings/count_binding', { instance }).catch(throwGlobalError);
}
