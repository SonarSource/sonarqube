/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';

const routes = [
  {
    path: 'new',
    component: lazyLoadComponent(() => import('./components/LoginContainer'))
  },
  {
    path: 'sso',
    component: lazyLoadComponent(() => import('./components/SamlLogin'))
  },
  {
    path: 'logout',
    component: lazyLoadComponent(() => import('./components/Logout'))
  },
  {
    path: 'unauthorized',
    component: lazyLoadComponent(() => import('./components/Unauthorized'))
  },
  {
    path: 'email_already_exists',
    component: lazyLoadComponent(() => import('./components/EmailAlreadyExists'))
  }
];

export default routes;
