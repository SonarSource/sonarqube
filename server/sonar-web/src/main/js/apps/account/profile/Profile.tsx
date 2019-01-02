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
import { connect } from 'react-redux';
import UserExternalIdentity from './UserExternalIdentity';
import UserGroups from './UserGroups';
import UserScmAccounts from './UserScmAccounts';
import { getCurrentUser, areThereCustomOrganizations, Store } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';

interface Props {
  customOrganizations?: boolean;
  user: T.LoggedInUser;
}

function Profile({ customOrganizations, user }: Props) {
  return (
    <div className="account-body account-container">
      <div className="boxed-group boxed-group-inner">
        <div className="spacer-bottom">
          {translate('login')}: <strong id="login">{user.login}</strong>
        </div>

        {!user.local &&
          user.externalProvider !== 'sonarqube' && (
            <div className="spacer-bottom" id="identity-provider">
              <UserExternalIdentity user={user} />
            </div>
          )}

        {!!user.email && (
          <div className="spacer-bottom">
            {translate('my_profile.email')}: <strong id="email">{user.email}</strong>
          </div>
        )}

        {!customOrganizations && (
          <>
            <hr className="account-separator" />
            <UserGroups groups={user.groups} />
          </>
        )}

        <hr />

        <UserScmAccounts scmAccounts={user.scmAccounts} user={user} />
      </div>
    </div>
  );
}

const mapStateToProps = (state: Store) => ({
  customOrganizations: areThereCustomOrganizations(state),
  user: getCurrentUser(state) as T.LoggedInUser
});

export default connect(mapStateToProps)(Profile);
