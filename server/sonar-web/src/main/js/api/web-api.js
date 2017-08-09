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
import { getJSON } from '../helpers/request';

/*::
export type Param = {
  key: string,
  defaultValue?: string,
  description: string,
  deprecatedKey?: string,
  deprecatedKeySince?: string,
  deprecatedSince?: string,
  exampleValue?: string,
  internal: boolean,
  possibleValues?: Array<string>,
  required: boolean
};
*/

/*::
export type Action = {
  key: string,
  description: string,
  deprecatedSince?: string,
  since?: string,
  internal: boolean,
  post: boolean,
  hasResponseExample: boolean,
  changelog: Array<{
    version: string,
    description: string
  }>,
  params?: Array<Param>
};
*/

/*::
export type Domain = {
  actions: Array<Action>,
  description: string,
  deprecated: boolean,
  internal: boolean,
  path: string
};
*/

export function fetchWebApi(showInternal /*: boolean */ = true) /*: Promise<Array<Domain>> */ {
  const url = '/api/webservices/list';
  const data = { include_internals: showInternal };

  return getJSON(url, data).then(r =>
    r.webServices.map(domain => {
      const deprecated = !domain.actions.find(action => !action.deprecatedSince);
      const internal = !domain.actions.find(action => !action.internal);

      return { ...domain, deprecated, internal };
    })
  );
}

export function fetchResponseExample(
  domain /*: string */,
  action /*: string */
) /*: Promise<{ example: string }> */ {
  const url = '/api/webservices/response_example';
  const data = { controller: domain, action };

  return getJSON(url, data);
}
