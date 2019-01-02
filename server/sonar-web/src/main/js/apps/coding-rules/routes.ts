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
import { RouterState, RedirectFunction } from 'react-router';
import { parseQuery, serializeQuery } from './query';
import { lazyLoad } from '../../components/lazyLoad';
import { RawQuery } from '../../helpers/query';

function parseHash(hash: string): RawQuery {
  const query: RawQuery = {};
  const parts = hash.split('|');
  parts.forEach(part => {
    const tokens = part.split('=');
    if (tokens.length === 2) {
      query[decodeURIComponent(tokens[0])] = decodeURIComponent(tokens[1]);
    }
  });
  return query;
}

const routes = [
  {
    indexRoute: {
      onEnter: (nextState: RouterState, replace: RedirectFunction) => {
        const { hash } = window.location;
        if (hash.length > 1) {
          const query = parseHash(hash.substr(1));
          const normalizedQuery = {
            ...serializeQuery(parseQuery(query)),
            open: query.open
          };
          replace({ pathname: nextState.location.pathname, query: normalizedQuery });
        }
      },
      component: lazyLoad(() => import('./components/App'))
    }
  }
];

export default routes;
