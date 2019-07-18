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
import { FormattedMessage } from 'react-intl';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { getTextColor } from 'sonar-ui-common/helpers/colors';
import { getCookie } from 'sonar-ui-common/helpers/cookies';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { getIdentityProviders } from '../../../api/users';
import { colors } from '../../../app/theme';

interface State {
  identityProviders: T.IdentityProvider[];
}

export default class EmailAlreadyExists extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { identityProviders: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders = () => {
    getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({ identityProviders });
        }
      },
      () => {}
    );
  };

  getAuthError = (): {
    email?: string;
    login?: string;
    provider?: string;
    existingLogin?: string;
    existingProvider?: string;
  } => {
    const cookie = getCookie('AUTHENTICATION-ERROR');
    if (cookie) {
      return JSON.parse(decodeURIComponent(cookie));
    }
    return {};
  };

  renderIdentityProvier = (provider?: string, login?: string) => {
    const identityProvider = this.state.identityProviders.find(p => p.key === provider);

    return identityProvider ? (
      <div
        className="identity-provider"
        style={{
          backgroundColor: identityProvider.backgroundColor,
          color: getTextColor(identityProvider.backgroundColor, colors.secondFontColor)
        }}>
        <img
          alt={identityProvider.name}
          className="little-spacer-right"
          height="14"
          src={getBaseUrl() + identityProvider.iconPath}
          width="14"
        />
        {login}
      </div>
    ) : (
      <div>
        {provider !== 'sonarqube' && provider} {login}
      </div>
    );
  };

  render() {
    const authError = this.getAuthError();
    return (
      <div className="page-wrapper-simple" id="bd">
        <div className="page-simple" id="nonav">
          <div className="big-spacer-bottom js-existing-account">
            <p className="little-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('sessions.email_already_exists.1')}
                id="sessions.email_already_exists.1"
                values={{ email: <strong>{authError.email}</strong> }}
              />
            </p>
            {this.renderIdentityProvier(authError.existingProvider, authError.existingLogin)}
          </div>

          <div className="big-spacer-bottom js-new-account">
            <p className="little-spacer-bottom">{translate('sessions.email_already_exists.2')}</p>
            {this.renderIdentityProvier(authError.provider, authError.login)}
          </div>

          <Alert variant="warning">
            {translate('sessions.email_already_exists.3')}
            <ul className="list-styled">
              <li className="spacer-top">{translate('sessions.email_already_exists.4')}</li>
              <li className="spacer-top">{translate('sessions.email_already_exists.5')}</li>
              <li className="spacer-top">{translate('sessions.email_already_exists.6')}</li>
            </ul>
          </Alert>

          <div className="big-spacer-top text-right">
            <a
              className="button js-continue"
              href={`${getBaseUrl()}/sessions/init/${authError.provider}?allowEmailShift=true`}>
              {translate('continue')}
            </a>
            <a className="big-spacer-left js-cancel" href={getBaseUrl() + '/'}>
              {translate('cancel')}
            </a>
          </div>
        </div>
      </div>
    );
  }
}
