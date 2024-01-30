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

import { PageTitle } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { useCurrentLoginUser } from '../../../app/components/current-user/CurrentUserContext';
import ResetPasswordForm from '../../../components/common/ResetPasswordForm';
import { translate } from '../../../helpers/l10n';
import Tokens from './Tokens';

export default function Security() {
  const currentUser = useCurrentLoginUser();
  return (
    <>
      <Helmet defer={false} title={translate('my_account.security')} />

      <Tokens login={currentUser.login} />

      {currentUser.local && (
        <>
          <PageTitle
            className="sw-heading-md sw-my-6"
            text={translate('my_profile.password.title')}
          />

          <ResetPasswordForm user={currentUser} />
        </>
      )}
    </>
  );
}
