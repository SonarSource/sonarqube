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
import { RouterState, IndexRouteProps, RedirectFunction } from 'react-router';

const routes = [
  {
    getIndexRoute(_: RouterState, callback: (err: any, route: IndexRouteProps) => any) {
      import('./components/AppContainer').then(i => callback(null, { component: i.default }));
    }
  },
  {
    path: 'domain/:domainName',
    onEnter(nextState: RouterState, replace: RedirectFunction) {
      replace({
        pathname: '/component_measures',
        query: {
          ...nextState.location.query,
          metric: nextState.params.domainName
        }
      });
    }
  },
  {
    path: 'metric/:metricKey(/:view)',
    onEnter(nextState: RouterState, replace: RedirectFunction) {
      if (nextState.params.view === 'history') {
        replace({
          pathname: '/project/activity',
          query: {
            id: nextState.location.query.id,
            graph: 'custom',
            custom_metrics: nextState.params.metricKey
          }
        });
      } else {
        replace({
          pathname: '/component_measures',
          query: {
            ...nextState.location.query,
            metric: nextState.params.metricKey,
            view: nextState.params.view
          }
        });
      }
    }
  }
];

export default routes;
