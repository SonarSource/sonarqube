/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as classNames from 'classnames';
import LoginForm from './LoginForm';
import OAuthProviders from './OAuthProviders';
import { IdentityProvider } from '../../../app/types';
import { getHostUrl } from '../../../helpers/urls';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import './LoginSonarCloud.css';

interface Props {
  identityProviders: IdentityProvider[];
  onSubmit: (login: string, password: string) => Promise<void>;
  returnTo: string;
  showForm?: boolean;
}

export default function LoginSonarCloud({
  identityProviders,
  onSubmit,
  returnTo,
  showForm
}: Props) {
  const displayForm = showForm || identityProviders.length <= 0;
  return (
    <div
      className={classNames('sonarcloud-login-page boxed-group boxed-group-inner', {
        'sonarcloud-login-page-large': displayForm
      })}
      id="login_form">
      <div className="text-center">
        <img
          alt="SonarCloud logo"
          height={36}
          src={`${getHostUrl()}/images/sc-icon.svg`}
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
    </div>
  );
}

function formatLabel(name: string) {
  return translateWithParameters('login.with_x', name);
}
