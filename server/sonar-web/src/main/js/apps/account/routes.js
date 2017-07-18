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
      import('./components/Account').then(i => callback(null, i.default));
    },
    childRoutes: [
      {
        getIndexRoute(_, callback) {
          import('./profile/Profile').then(i => callback(null, { component: i.default }));
        }
      },
      {
        path: 'security',
        getComponent(_, callback) {
          import('./components/Security').then(i => callback(null, i.default));
        }
      },
      {
        path: 'projects',
        getComponent(_, callback) {
          import('./projects/ProjectsContainer').then(i => callback(null, i.default));
        }
      },
      {
        path: 'notifications',
        getComponent(_, callback) {
          import('./notifications/Notifications').then(i => callback(null, i.default));
        }
      },
      {
        path: 'organizations',
        getComponent(_, callback) {
          import('./organizations/UserOrganizations').then(i => callback(null, i.default));
        },
        childRoutes: [
          {
            path: 'create',
            getComponent(_, callback) {
              import('./organizations/CreateOrganizationForm').then(i => callback(null, i.default));
            }
          }
        ]
      }
    ]
  }
];

export default routes;
