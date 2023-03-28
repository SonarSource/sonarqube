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
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { translate } from '../../../helpers/l10n';
import { LoggedInUser } from '../../../types/users';
import { Preferences } from './Preferences';
import UserExternalIdentity from './UserExternalIdentity';
import Link from "../../../components/common/Link";

export interface ProfileProps {
  currentUser: LoggedInUser;
}

export function Profile({ currentUser }: ProfileProps) {
  const isExternalProvider = !currentUser.local && currentUser.externalProvider !== 'sonarqube';

  return (
    <div className="account-body account-container account-profile">
      <div className="boxed-group">
        {renderLogin()}
        {renderEmail()}
        {renderOrganizationGroups()}
        {renderScmAccounts()}
      </div>
      <Preferences />
    </div>
  );

  function renderLogin() {
    if (!currentUser.login && !isExternalProvider) {
      return null;
    }

    return (
      <div className="boxed-group-inner">
        <h2 className="spacer-bottom">{translate('my_profile.login')}</h2>
        {currentUser.login && (
          <p className="spacer-bottom" id="login">
            {currentUser.login}
          </p>
        )}
        {isExternalProvider && <UserExternalIdentity user={currentUser} />}
      </div>
    );
  }

  function renderEmail() {
    if (!currentUser.email) {
      return null;
    }

    return (
      <div className="boxed-group-inner">
        <h2 className="spacer-bottom">{translate('my_profile.email')}</h2>
        <div className="spacer-bottom">
          <p id="email">{currentUser.email}</p>
        </div>
      </div>
    );
  }

  function renderOrganizationGroups() {
    if (!currentUser.orgGroups || currentUser.orgGroups.length === 0) {
      return null;
    }

    return (
        <div className="boxed-group-inner">
          <h2 className="spacer-bottom">{translate('my_profile.groups')}</h2>
          <table className="data zebra zebra-hover" id="organization-group-table">
            <thead>
            <tr>
              <th className="nowrap width-20">
                Organization
              </th>
              <th className="nowrap width-80">
                Groups
              </th>
            </tr>
            </thead>
            <tbody>
            {currentUser.orgGroups.map(orgGroup => (
              <tr key={orgGroup.organizationKey}>
                <td className="width-20">
                  <Link to={`/organizations/${orgGroup.organizationKey}/groups`}>
                    <strong>{orgGroup.organizationName}</strong>
                  </Link>
                </td>
                <td className="width-80">
                <span >
                  {orgGroup.organizationGroups}
                </span>
                </td>
              </tr>
            ))}
            </tbody>
          </table>
        </div>
    );
  }

  function renderScmAccounts() {
    if (
      !currentUser.login &&
      !currentUser.email &&
      (!currentUser.scmAccounts || currentUser.scmAccounts.length === 0)
    ) {
      return null;
    }

    return (
      <div className="boxed-group-inner">
        <h2 className="spacer-bottom">
          {translate('my_profile.scm_accounts')}
          <HelpTooltip
            className="little-spacer-left"
            overlay={translate('my_profile.scm_accounts.tooltip')}
          />
        </h2>
        <ul id="scm-accounts">
          {currentUser.login && (
            <li className="little-spacer-bottom text-ellipsis" title={currentUser.login}>
              {currentUser.login}
            </li>
          )}

          {currentUser.email && (
            <li className="little-spacer-bottom text-ellipsis" title={currentUser.email}>
              {currentUser.email}
            </li>
          )}

          {currentUser.scmAccounts &&
            currentUser.scmAccounts.length > 0 &&
            currentUser.scmAccounts.map((scmAccount) => (
              <li className="little-spacer-bottom" key={scmAccount} title={scmAccount}>
                {scmAccount}
              </li>
            ))}
        </ul>
      </div>
    );
  }
}

export default whenLoggedIn(Profile);
