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

import * as React from 'react';
import { addGlobalErrorMessage } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location } from '~sonar-aligned/types/router';
import { logIn } from '../../../api/auth';
import { getLoginMessage, getAccessConsentMessage } from '../../../api/settings';
import { getIdentityProviders } from '../../../api/users';
import { translate } from '../../../helpers/l10n';
import { getReturnUrl } from '../../../helpers/urls';
import { IdentityProvider } from '../../../types/types';
import Login from './Login';

interface Props {
  location: Location;
}
interface State {
  identityProviders: IdentityProvider[];
  loading: boolean;
  message?: string;
  accessConsentMessage?: string;
}

export class LoginContainer extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    identityProviders: [],
    loading: true,
  };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async loadData() {
    await Promise.all([this.loadIdentityProviders(), this.loadLoginMessage(), this.loadAccessConsentMessage()]);
    this.setState({ loading: false });
  }

  loadIdentityProviders() {
    return getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({ identityProviders });
        }
      },
      () => {
        /* already handled */
      },
    );
  }

  async loadLoginMessage() {
    try {
      const { message } = await getLoginMessage();

      if (this.mounted) {
        this.setState({ message });
      }
    } catch (_) {
      /* already handled */
    }
  }

  async loadAccessConsentMessage() {
      try {
        const { message } = await getAccessConsentMessage();

        if (this.mounted) {
          this.setState({ accessConsentMessage: message });
        }
      } catch (_) {
        /* already handled */
      }
    }

  handleSuccessfulLogin = () => {
    window.location.replace(getReturnUrl(this.props.location));
  };

  handleSubmit = (id: string, password: string) => {
    return logIn(id, password)
      .then(this.handleSuccessfulLogin).then(()=>sessionStorage.removeItem('chatMessages'))
      .catch(() => {
        addGlobalErrorMessage(translate('login.authentication_failed'));
        return Promise.reject();
      });
  };

  render() {
    const { location } = this.props;
    const { identityProviders, loading, message, accessConsentMessage } = this.state;

    return (
      <Login
        identityProviders={identityProviders}
        loading={loading}
        message={message}
        onSubmit={this.handleSubmit}
        location={location}
        accessConsentMessage={accessConsentMessage}
      />
    );
  }
}

export default withRouter(LoginContainer);
