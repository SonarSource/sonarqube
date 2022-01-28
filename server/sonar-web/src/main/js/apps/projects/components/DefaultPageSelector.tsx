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
import * as React from 'react';
import { searchProjects } from '../../../api/components';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { get } from '../../../helpers/storage';
import { hasGlobalPermission, isLoggedIn } from '../../../helpers/users';
import { CurrentUser } from '../../../types/types';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER, PROJECTS_FAVORITE } from '../utils';
import AllProjectsContainer from './AllProjectsContainer';

interface Props {
  currentUser: CurrentUser;
  location: Pick<Location, 'pathname' | 'query'>;
  router: Pick<Router, 'replace'>;
}

interface State {
  checking: boolean;
}

export class DefaultPageSelector extends React.PureComponent<Props, State> {
  state: State = { checking: true };

  componentDidMount() {
    this.checkIfNeedsRedirecting();
  }

  checkIfNeedsRedirecting = async () => {
    const { currentUser, router, location } = this.props;
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
      this.setState({ checking: false });
      return;
    }

    // 2. Redirect to the favorites page if:
    //   2.1 The last interaction with the filter was to set it to "favorites"
    //   2.2 The user has starred some projects
    if (
      setting === PROJECTS_FAVORITE ||
      (await searchProjects({ filter: 'isFavorite', ps: 1 })).paging.total > 0
    ) {
      router.replace('/projects/favorite');
      return;
    }

    // 3. Redirect to the create project page if:
    //   3.1 The user has permission to provision projects, AND there are 0 projects on the instance
    if (
      hasGlobalPermission(currentUser, 'provisioning') &&
      (await searchProjects({ ps: 1 })).paging.total === 0
    ) {
      this.props.router.replace('/projects/create');
    }

    // None of the above apply. Do not redirect, and stay on this page.
    this.setState({ checking: false });
  };

  render() {
    const { checking } = this.state;

    if (checking) {
      // We don't return a loader here, on purpose. We don't want to show anything
      // just yet.
      return null;
    }

    return <AllProjectsContainer isFavorite={false} />;
  }
}

export default withCurrentUser(withRouter(DefaultPageSelector));
