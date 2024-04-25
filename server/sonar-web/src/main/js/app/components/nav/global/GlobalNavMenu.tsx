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
import { MainMenu, MainMenuItem } from 'design-system';
import * as React from 'react';
import { NavLink } from 'react-router-dom';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { isMySet } from '../../../../apps/issues/utils';
import Link from '../../../../components/common/Link';
import { DEFAULT_ISSUES_QUERY } from '../../../../components/shared/utils';
import { translate } from '../../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../../helpers/urls';
import { AppState } from '../../../../types/appstate';
import { CurrentUser } from '../../../../types/users';
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
          {translate('issues.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  renderRulesLink() {
    return (
      <MainMenuItem>
        <NavLink
          className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
          to="/coding_rules"
        >
          {translate('coding_rules.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  renderProfilesLink() {
    return (
      <MainMenuItem>
        <NavLink className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')} to="/profiles">
          {translate('quality_profiles.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  renderQualityGatesLink() {
    return (
      <MainMenuItem>
        <NavLink
          className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
          to={getQualityGatesUrl()}
        >
          {translate('quality_gates.page')}
        </NavLink>
      </MainMenuItem>
    );
  }

  renderAdministrationLink() {
    if (!this.props.appState.canAdmin) {
      return null;
    }

    return (
      <MainMenuItem>
        <NavLink
          className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
          to="/admin/settings"
        >
          {translate('layout.settings')}
        </NavLink>
      </MainMenuItem>
    );
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
          {this.renderRulesLink()}
          {this.renderProfilesLink()}
          {this.renderQualityGatesLink()}
          {this.renderAdministrationLink()}
          <GlobalNavMore />
        </MainMenu>
      </nav>
    );
  }
}

export default withAppStateContext(GlobalNavMenu);
