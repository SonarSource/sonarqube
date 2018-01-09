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
import { RouterState, IndexRouteProps, RouteComponent } from 'react-router';

const routes = [
  {
    getComponent(_: RouterState, callback: (err: any, component: RouteComponent) => any) {
      import('./components/Account').then(i => callback(null, i.default));
    },
    childRoutes: [
      {
        getIndexRoute(_: RouterState, callback: (err: any, route: IndexRouteProps) => any) {
          import('./profile/Profile').then(i => callback(null, { component: i.default }));
        }
      },
      {
        path: 'security',
        getComponent(_: RouterState, callback: (err: any, component: RouteComponent) => any) {
          import('./components/Security').then(i => callback(null, i.default));
        }
      },
      {
        path: 'projects',
        getComponent(_: RouterState, callback: (err: any, component: RouteComponent) => any) {
          import('./projects/ProjectsContainer').then(i => callback(null, i.default));
        }
      },
      {
        path: 'notifications',
        getComponent(_: RouterState, callback: (err: any, component: RouteComponent) => any) {
          import('./notifications/Notifications').then(i => callback(null, i.default));
        }
      },
      {
        path: 'organizations',
        getComponent(_: RouterState, callback: (err: any, component: RouteComponent) => any) {
          import('./organizations/UserOrganizations').then(i => callback(null, i.default));
        }
      }
    ]
  }
];

export default routes;
