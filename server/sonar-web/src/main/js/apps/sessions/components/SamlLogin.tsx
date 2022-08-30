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
import * as React from "react";
import {SubmitButton} from 'sonar-ui-common/components/controls/buttons';
import {translate} from 'sonar-ui-common/helpers/l10n';
import {getBaseUrl} from 'sonar-ui-common/helpers/urls';
import './Login.css';
import './LoginForm.css';

export default function SamlLogin() {

  const [email, setEmail] = React.useState<string>();

  const handleLoginChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(event.currentTarget.value)
  }

  return (
      <div className="login-page" id="login_form">
        <h1 className="login-title text-center">Sign in Using SSO</h1>
        <form className="login-form" action={`/_codescan/saml2/login/${email}`} method="POST">
          <div className="big-spacer-bottom">
            <input
                autoFocus={true}
                className="login-input"
                id="login"
                maxLength={255}
                name="login"
                onChange={handleLoginChange}
                placeholder="Your company email"
                required={true}
                type="text"
            />
          </div>

          <div>
            <div className="text-right overflow-hidden">
              <SubmitButton>
                {translate('sessions.log_in')}
              </SubmitButton>
              <a className="spacer-left" href={`${getBaseUrl()}/`}>
                {translate('back')}
              </a>
            </div>
          </div>
        </form>
      </div>
  );
}