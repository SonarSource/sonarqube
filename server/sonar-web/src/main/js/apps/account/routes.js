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
      require.ensure([], require => callback(null, require('./components/Account').default));
    },
    childRoutes: [
      {
        getIndexRoute(_, callback) {
          require.ensure([], require =>
            callback(null, { component: require('./profile/Profile').default }));
        }
      },
      {
        path: 'security',
        getComponent(_, callback) {
          require.ensure([], require => callback(null, require('./components/Security').default));
        }
      },
      {
        path: 'projects',
        getComponent(_, callback) {
          require.ensure([], require =>
            callback(null, require('./projects/ProjectsContainer').default));
        }
      },
      {
        path: 'notifications',
        getComponent(_, callback) {
          require.ensure([], require =>
            callback(null, require('./notifications/Notifications').default));
        }
      },
      {
        path: 'organizations',
        getComponent(_, callback) {
          require.ensure([], require =>
            callback(null, require('./organizations/UserOrganizations').default));
        },
        childRoutes: [
          {
            path: 'create',
            getComponent(_, callback) {
              require.ensure([], require =>
                callback(null, require('./organizations/CreateOrganizationForm').default));
            }
          }
        ]
      }
    ]
  }
];

export default routes;
