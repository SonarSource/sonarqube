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
import { Link } from 'react-router';
import Dropdown from '../../../../components/controls/Dropdown';
import { Router, withRouter } from '../../../../components/hoc/withRouter';
import Avatar from '../../../../components/ui/Avatar';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { isLoggedIn } from '../../../../helpers/users';
import { CurrentUser, LoggedInUser } from '../../../../types/types';
import { rawSizes } from '../../../theme';

interface Props {
  currentUser: CurrentUser;
  router: Pick<Router, 'push'>;
}

export class GlobalNavUser extends React.PureComponent<Props> {
  handleLogin = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.href =
      getBaseUrl() + `/sessions/new?return_to=${returnTo}${window.location.hash}`;
  };

  handleLogout = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.router.push('/sessions/logout');
  };

  renderAuthenticated() {
    const currentUser = this.props.currentUser as LoggedInUser;
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
            <li>
              <a href="#" onClick={this.handleLogout}>
                {translate('layout.logout')}
              </a>
            </li>
          </ul>
        }
        tagName="li">
        <a className="dropdown-toggle navbar-avatar" href="#" title={currentUser.name}>
          <Avatar
            hash={currentUser.avatar}
            name={currentUser.name}
            size={rawSizes.globalNavContentHeightRaw}
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

export default withRouter(GlobalNavUser);
