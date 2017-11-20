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
import * as React from 'react';
import * as classNames from 'classnames';
import * as PropTypes from 'prop-types';
import { sortBy } from 'lodash';
import { Link } from 'react-router';
import Avatar from '../../../../components/ui/Avatar';
import OrganizationIcon from '../../../../components/icons-components/OrganizationIcon';
import OrganizationLink from '../../../../components/ui/OrganizationLink';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/urls';
import { AppState } from '../../../../store/appState/duck';
import { CurrentUser, LoggedInUser, isLoggedInUser } from '../../../types';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
  fetchMyOrganizations: () => Promise<void>;
  location: { pathname: string };
  organizations: Array<{ key: string; name: string }>;
}

interface State {
  open: boolean;
}

export default class GlobalNavUser extends React.PureComponent<Props, State> {
  node?: HTMLElement | null;

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = { open: false };
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.handleClickOutside);
  }

  handleClickOutside = (event: MouseEvent) => {
    if (!this.node || !this.node.contains(event.currentTarget as HTMLElement)) {
      this.closeDropdown();
    }
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

  handleLogout = (e: React.SyntheticEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    this.closeDropdown();
    this.context.router.push('/sessions/logout');
  };

  toggleDropdown = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    if (this.state.open) {
      this.closeDropdown();
    } else {
      this.openDropdown();
    }
  };

  openDropdown = () => {
    this.fetchMyOrganizations().then(
      () => {
        window.addEventListener('click', this.handleClickOutside, true);
        this.setState({ open: true });
      },
      () => {}
    );
  };

  closeDropdown = () => {
    window.removeEventListener('click', this.handleClickOutside);
    this.setState({ open: false });
  };

  fetchMyOrganizations = () => {
    if (this.props.appState.organizationsEnabled) {
      return this.props.fetchMyOrganizations();
    }
    return Promise.resolve();
  };

  renderAuthenticated(user: LoggedInUser) {
    const { organizations } = this.props;
    const hasOrganizations = this.props.appState.organizationsEnabled && organizations.length > 0;
    return (
      <li
        className={classNames('dropdown js-user-authenticated', { open: this.state.open })}
        ref={node => (this.node = node)}>
        <a className="dropdown-toggle navbar-avatar" href="#" onClick={this.toggleDropdown}>
          <Avatar hash={user.avatar} name={user.name!} size={24} />
        </a>
        {this.state.open && (
          <ul className="dropdown-menu dropdown-menu-right">
            <li className="dropdown-item">
              <div className="text-ellipsis text-muted" title={user.name}>
                <strong>{user.name}</strong>
              </div>
              {user.email != null && (
                <div className="little-spacer-top text-ellipsis text-muted" title={user.email}>
                  {user.email}
                </div>
              )}
            </li>
            <li className="divider" />
            <li>
              <Link to="/account" onClick={this.closeDropdown}>
                {translate('my_account.page')}
              </Link>
            </li>
            {hasOrganizations && <li role="separator" className="divider" />}
            {hasOrganizations && (
              <li>
                <Link to="/account/organizations" onClick={this.closeDropdown}>
                  {translate('my_organizations')}
                </Link>
              </li>
            )}
            {hasOrganizations &&
              sortBy(organizations, org => org.name.toLowerCase()).map(organization => (
                <li key={organization.key}>
                  <OrganizationLink organization={organization} onClick={this.closeDropdown}>
                    <OrganizationIcon />
                    <span className="spacer-left">{organization.name}</span>
                  </OrganizationLink>
                </li>
              ))}
            {hasOrganizations && <li role="separator" className="divider" />}
            <li>
              <a onClick={this.handleLogout} href="#">
                {translate('layout.logout')}
              </a>
            </li>
          </ul>
        )}
      </li>
    );
  }

  renderAnonymous() {
    return (
      <li>
        <a className="navbar-login" onClick={this.handleLogin} href="#">
          {translate('layout.login')}
        </a>
      </li>
    );
  }

  render() {
    return isLoggedInUser(this.props.currentUser)
      ? this.renderAuthenticated(this.props.currentUser)
      : this.renderAnonymous();
  }
}
