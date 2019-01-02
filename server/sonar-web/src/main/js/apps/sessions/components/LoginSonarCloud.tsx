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
import * as classNames from 'classnames';
import LoginForm from './LoginForm';
import OAuthProviders from './OAuthProviders';
import { getBaseUrl } from '../../../helpers/urls';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Alert } from '../../../components/ui/Alert';
import './LoginSonarCloud.css';
import { Store } from '../../../store/rootReducer';

interface Props {
  identityProviders: T.IdentityProvider[];
  onSubmit: (login: string, password: string) => Promise<void>;
  returnTo: string;
  showForm?: boolean;
  authorizationError?: boolean;
  authenticationError?: boolean;
}

function formatLabel(name: string) {
  return translateWithParameters('login.with_x', name);
}

export function LoginSonarCloud({
  showForm,
  identityProviders,
  returnTo,
  onSubmit,
  authorizationError,
  authenticationError
}: Props) {
  const displayForm = showForm || identityProviders.length <= 0;
  const displayErrorAction = authorizationError || authenticationError;
  return (
    <>
      {displayErrorAction && (
        <Alert className="sonarcloud-login-alert" display="block" variant="warning">
          {translate('login.unauthorized_access_alert')}
        </Alert>
      )}
      <div
        className={classNames('sonarcloud-login-page boxed-group boxed-group-inner', {
          'sonarcloud-login-page-large': displayForm
        })}
        id="login_form">
        <div className="text-center">
          <img
            alt="SonarCloud logo"
            height={36}
            src={`${getBaseUrl()}/images/sonarcloud-square-logo.svg`}
            width={36}
          />
          <h1 className="sonarcloud-login-title">
            {translate('login.login_or_signup_to_sonarcloud')}
          </h1>
        </div>

        {displayForm ? (
          <LoginForm onSubmit={onSubmit} returnTo={returnTo} />
        ) : (
          <OAuthProviders
            className="sonarcloud-oauth-providers"
            formatLabel={formatLabel}
            identityProviders={identityProviders}
            returnTo={returnTo}
          />
        )}

        {displayErrorAction && (
          <div className="sonarcloud-login-cancel">
            <div className="horizontal-pipe-separator">
              <div className="horizontal-separator" />
              <span className="note">{translate('or')}</span>
              <div className="horizontal-separator" />
            </div>
            <a href={`${getBaseUrl()}/`}>{translate('go_back_to_homepage')}</a>
          </div>
        )}
      </div>
    </>
  );
}

const mapStateToProps = (state: Store) => ({
  authorizationError: state.appState.authorizationError,
  authenticationError: state.appState.authenticationError
});

export default connect(mapStateToProps)(LoginSonarCloud);
