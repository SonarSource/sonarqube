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
// @flow
import { parseQuery, areMyIssuesSelected, serializeQuery } from './utils';
/*:: import type { RawQuery } from '../../helpers/query'; */

function parseHash(hash /*: string */) /*: RawQuery */ {
  const query /*: RawQuery */ = {};
  const parts = hash.split('|');
  parts.forEach(part => {
    const tokens = part.split('=');
    if (tokens.length === 2) {
      const property = decodeURIComponent(tokens[0]);
      const value = decodeURIComponent(tokens[1]);
      if (property === 'assigned_to_me' && value === 'true') {
        query.myIssues = 'true';
      } else {
        query[property] = value;
      }
    }
  });
  return query;
}

export function onEnter(state /*: Object */, replace /*: Function */) {
  const { hash } = window.location;
  if (hash.length > 1) {
    const query = parseHash(hash.substr(1));
    const normalizedQuery = {
      ...serializeQuery(parseQuery(query)),
      myIssues: areMyIssuesSelected(query) ? 'true' : undefined
    };
    replace({
      pathname: state.location.pathname,
      query: normalizedQuery
    });
  }
}
