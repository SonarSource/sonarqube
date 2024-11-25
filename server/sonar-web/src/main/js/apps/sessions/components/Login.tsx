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
import { Location } from '../../../components/hoc/withRouter';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { SafeHTMLInjection, SanitizeLevel } from '../../../helpers/sanitize';
import { getReturnUrl } from '../../../helpers/urls';
import { IdentityProvider } from '../../../types/types';
import './Login.css';
import LoginForm from './LoginForm';
import OAuthProviders from './OAuthProviders';

export interface LoginProps {
  identityProviders: IdentityProvider[];
  loading: boolean;
  message?: string;
  onSubmit: (login: string, password: string) => Promise<void>;
  location: Location;
}

export default function Login(props: LoginProps) {
  const { identityProviders, loading, location, message } = props;
  const returnTo = getReturnUrl(location);
  const displayError = Boolean(location.query.authorizationError);

  return (
    <div className="login-page" id="login_form">
      <h1 className="login-title text-center big-spacer-bottom">
        {translate('login.login_to_sonarqube')}
      </h1>

      {loading ? (
        <DeferredSpinner loading={loading} timeout={0} />
      ) : (
        <>
          {displayError && (
            <Alert className="big-spacer-bottom" display="block" variant="error">
              {translate('login.unauthorized_access_alert')}
            </Alert>
          )}

          {message && (
            <SafeHTMLInjection htmlAsString={message} sanitizeLevel={SanitizeLevel.USER_INPUT}>
              <div className="login-message markdown big-padded spacer-top huge-spacer-bottom" />
            </SafeHTMLInjection>
          )}

          {identityProviders.length > 0 && (
            <OAuthProviders identityProviders={identityProviders} returnTo={returnTo} />
          )}

          <LoginForm collapsed={identityProviders.length > 0} onSubmit={props.onSubmit} />
        </>
      )}
    </div>
  );
}
