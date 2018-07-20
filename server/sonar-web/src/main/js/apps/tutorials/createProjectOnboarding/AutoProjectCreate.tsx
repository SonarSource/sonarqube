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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import IdentityProviderLink from '../../../components/ui/IdentityProviderLink';
import { getIdentityProviders } from '../../../api/users';
import { getRepositories } from '../../../api/alm-integration';
import { translateWithParameters } from '../../../helpers/l10n';
import { IdentityProvider, LoggedInUser } from '../../../app/types';

interface Props {
  currentUser: LoggedInUser;
}

interface State {
  identityProviders: IdentityProvider[];
  installationUrl?: string;
  installed?: boolean;
  loading: boolean;
}

export default class AutoProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { identityProviders: [], loading: true };

  componentDidMount() {
    this.mounted = true;
    Promise.all([this.fetchIdentityProviders(), this.fetchRepositories()]).then(
      this.stopLoading,
      this.stopLoading
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchIdentityProviders = () => {
    return getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({ identityProviders });
        }
      },
      () => {
        return Promise.resolve();
      }
    );
  };

  fetchRepositories = () => {
    return getRepositories().then(({ installation }) => {
      if (this.mounted) {
        this.setState({
          installationUrl: installation.installationUrl,
          installed: installation.enabled
        });
      }
    });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    if (this.state.loading) {
      return <DeferredSpinner />;
    }

    const { currentUser } = this.props;
    const identityProvider = this.state.identityProviders.find(
      identityProvider => identityProvider.key === currentUser.externalProvider
    );

    if (!identityProvider) {
      return null;
    }

    return (
      <>
        <p className="alert alert-info width-60 big-spacer-bottom">
          {translateWithParameters(
            'onboarding.create_project.beta_feature_x',
            identityProvider.name
          )}
        </p>
        {this.state.installed ? (
          'Repositories list'
        ) : (
          <div>
            <p className="spacer-bottom">
              {translateWithParameters(
                'onboarding.create_project.install_app_x',
                identityProvider.name
              )}
            </p>
            <IdentityProviderLink
              className="display-inline-block"
              identityProvider={identityProvider}
              small={true}
              url={this.state.installationUrl}>
              {translateWithParameters(
                'onboarding.create_project.install_app_x.button',
                identityProvider.name
              )}
            </IdentityProviderLink>
          </div>
        )}
      </>
    );
  }
}
