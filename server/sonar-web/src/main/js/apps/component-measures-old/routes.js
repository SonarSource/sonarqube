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
const routes = [
  {
    getComponent(_, callback) {
      import('./app/AppContainer').then(i => callback(null, i.default));
    },
    childRoutes: [
      {
        getComponent(_, callback) {
          import('./home/HomeContainer').then(i => callback(null, i.default));
        },
        childRoutes: [
          {
            getIndexRoute(_, callback) {
              import('./home/AllMeasuresContainer').then(i =>
                callback(null, { component: i.default })
              );
            }
          },
          {
            path: 'domain/:domainName',
            getComponent(_, callback) {
              import('./home/DomainMeasuresContainer').then(i => callback(null, i.default));
            }
          }
        ]
      },
      {
        path: 'metric/:metricKey',
        getComponent(_, callback) {
          import('./details/MeasureDetailsContainer').then(i => callback(null, i.default));
        },
        childRoutes: [
          {
            indexRoute: {
              onEnter(nextState, replace) {
                const { params, location } = nextState;
                replace({
                  pathname: `/component_measures_old/metric/${params.metricKey}/list`,
                  query: location.query
                });
              }
            }
          },
          {
            path: 'list',
            getComponent(_, callback) {
              import('./details/drilldown/ListViewContainer').then(i => callback(null, i.default));
            }
          },
          {
            path: 'tree',
            getComponent(_, callback) {
              import('./details/drilldown/TreeViewContainer').then(i => callback(null, i.default));
            }
          },
          {
            path: 'history',
            onEnter(nextState, replace) {
              replace({
                pathname: '/project/activity',
                query: {
                  id: nextState.location.query.id,
                  graph: 'custom',
                  custom_metrics: nextState.params.metricKey
                }
              });
            }
          },
          {
            path: 'treemap',
            getComponent(_, callback) {
              import('./details/treemap/MeasureTreemapContainer').then(i =>
                callback(null, i.default)
              );
            }
          }
        ]
      }
    ]
  }
];

export default routes;
