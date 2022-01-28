/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { RedirectFunction, RouterState } from 'react-router';
import { lazyLoadComponent } from '../../components/lazyLoadComponent';
import { save } from '../../helpers/storage';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER } from './utils';

const routes = [
  {
    indexRoute: { component: lazyLoadComponent(() => import('./components/DefaultPageSelector')) }
  },
  {
    path: 'all',
    onEnter(_: RouterState, replace: RedirectFunction) {
      save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);
      replace('/projects');
    }
  },
  {
    path: 'favorite',
    component: lazyLoadComponent(() => import('./components/FavoriteProjectsContainer'))
  },
  {
    path: 'create',
    component: lazyLoadComponent(() => import('../create/project/CreateProjectPage'))
  }
];

export default routes;
