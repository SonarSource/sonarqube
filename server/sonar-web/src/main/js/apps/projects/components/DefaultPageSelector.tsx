/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { searchProjects } from '../../../api/components';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { get } from '../../../helpers/storage';
import { hasGlobalPermission } from '../../../helpers/users';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE } from '../utils';
import AllProjects from './AllProjects';

export interface DefaultPageSelectorProps {
  currentUser: CurrentUser;
}

export function DefaultPageSelector(props: DefaultPageSelectorProps) {
  const [checking, setChecking] = React.useState(true);
  const navigate = useNavigate();
  const location = useLocation();

  React.useEffect(
    () => {
      async function checkRedirect() {
        const { currentUser } = props;
        const setting = get(PROJECTS_DEFAULT_FILTER);

        // 1. Don't have to redirect if:
        //   1.1 User is anonymous
        //   1.2 There's a query, which means the user is interacting with the current page
        //   1.3 The last interaction with the filter was to set it to "all"
        if (
          !isLoggedIn(currentUser) ||
          Object.keys(location.query).length > 0 ||
          setting === PROJECTS_ALL
        ) {
          setChecking(false);
          return;
        }

        // 2. Redirect to the favorites page if:
        //   2.1 The last interaction with the filter was to set it to "favorites"
        //   2.2 The user has starred some projects
        if (
          setting === PROJECTS_FAVORITE ||
          (await searchProjects({ filter: 'isFavorite', ps: 1 })).paging.total > 0
        ) {
          navigate('/projects/favorite', { replace: true });
          return;
        }

        // 3. Redirect to the create project page if:
        //   3.1 The user has permission to provision projects, AND there are 0 projects on the instance
        if (
          hasGlobalPermission(currentUser, 'provisioning') &&
          (await searchProjects({ ps: 1 })).paging.total === 0
        ) {
          navigate('/projects/create', { replace: true });
          return;
        }

        // None of the above apply. Do not redirect, and stay on this page.
        setChecking(false);
      }

      checkRedirect();
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [
      /* run only once on mount*/
    ],
  );

  if (checking) {
    // We don't return a loader here, on purpose. We don't want to show anything
    // just yet.
    return null;
  }

  return <AllProjects isFavorite={false} />;
}

export default withCurrentUserContext(DefaultPageSelector);
