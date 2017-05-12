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
// @flow
import React from 'react';
import classNames from 'classnames';
import { sortBy } from 'lodash';
import { Link } from 'react-router';
import Avatar from '../../../../components/ui/Avatar';
import OrganizationLink from '../../../../components/ui/OrganizationLink';
import { translate } from '../../../../helpers/l10n';

type CurrentUser = {
  email?: string,
  isLoggedIn: boolean,
  name: string
};

type Props = {
  appState: {
    organizationsEnabled: boolean
  },
  currentUser: CurrentUser,
  fetchMyOrganizations: () => Promise<*>,
  location: Object,
  organizations: Array<{ key: string, name: string }>,
  router: { push: string => void }
};

type State = {
  open: boolean
};

export default class GlobalNavUser extends React.PureComponent {
  node: HTMLElement;
  props: Props;
  state: State = { open: false };

  componentWillUnmount() {
    window.removeEventListener('click', this.handleClickOutside);
  }

  handleClickOutside = (event: { target: HTMLElement }) => {
    if (!this.node || !this.node.contains(event.target)) {
      this.closeDropdown();
    }
  };

  handleLogin = (e: Event) => {
    e.preventDefault();
    const shouldReturnToCurrentPage = window.location.pathname !== `${window.baseUrl}/about`;
    if (shouldReturnToCurrentPage) {
      const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
      window.location =
        window.baseUrl + `/sessions/new?return_to=${returnTo}${window.location.hash}`;
    } else {
      window.location = `${window.baseUrl}/sessions/new`;
    }
  };

  handleLogout = (e: Event) => {
    e.preventDefault();
    this.closeDropdown();
    this.props.router.push('/sessions/logout');
  };

  toggleDropdown = (evt: Event) => {
    evt.preventDefault();
    if (this.state.open) {
      this.closeDropdown();
    } else {
      this.openDropdown();
    }
  };

  openDropdown = () => {
    this.fetchMyOrganizations().then(() => {
      window.addEventListener('click', this.handleClickOutside, true);
      this.setState({ open: true });
    });
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

  renderAuthenticated() {
    const { currentUser, organizations } = this.props;
    const hasOrganizations = this.props.appState.organizationsEnabled && organizations.length > 0;
    return (
      <li
        className={classNames('dropdown js-user-authenticated', { open: this.state.open })}
        ref={node => (this.node = node)}>
        <a className="dropdown-toggle navbar-avatar" href="#" onClick={this.toggleDropdown}>
          <Avatar email={currentUser.email} name={currentUser.name} size={24} />
        </a>
        {this.state.open &&
          <ul className="dropdown-menu dropdown-menu-right">
            <li className="dropdown-item">
              <div className="text-ellipsis text-muted" title={currentUser.name}>
                <strong>{currentUser.name}</strong>
              </div>
              {currentUser.email != null &&
                <div
                  className="little-spacer-top text-ellipsis text-muted"
                  title={currentUser.email}>
                  {currentUser.email}
                </div>}
            </li>
            <li className="divider" />
            <li>
              <Link to="/account" onClick={this.closeDropdown}>{translate('my_account.page')}</Link>
            </li>
            {hasOrganizations && <li role="separator" className="divider" />}
            {hasOrganizations &&
              <li className="dropdown-header spacer-left">{translate('my_organizations')}</li>}
            {hasOrganizations &&
              sortBy(organizations, org => org.name.toLowerCase()).map(organization => (
                <li key={organization.key}>
                  <OrganizationLink organization={organization} onClick={this.closeDropdown}>
                    <span className="spacer-left">{organization.name}</span>
                  </OrganizationLink>
                </li>
              ))}
            {hasOrganizations && <li role="separator" className="divider" />}
            <li>
              <a onClick={this.handleLogout} href="#">{translate('layout.logout')}</a>
            </li>
          </ul>}
      </li>
    );
  }

  renderAnonymous() {
    return (
      <li>
        <a onClick={this.handleLogin} href="#">{translate('layout.login')}</a>
      </li>
    );
  }

  render() {
    return this.props.currentUser.isLoggedIn ? this.renderAuthenticated() : this.renderAnonymous();
  }
}
