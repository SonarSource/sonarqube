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
import * as React from 'react';
import { sortBy } from 'lodash';
import * as PropTypes from 'prop-types';
import { Link } from 'react-router';
import * as theme from '../../../theme';
import { CurrentUser, LoggedInUser, isLoggedIn, Organization } from '../../../types';
import Avatar from '../../../../components/ui/Avatar';
import OrganizationListItem from '../../../../components/ui/OrganizationListItem';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/urls';
import Dropdown from '../../../../components/controls/Dropdown';

interface Props {
  appState: { organizationsEnabled?: boolean };
  currentUser: CurrentUser;
  organizations: Organization[];
}

export default class GlobalNavUser extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object
  };

  handleLogin = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    const shouldReturnToCurrentPage = window.location.pathname !== `${getBaseUrl()}/about`;
    if (shouldReturnToCurrentPage) {
      const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
      window.location.href =
        getBaseUrl() + `/sessions/new?return_to=${returnTo}${window.location.hash}`;
    } else {
      window.location.href = `${getBaseUrl()}/sessions/new`;
    }
  };

  handleLogout = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.context.router.push('/sessions/logout');
  };

  renderAuthenticated() {
    const { organizations } = this.props;
    const currentUser = this.props.currentUser as LoggedInUser;
    const hasOrganizations = this.props.appState.organizationsEnabled && organizations.length > 0;
    return (
      <Dropdown
        className="js-user-authenticated"
        overlay={
          <ul className="menu">
            <li className="menu-item">
              <div className="text-ellipsis text-muted" title={currentUser.name}>
                <strong>{currentUser.name}</strong>
              </div>
              {currentUser.email != null && (
                <div
                  className="little-spacer-top text-ellipsis text-muted"
                  title={currentUser.email}>
                  {currentUser.email}
                </div>
              )}
            </li>
            <li className="divider" />
            <li>
              <Link to="/account">{translate('my_account.page')}</Link>
            </li>
            {hasOrganizations && <li className="divider" role="separator" />}
            {hasOrganizations && (
              <li>
                <Link to="/account/organizations">{translate('my_organizations')}</Link>
              </li>
            )}
            {hasOrganizations &&
              sortBy(organizations, org => org.name.toLowerCase()).map(organization => (
                <OrganizationListItem key={organization.key} organization={organization} />
              ))}
            {hasOrganizations && <li className="divider" role="separator" />}
            <li>
              <a href="#" onClick={this.handleLogout}>
                {translate('layout.logout')}
              </a>
            </li>
          </ul>
        }
        tagName="li">
        <a className="dropdown-toggle navbar-avatar" href="#">
          <Avatar
            hash={currentUser.avatar}
            name={currentUser.name}
            size={theme.globalNavContentHeightRaw}
          />
        </a>
      </Dropdown>
    );
  }

  renderAnonymous() {
    return (
      <li>
        <a className="navbar-login" href="/sessions/new" onClick={this.handleLogin}>
          {translate('layout.login')}
        </a>
      </li>
    );
  }

  render() {
    return isLoggedIn(this.props.currentUser) ? this.renderAuthenticated() : this.renderAnonymous();
  }
}
