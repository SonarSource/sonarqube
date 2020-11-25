/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import ResetPasswordForm from '../../components/common/ResetPassword';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { Router, withRouter } from '../../components/hoc/withRouter';
import GlobalMessagesContainer from './GlobalMessagesContainer';
import './ResetPassword.css';

export interface ResetPasswordProps {
  currentUser: T.LoggedInUser;
  router: Router;
}

export function ResetPassword(props: ResetPasswordProps) {
  const { router, currentUser } = props;
  const redirect = () => {
    router.replace('/');
  };

  return (
    <div className="reset-page">
      <h1 className="text-center spacer-bottom">{translate('my_account.reset_password')}</h1>
      <h2 className="text-center huge-spacer-bottom">
        {translate('my_account.reset_password.explain')}
      </h2>
      <GlobalMessagesContainer />
      <div className="reset-form">
        <ResetPasswordForm user={currentUser} onPasswordChange={redirect} />
      </div>
    </div>
  );
}

export default whenLoggedIn(withRouter(ResetPassword));
