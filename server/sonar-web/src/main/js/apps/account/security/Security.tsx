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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import ResetPasswordForm from '../../../components/common/ResetPasswordForm';
import { translate } from '../../../helpers/l10n';
import { LoggedInUser } from '../../../types/users';
import Tokens from './Tokens';

export interface SecurityProps {
  currentUser: LoggedInUser;
}

export function Security({ currentUser }: SecurityProps) {
  return (
    <div className="account-body account-container">
      <Helmet defer={false} title={translate('my_account.security')} />
      <Tokens login={currentUser.login} />
      {currentUser.local && (
        <section className="boxed-group">
          <h2 className="spacer-bottom">{translate('my_profile.password.title')}</h2>
          <ResetPasswordForm className="boxed-group-inner" user={currentUser} />
        </section>
      )}
    </div>
  );
}

export default withCurrentUserContext(Security);
