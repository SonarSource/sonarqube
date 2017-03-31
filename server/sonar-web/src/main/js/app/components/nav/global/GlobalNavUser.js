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
import { Link, withRouter } from 'react-router';
import Avatar from '../../../../components/ui/Avatar';
import { translate } from '../../../../helpers/l10n';

class GlobalNavUser extends React.Component {
  props: {
    currentUser: {
      email?: string,
      name: string
    },
    location: Object,
    router: { push: (string) => void }
  };

  handleLogin = e => {
    e.preventDefault();
    const shouldReturnToCurrentPage = window.location.pathname !== `${window.baseUrl}/about`;
    if (shouldReturnToCurrentPage) {
      const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
      window.location = window.baseUrl +
        `/sessions/new?return_to=${returnTo}${window.location.hash}`;
    } else {
      window.location = `${window.baseUrl}/sessions/new`;
    }
  };

  handleLogout = e => {
    e.preventDefault();
    this.props.router.push('/sessions/logout');
  };

  renderAuthenticated() {
    const { currentUser } = this.props;
    return (
      <li className="dropdown js-user-authenticated">
        <a className="dropdown-toggle" data-toggle="dropdown" href="#">
          <Avatar email={currentUser.email} size={20} />&nbsp;
          {currentUser.name}&nbsp;<i className="icon-dropdown" />
        </a>
        <ul className="dropdown-menu dropdown-menu-right">
          <li>
            <Link to="/account">{translate('my_account.page')}</Link>
          </li>
          <li>
            <a onClick={this.handleLogout} href="#">{translate('layout.logout')}</a>
          </li>
        </ul>
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

export default withRouter(GlobalNavUser);
