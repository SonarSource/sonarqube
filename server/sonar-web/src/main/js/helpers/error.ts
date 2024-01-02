/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { addGlobalErrorMessage } from './globalMessages';
import { parseError } from './request';

export function throwGlobalError(param: Response | any): Promise<Response | any> {
  if (param.response instanceof Response) {
    /* eslint-disable-next-line no-console */
    console.warn('DEPRECATED: response should not be wrapped, pass it directly.');
    param = param.response;
  }

  if (param instanceof Response) {
    return parseError(param)
      .then(addGlobalErrorMessage, () => {
        /* ignore parsing errors */
      })
      .then(() => Promise.reject(param));
  }

  // Axios response object
  if (param.data?.message) {
    addGlobalErrorMessage(param.data?.message);
    return Promise.reject(param);
  }

  return Promise.reject(param);
}
