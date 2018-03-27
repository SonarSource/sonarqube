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
import { BitbucketIcon } from '@atlaskit/logo';
import Spinner from '@atlaskit/spinner';
import { IdentityProvider } from '@sqcore/app/types';
import LoginButton from './LoginButton';
import { getIdentityProviders } from '../api';

interface Props {
  onReload: () => void;
}

interface State {
  identityProviders?: IdentityProvider[];
  loading: boolean;
}

export default class LoginForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders = () => {
    getIdentityProviders().then(
      identityProvidersResponse => {
        if (this.mounted) {
          this.setState({
            identityProviders: identityProvidersResponse.identityProviders,
            loading: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { identityProviders, loading } = this.state;

    if (loading) {
      return (
        <div className="huge-spacer-top">
          <Spinner size="large" />
        </div>
      );
    }

    const { onReload } = this.props;
    const bitbucketProvider =
      identityProviders && identityProviders.find(provider => provider.key === 'bitbucket');
    return (
      <>
        <p className="huge-spacer-top">
          You must be logged in SonarCloud to link to a project you administer:
        </p>
        <div className="ak-field-group">
          {bitbucketProvider && (
            <LoginButton
              appearance="primary"
              icon={<BitbucketIcon label={bitbucketProvider.name} size="small" />}
              onReload={onReload}
              sessionUrl={`sessions/init/${bitbucketProvider.key}`}>
              <span>Log in with {bitbucketProvider.name}</span>
            </LoginButton>
          )}

          <LoginButton appearance="link" onReload={onReload} sessionUrl={'sessions/new'}>
            {bitbucketProvider ? 'More options' : 'Log in on SonarCloud'}
          </LoginButton>
        </div>
      </>
    );
  }
}
