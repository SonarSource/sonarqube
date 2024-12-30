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

import classNames from 'classnames';
import * as React from 'react';
import { NavLink } from 'react-router-dom';
import { MainMenu, MainMenuItem } from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { isMySet } from '../../../../apps/issues/utils';
import Link from '../../../../components/common/Link';
import { DEFAULT_ISSUES_QUERY } from '../../../../components/shared/utils';
import { translate } from '../../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../../helpers/urls';
import { AppState } from '../../../../types/appstate';
import { CurrentUser, LoggedInUser } from '../../../../types/users';
import withAppStateContext from '../../app-state/withAppStateContext';
import GlobalNavMore from './GlobalNavMore';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
  location: { pathname: string };
}

const ACTIVE_CLASS_NAME = 'active';

class GlobalNavMenu extends React.PureComponent<Props> {
  renderProjects() {
    const active =
      this.props.location.pathname.startsWith('/projects') &&
      this.props.location.pathname !== '/projects/create';

    return (
      <MainMenuItem>
        <Link
          aria-current={active ? 'page' : undefined}
          className={classNames({ active })}
          to="/projects"
        >
          {translate('projects.page')}
        </Link>
      </MainMenuItem>
    );
  }

  renderPortfolios() {
    return (
      <MainMenuItem>
        <NavLink className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')} to="/portfolios">
          {translate('portfolios.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  renderIssuesLink() {
    const search = (
      this.props.currentUser.isLoggedIn && isMySet()
        ? new URLSearchParams({ myIssues: 'true', ...DEFAULT_ISSUES_QUERY })
        : new URLSearchParams(DEFAULT_ISSUES_QUERY)
    ).toString();

    return (
      <MainMenuItem>
        <NavLink
          className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
          to={{ pathname: '/issues', search }}
        >
          {translate('myissues.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  /**
   * We will display the link only to the root user who are included into 'sonar-administrators' group
   * inside default org, and to the customer admin users.
   */
  renderAdministrationLink() {
    const { appState, currentUser } = this.props;

    if (currentUser.isLoggedIn) {
      const loggedInUser = currentUser as LoggedInUser;
      const isSonarAdminGroupAvailable = loggedInUser.groups.includes('sonar-administrators');

      if ((appState.canAdmin && isSonarAdminGroupAvailable) || (!appState.canAdmin && appState.canCustomerAdmin)) {
        return (
          <MainMenuItem>
            <NavLink
              data-guiding-id="mode-tour-1"
              className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
              to="/admin/settings"
            >
              {translate('layout.settings')}
            </NavLink>
          </MainMenuItem>
        );
      }
    }

    return null;
  }

  render() {
    const governanceInstalled = this.props.appState.qualifiers.includes(
      ComponentQualifier.Portfolio,
    );

    return (
      <nav aria-label={translate('global')}>
        <MainMenu>
          {this.renderProjects()}
          {governanceInstalled && this.renderPortfolios()}
          {this.renderIssuesLink()}
          {this.renderAdministrationLink()}
          <GlobalNavMore />
        </MainMenu>
      </nav>
    );
  }
}

export default withAppStateContext(GlobalNavMenu);
