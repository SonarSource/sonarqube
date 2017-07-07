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
      require.ensure([], require => callback(null, require('./app/AppContainer').default));
    },
    childRoutes: [
      {
        getComponent(_, callback) {
          require.ensure([], require => callback(null, require('./home/HomeContainer').default));
        },
        childRoutes: [
          {
            getIndexRoute(_, callback) {
              require.ensure([], require =>
                callback(null, { component: require('./home/AllMeasuresContainer').default })
              );
            }
          },
          {
            path: 'domain/:domainName',
            getComponent(_, callback) {
              require.ensure([], require =>
                callback(null, require('./home/DomainMeasuresContainer').default)
              );
            }
          }
        ]
      },
      {
        path: 'metric/:metricKey',
        getComponent(_, callback) {
          require.ensure([], require =>
            callback(null, require('./details/MeasureDetailsContainer').default)
          );
        },
        childRoutes: [
          {
            indexRoute: {
              onEnter(nextState, replace) {
                const { params, location } = nextState;
                replace({
                  pathname: `/component_measures/metric/${params.metricKey}/list`,
                  query: location.query
                });
              }
            }
          },
          {
            path: 'list',
            getComponent(_, callback) {
              require.ensure([], require =>
                callback(null, require('./details/drilldown/ListViewContainer').default)
              );
            }
          },
          {
            path: 'tree',
            getComponent(_, callback) {
              require.ensure([], require =>
                callback(null, require('./details/drilldown/TreeViewContainer').default)
              );
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
              require.ensure([], require =>
                callback(null, require('./details/treemap/MeasureTreemapContainer').default)
              );
            }
          }
        ]
      }
    ]
  }
];

export default routes;
