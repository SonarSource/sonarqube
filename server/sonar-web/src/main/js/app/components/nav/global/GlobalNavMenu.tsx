/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import Link from '../../../../components/common/Link';
import Dropdown from '../../../../components/controls/Dropdown';
import DropdownIcon from '../../../../components/icons/DropdownIcon';
import { translate } from '../../../../helpers/l10n';
import { AppState } from '../../../../types/appstate';
import { ComponentQualifier } from '../../../../types/component';
import { Extension } from '../../../../types/types';
import { CurrentUser, LoggedInUser } from '../../../../types/users';
import withAppStateContext from '../../app-state/withAppStateContext';
import handleRequiredAuthentication from '../../../../helpers/handleRequiredAuthentication';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
  location: { pathname: string };
}

const ACTIVE_CLASS_NAME = 'active';
export class GlobalNavMenu extends React.PureComponent<Props> {
  renderProjects() {
    const active =
      this.props.location.pathname.startsWith('/projects') &&
      this.props.location.pathname !== '/projects/create';

    return (
      <li>
        <Link
          aria-current={active ? 'page' : undefined}
          className={classNames({ active })}
          to="/projects"
        >
          {translate('projects.page')}
        </Link>
      </li>
    );
  }

  renderPortfolios() {
    return (
      <li>
        <NavLink className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')} to="/portfolios">
          {translate('portfolios.page')}
        </NavLink>
      </li>
    );
  }

  renderIssuesLink() {
    if (!this.props.currentUser.isLoggedIn) {
          handleRequiredAuthentication();
          return;
      }
    const search = new URLSearchParams({ resolved: 'false', myIssues: 'true' }).toString();
    return (
      <li>
        <NavLink
          className={({ isActive }) => (isActive ? ACTIVE_CLASS_NAME : '')}
          to={{pathname: '/issues', search }}
        >
          {translate('myissues.page')}
        </NavLink>
      </li>
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
            <li>
              <Link to="/admin">
                {translate('layout.settings')}
              </Link>
            </li>
        );
      }
    }

    return null;
  }

  renderGlobalPageLink = ({ key, name }: Extension) => {
    return (
      <li key={key}>
        <Link to={`/extension/${key}`}>{name}</Link>
      </li>
    );
  };

  renderMore() {
    const { globalPages = [] } = this.props.appState;
    const withoutPortfolios = globalPages.filter((page) => page.key !== 'governance/portfolios');
    if (withoutPortfolios.length === 0) {
      return null;
    }
    return (
      <Dropdown
        overlay={<ul className="menu">{withoutPortfolios.map(this.renderGlobalPageLink)}</ul>}
        tagName="li"
      >
        {({ onToggleClick, open }) => (
          <a
            aria-expanded={open}
            aria-haspopup="menu"
            role="button"
            className={classNames('dropdown-toggle', { active: open })}
            href="#"
            id="global-navigation-more"
            onClick={onToggleClick}
          >
            {translate('more')}
            <DropdownIcon className="little-spacer-left text-middle" />
          </a>
        )}
      </Dropdown>
    );
  }

  render() {
    const governanceInstalled = this.props.appState.qualifiers.includes(
      ComponentQualifier.Portfolio
    );

    return (
      <ul className="global-navbar-menu">
        {this.renderProjects()}
        {governanceInstalled && this.renderPortfolios()}
        {this.renderIssuesLink()}
        {this.renderAdministrationLink()}
        {this.renderMore()}
      </ul>
    );
  }
}

export default withAppStateContext(GlobalNavMenu);
