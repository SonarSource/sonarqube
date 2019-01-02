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
import { getIdentityProviders } from '../../../api/users';
import * as theme from '../../../app/theme';
import { getTextColor } from '../../../helpers/colors';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  location: {
    query: {
      login: string;
      providerKey: string;
      providerName: string;
      oldLogin: string;
      oldOrganizationKey: string;
    };
  };
}

interface State {
  identityProviders: T.IdentityProvider[];
  loading: boolean;
}

export default class UpdateLogin extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { identityProviders: [], loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders = () => {
    this.setState({ loading: true });
    getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({ identityProviders, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  renderIdentityProvier = (provider: string, login: string) => {
    const identityProvider = this.state.identityProviders.find(p => p.key === provider);

    return identityProvider ? (
      <div
        className="identity-provider"
        style={{
          backgroundColor: identityProvider.backgroundColor,
          color: getTextColor(identityProvider.backgroundColor, theme.secondFontColor)
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
    const { query } = this.props.location;

    return (
      <div className="page-wrapper-simple" id="bd">
        <div className="page-simple" id="nonav">
          <div className="big-spacer-bottom js-provider-name">
            <p className="little-spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('sessions.update_login.1')}
                id="sessions.update_login.1"
                values={{ providerName: <strong>{query.providerName}</strong> }}
              />
            </p>
          </div>

          <div className="big-spacer-bottom js-new-account">
            <p className="little-spacer-bottom">{translate('sessions.update_login.2')}</p>
            {this.renderIdentityProvier(query.providerKey, query.login)}
          </div>

          <Alert variant="warning">
            {translate('sessions.update_login.3')}
            <ul className="list-styled">
              <li className="spacer-top js-old-organization-key">
                <FormattedMessage
                  defaultMessage={translate('sessions.update_login.4')}
                  id="sessions.update_login.4"
                  values={{ organizationKey: <strong>{query.oldOrganizationKey}</strong> }}
                />
              </li>
              <li className="spacer-top js-old-login">
                <FormattedMessage
                  defaultMessage={translate('sessions.update_login.5')}
                  id="sessions.update_login.5"
                  values={{ login: <strong>{query.oldLogin}</strong> }}
                />
              </li>
            </ul>
          </Alert>

          <div className="big-spacer-top text-right">
            <a
              className="button js-continue"
              href={`${getBaseUrl()}/sessions/init/${query.providerKey}?allowUpdateLogin=true`}>
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
