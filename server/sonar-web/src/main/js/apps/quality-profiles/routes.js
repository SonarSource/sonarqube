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
import { withRouter } from 'react-router';

const routes = [
  {
    getComponent(state, callback) {
      import('./components/AppContainer').then(i => i.default).then(AppContainer => {
        if (state.params.organizationKey) {
          callback(null, AppContainer);
        } else {
          import('../organizations/forSingleOrganization')
            .then(i => i.default)
            .then(forSingleOrganization => callback(null, forSingleOrganization(AppContainer)));
        }
      });
    },
    getIndexRoute(_, callback) {
      import('./home/HomeContainer').then(i => callback(null, { component: i.default }));
    },
    childRoutes: [
      {
        getComponent(_, callback) {
          import('./components/ProfileContainer').then(i => callback(null, withRouter(i.default)));
        },
        childRoutes: [
          {
            path: 'show',
            getComponent(_, callback) {
              import('./details/ProfileDetails').then(i => callback(null, i.default));
            }
          },
          {
            path: 'changelog',
            getComponent(_, callback) {
              import('./changelog/ChangelogContainer').then(i => callback(null, i.default));
            }
          },
          {
            path: 'compare',
            getComponent(_, callback) {
              import('./compare/ComparisonContainer').then(i => callback(null, i.default));
            }
          }
        ]
      }
    ]
  }
];

export default routes;
