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
import LoginLink from './LoginLink';
import SonarCloudIcon from './SonarCloudIcon';
import * as theme from '../../../../../sonar-web/src/main/js/app/theme';
import { getIdentityProviders } from '../../../../../sonar-web/src/main/js/api/users';
import { getTextColor } from '../../../../../sonar-web/src/main/js/helpers/colors';
import { getBaseUrl } from '../../../../../sonar-web/src/main/js/helpers/urls';

interface Props {
  onReload: () => void;
  title?: string;
}

interface State {
  identityProviders?: T.IdentityProvider[];
}

export default class LoginForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    getIdentityProviders().then(
      identityProvidersResponse => {
        if (this.mounted) {
          this.setState({
            identityProviders: identityProvidersResponse.identityProviders
          });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  render() {
    const { onReload, title } = this.props;
    const { identityProviders } = this.state;
    const vstsProvider =
      identityProviders && identityProviders.find(provider => provider.key === 'microsoft');

    return (
      <div className="vsts-widget-login">
        {title && <SonarCloudIcon size={32} />}
        {title && <p className="login-message-text">{title}</p>}
        {identityProviders && (
          <section className="oauth-providers">
            {vstsProvider && (
              <LoginLink
                onReload={onReload}
                sessionUrl={`sessions/init/${vstsProvider.key}`}
                style={{
                  backgroundColor: vstsProvider.backgroundColor,
                  color: getTextColor(vstsProvider.backgroundColor, theme.secondFontColor)
                }}>
                <img
                  alt={vstsProvider.name}
                  height="20"
                  src={getBaseUrl() + vstsProvider.iconPath}
                  width="20"
                />
                <span>{vstsProvider.name} log in</span>
              </LoginLink>
            )}
          </section>
        )}

        <div className="text-center">
          <LoginLink onReload={onReload} sessionUrl={'sessions/new'}>
            {vstsProvider ? 'More options' : 'Log in on SonarCloud'}
          </LoginLink>
        </div>
      </div>
    );
  }
}
