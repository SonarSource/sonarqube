/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import UserExternalIdentity from './UserExternalIdentity';
import UserGroups from './UserGroups';
import UserScmAccounts from './UserScmAccounts';
import { translate } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';

export interface Props {
  currentUser: T.LoggedInUser;
}

export function Profile({ currentUser }: Props) {
  return (
    <div className="account-body account-container">
      <div className="boxed-group boxed-group-inner">
        <div className="spacer-bottom">
          {translate('login')}: <strong id="login">{currentUser.login}</strong>
        </div>

        {!currentUser.local && currentUser.externalProvider !== 'sonarqube' && (
          <div className="spacer-bottom" id="identity-provider">
            <UserExternalIdentity user={currentUser} />
          </div>
        )}

        {Boolean(currentUser.email) && (
          <div className="spacer-bottom">
            {translate('my_profile.email')}: <strong id="email">{currentUser.email}</strong>
          </div>
        )}

        {!isSonarCloud() && (
          <>
            <hr className="account-separator" />
            <UserGroups groups={currentUser.groups} />
          </>
        )}

        <hr />

        <UserScmAccounts scmAccounts={currentUser.scmAccounts} user={currentUser} />
      </div>
    </div>
  );
}

export default whenLoggedIn(Profile);
