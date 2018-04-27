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
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import Login from './Login';
import LoginSonarCloud from './LoginSonarCloud';
import { doLogin } from '../../../store/rootActions';
import { getIdentityProviders } from '../../../api/users';
import { IdentityProvider } from '../../../app/types';
import { getBaseUrl } from '../../../helpers/urls';

interface OwnProps {
  location: {
    hash?: string;
    pathName: string;
    query: {
      advanced?: string;
      return_to?: string; // eslint-disable-line camelcase
    };
  };
}

interface DispatchToProps {
  doLogin: (login: string, password: string) => Promise<void>;
}

type Props = OwnProps & DispatchToProps;

interface State {
  identityProviders?: IdentityProvider[];
}

class LoginContainer extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    onSonarCloud: PropTypes.bool
  };

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

  getReturnUrl = () => {
    const { location } = this.props;
    const queryReturnTo = location.query['return_to'];
    return queryReturnTo ? `${queryReturnTo}${location.hash}` : `${getBaseUrl()}/`;
  };

  handleSuccessfulLogin = () => {
    window.location.href = this.getReturnUrl();
  };

  handleSubmit = (login: string, password: string) => {
    return this.props.doLogin(login, password).then(this.handleSuccessfulLogin, () => {});
  };

  render() {
    const { location } = this.props;
    const { identityProviders } = this.state;
    if (!identityProviders) {
      return null;
    }

    if (this.context.onSonarCloud) {
      return (
        <LoginSonarCloud
          identityProviders={identityProviders}
          onSubmit={this.handleSubmit}
          returnTo={this.getReturnUrl()}
          showForm={location.query['advanced'] !== undefined}
        />
      );
    }

    return (
      <Login
        identityProviders={identityProviders}
        onSubmit={this.handleSubmit}
        returnTo={this.getReturnUrl()}
      />
    );
  }
}

const mapStateToProps = null;
const mapDispatchToProps = { doLogin: doLogin as any };

export default connect<{}, DispatchToProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  LoginContainer
);
