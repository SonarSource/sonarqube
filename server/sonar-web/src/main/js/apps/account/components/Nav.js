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
// @flow
import React from 'react';
import { Link, IndexLink } from 'react-router';
import NavBarTabs from '../../../components/nav/NavBarTabs';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  customOrganizations: boolean
};
*/

export default function Nav({ customOrganizations } /*: Props */) {
  return (
    <nav className="account-nav">
      <NavBarTabs>
        <li>
          <IndexLink to="/account/" activeClassName="active">
            {translate('my_account.profile')}
          </IndexLink>
        </li>
        <li>
          <Link to="/account/security/" activeClassName="active">
            {translate('my_account.security')}
          </Link>
        </li>
        <li>
          <Link to="/account/notifications" activeClassName="active">
            {translate('my_account.notifications')}
          </Link>
        </li>
        {!customOrganizations && (
          <li>
            <Link to="/account/projects/" activeClassName="active">
              {translate('my_account.projects')}
            </Link>
          </li>
        )}
        {customOrganizations && (
          <li>
            <Link to="/account/organizations" activeClassName="active">
              {translate('my_account.organizations')}
            </Link>
          </li>
        )}
      </NavBarTabs>
    </nav>
  );
}
