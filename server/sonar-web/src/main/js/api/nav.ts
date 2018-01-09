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
import { getJSON, parseJSON, request } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export function getGlobalNavigation(): Promise<any> {
  return getJSON('/api/navigation/global');
}

export function getComponentNavigation(componentKey: string, branch?: string): Promise<any> {
  return getJSON('/api/navigation/component', { componentKey, branch }).catch(throwGlobalError);
}

export function getSettingsNavigation(): Promise<any> {
  return getJSON('/api/navigation/settings').catch(throwGlobalError);
}

export function tryGetGlobalNavigation(): Promise<any> {
  return request('/api/navigation/global')
    .submit()
    .then(response => {
      if (response.status >= 200 && response.status < 300) {
        return parseJSON(response);
      } else if (response.status === 401) {
        return {};
      } else {
        return Promise.reject(response);
      }
    })
    .catch(response => throwGlobalError({ response }).catch(() => Promise.resolve({})));
}
