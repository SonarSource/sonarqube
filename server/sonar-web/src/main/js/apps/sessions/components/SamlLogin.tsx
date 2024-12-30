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
import { ButtonLink, ButtonPrimary, Card, InputField } from '~design-system';
import * as React from 'react';
import { useNavigate } from 'react-router-dom';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { getReturnUrl } from '../../../helpers/urls';
import './SamlLogin.css';

export default function SamlLogin() {
  const location = useLocation();

  const navigate = useNavigate();

  const [email, setEmail] = React.useState<string>();

  const handleLoginChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(event.currentTarget.value);
  };

  function goToBaseUrl() {
    navigate('/sessions/new');
  }

  return (
    <div className="login-card-ctnr sw-flex sw-justify-center sw-items-center">
      <Card>
        <div className="login-page" id="login_form">
          <h1 className="login-title text-center sw-my-4">Sign in Using SSO</h1>
          <form
            className="login-form"
            action={`/_codescan/saml2/login/${email}?return_to=${encodeURIComponent(getReturnUrl(location))}`}
            method="POST"
          >
            <div className="big-spacer-bottom">
              <InputField
                autoFocus={true}
                className="login-input sw-my-4"
                id="login"
                maxLength={255}
                name="login"
                onChange={handleLoginChange}
                placeholder="Your Company Domain or Email"
                required={true}
                type="text"
              />
            </div>

            <div>
              <div className="text-right overflow-hidden sw-flex sw-justify-end">
                <ButtonPrimary type="submit" className="sw-mr-4">
                  {translate('sessions.log_in')}
                </ButtonPrimary>
                <ButtonLink onClick={goToBaseUrl}> {translate('back')} </ButtonLink>
              </div>
            </div>
          </form>
        </div>
      </Card>
    </div>
  );
}
