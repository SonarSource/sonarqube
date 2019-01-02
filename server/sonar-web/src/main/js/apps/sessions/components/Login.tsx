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
import LoginForm from './LoginForm';
import OAuthProviders from './OAuthProviders';
import { translate } from '../../../helpers/l10n';
import './Login.css';

interface Props {
  identityProviders: T.IdentityProvider[];
  onSubmit: (login: string, password: string) => Promise<void>;
  returnTo: string;
}

export default function Login({ identityProviders, onSubmit, returnTo }: Props) {
  return (
    <div className="login-page" id="login_form">
      <h1 className="login-title text-center">{translate('login.login_to_sonarqube')}</h1>

      {identityProviders.length > 0 && (
        <OAuthProviders identityProviders={identityProviders} returnTo={returnTo} />
      )}

      <LoginForm collapsed={identityProviders.length > 0} onSubmit={onSubmit} returnTo={returnTo} />
    </div>
  );
}
