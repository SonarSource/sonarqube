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
import * as React from 'react';
import Link from '../../../../components/common/Link';
import Dropdown from '../../../../components/controls/Dropdown';
import { Router, withRouter } from '../../../../components/hoc/withRouter';
import Avatar from '../../../../components/ui/Avatar';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { CurrentUser, isLoggedIn, LoggedInUser } from '../../../../types/users';
import { rawSizes } from '../../../theme';
import { Organization } from "../../../../types/types";
import OrganizationListItem from "../../../../apps/organizations/components/OrganizationListItem";
import { sortBy } from "lodash";

interface Props {
  currentUser: CurrentUser;
  userOrganizations: Organization[];
  router: Router;
}

interface State {
  pendoInitialized?: boolean;
}

export class GlobalNavUser extends React.PureComponent<Props, State> {
  state: State = {};

  componentDidMount() {
    this.initializePendo();
  }

  focusNode = (node: HTMLAnchorElement | null) => {
    if (node) {
      node.focus();
    }
  };

  handleLogin = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
    window.location.href = `${getBaseUrl()}/sessions/new?return_to=${returnTo}${
      window.location.hash
    }`;
  };

  handleLogout = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.router.push('/sessions/logout');
  };

  initializePendo = () => {
    const { pendoInitialized } = this.state;
    const currentUser = this.props.currentUser as LoggedInUser;
    const hasOrganizations = this.props.userOrganizations.length > 0;

    const isCodescan = window.location.hostname.includes('codescan.io') || window.location.hostname.includes('autorabit.com');
    if (isLoggedIn(currentUser) && hasOrganizations && !pendoInitialized && isCodescan) {
      const script = document.createElement('script');
      const orgKeys = this.props.userOrganizations.map(o => o.kee).join(',');

      script.innerHTML =
          "  pendo.initialize({\n" +
          "        visitor: {\n" +
          "          id: '" + (currentUser.email ? currentUser.email : currentUser.login) + "'\n" +
          "        },\n" +
          "        account: {\n" +
          "          id: '" + orgKeys + "'\n" +
          "        }\n" +
          "      });";

      document.body.appendChild(script);

      this.setState({ pendoInitialized: true });
    }
  }

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
                  title={currentUser.email}
                >
                  {currentUser.email}
                </div>
              )}
            </li>
            <li className="divider" />
            <li>
              <Link ref={this.focusNode} to="/account">
                {translate('my_account.page')}
              </Link>
            </li>
            <li className="divider" role="separator" />
            <li>
              <Link to="/account/organizations">{translate('my_organizations')}</Link>
            </li>
            {sortBy(this.props.userOrganizations, org => org.name.toLowerCase()).map(organization => (
                <OrganizationListItem key={organization.kee} organization={organization} />
            ))}
            <li className="divider" role="separator" />
            <li>
              <a href="#" onClick={this.handleLogout}>
                {translate('layout.logout')}
              </a>
            </li>
          </ul>
        }
      >
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
      <div>
        <Link className="navbar-login" to="/sessions/new" onClick={this.handleLogin}>
          {translate('layout.login')}
        </Link>
      </div>
    );
  }

  render() {
    return isLoggedIn(this.props.currentUser) ? this.renderAuthenticated() : this.renderAnonymous();
  }
}

export default withRouter(GlobalNavUser);
