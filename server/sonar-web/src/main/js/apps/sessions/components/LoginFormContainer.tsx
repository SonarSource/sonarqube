/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import LoginForm from './LoginForm';
import { doLogin } from '../../../store/rootActions';
import { tryGetGlobalNavigation } from '../../../api/nav';
import { IdentityProvider, getIdentityProviders } from '../../../api/users';
import { getReturnUrl } from '../../../helpers/urls';

interface Props {
  doLogin: (login: string, password: string) => Promise<void>;
  location: { hash?: string; pathName: string; query: { return_to?: string } };
}

interface State {
  identityProviders?: IdentityProvider[];
  onSonarCloud: boolean;
}

class LoginFormContainer extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { onSonarCloud: false };

  componentDidMount() {
    this.mounted = true;
    Promise.all([
      getIdentityProviders(),
      tryGetGlobalNavigation()
    ]).then(([identityProvidersResponse, appState]) => {
      if (this.mounted) {
        this.setState({
          onSonarCloud:
            appState.settings && appState.settings['sonar.sonarcloud.enabled'] === 'true',
          identityProviders: identityProvidersResponse.identityProviders
        });
      }
    });
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSuccessfulLogin = () => {
    window.location.href = getReturnUrl(
      this.props.location.query['return_to'],
      this.props.location.hash
    );
  };

  handleSubmit = (login: string, password: string) => {
    this.props.doLogin(login, password).then(this.handleSuccessfulLogin, () => {});
  };

  render() {
    const { identityProviders, onSonarCloud } = this.state;
    if (!identityProviders) {
      return null;
    }

    return (
      <LoginForm
        identityProviders={identityProviders}
        onSonarCloud={onSonarCloud}
        onSubmit={this.handleSubmit}
        returnTo={getReturnUrl(this.props.location.query['return_to'], this.props.location.hash)}
      />
    );
  }
}

const mapStateToProps = null;
const mapDispatchToProps = { doLogin };

export default connect(mapStateToProps, mapDispatchToProps)(LoginFormContainer as any);
