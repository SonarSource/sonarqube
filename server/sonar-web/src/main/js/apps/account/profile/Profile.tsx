/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { Helmet } from 'react-helmet-async';
import { GreySeparator, HelperHintIcon, SubHeading, Title } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { translate } from '../../../helpers/l10n';
import { LoggedInUser } from '../../../types/users';
import { Preferences } from './Preferences';
import UserExternalIdentity from './UserExternalIdentity';

export interface ProfileProps {
  currentUser: LoggedInUser;
}

export function Profile({ currentUser }: ProfileProps) {
  const isExternalProvider = !currentUser.local && currentUser.externalProvider !== 'sonarqube';

  return (
    <div className="sw-max-w-1/2">
      <Helmet defer={false} title={translate('my_account.profile')} />
      <Title className="sw-mb-6">{translate('my_account.profile')}</Title>
      {renderLogin()}
      {renderEmail()}
      <GreySeparator className="sw-my-4" />
      {renderUserGroups()}
      {renderScmAccounts()}
      <GreySeparator className="sw-my-4" />
      <Preferences />
    </div>
  );

  function renderLogin() {
    if (!currentUser.login && !isExternalProvider) {
      return null;
    }

    return (
      <div className="sw-flex sw-items-center sw-mb-2">
        <strong className="sw-typo-semibold sw-mr-1">{translate('my_profile.login')}:</strong>
        {currentUser.login && <span id="login">{currentUser.login}</span>}
        {isExternalProvider && <UserExternalIdentity user={currentUser} />}
      </div>
    );
  }

  function renderEmail() {
    if (!currentUser.email) {
      return null;
    }

    return (
      <div className="sw-mb-2">
        <strong className="sw-typo-semibold sw-mr-1">{translate('my_profile.email')}:</strong>
        <span id="email">{currentUser.email}</span>
      </div>
    );
  }

  function renderUserGroups() {
    if (!currentUser.groups || currentUser.groups.length === 0) {
      return null;
    }

    return (
      <>
        <SubHeading as="h2">{translate('my_profile.groups')}</SubHeading>
        <ul id="groups">
          {currentUser.groups.map((group) => (
            <li className="sw-mb-2" key={group} title={group}>
              {group}
            </li>
          ))}
        </ul>
        <GreySeparator className="sw-my-4" />
      </>
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
      <>
        <SubHeading as="h2">
          {translate('my_profile.scm_accounts')}
          <HelpTooltip className="sw-ml-2" overlay={translate('my_profile.scm_accounts.tooltip')}>
            <HelperHintIcon />
          </HelpTooltip>
        </SubHeading>
        <ul id="scm-accounts">
          {currentUser.login && <li title={currentUser.login}>{currentUser.login}</li>}

          {currentUser.email && <li title={currentUser.email}>{currentUser.email}</li>}

          {currentUser.scmAccounts &&
            currentUser.scmAccounts.length > 0 &&
            currentUser.scmAccounts.map((scmAccount) => (
              <li key={scmAccount} title={scmAccount}>
                {scmAccount}
              </li>
            ))}
        </ul>
      </>
    );
  }
}

export default whenLoggedIn(Profile);
