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
import ResetPasswordForm from '../../components/common/ResetPasswordForm';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { LoggedInUser } from '../../types/users';

export interface ResetPasswordProps {
  currentUser: LoggedInUser;
}

export function ResetPassword({ currentUser }: ResetPasswordProps) {
  return (
    <div className="page-wrapper-simple">
      <Helmet defer={false} title={translate('my_account.reset_password.page')} />
      <div className="page-simple">
        <h1 className="text-center huge">{translate('my_account.reset_password')}</h1>
        <p className="text-center huge-spacer-top huge-spacer-bottom">
          {translate('my_account.reset_password.explain')}
        </p>

        <div className="text-center">
          <h2 className="big-spacer-bottom big">{translate('my_profile.password.title')}</h2>

          <ResetPasswordForm
            user={currentUser}
            onPasswordChange={() => {
              // Force a refresh for the backend to handle additional redirects.
              window.location.href = `${getBaseUrl()}/`;
            }}
          />
        </div>
      </div>
    </div>
  );
}

export default whenLoggedIn(ResetPassword);
