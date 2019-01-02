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
import getStore from './getStore';
import { parseError } from '../../helpers/request';
import { addGlobalErrorMessage } from '../../store/globalMessages';

export default function throwGlobalError(error: { response: Response }): Promise<Response> {
  const store = getStore();

  // eslint-disable-next-line promise/no-promise-in-callback
  parseError(error)
    .then(message => {
      store.dispatch(addGlobalErrorMessage(message));
    })
    .catch(() => {});

  return Promise.reject(error.response);
}
