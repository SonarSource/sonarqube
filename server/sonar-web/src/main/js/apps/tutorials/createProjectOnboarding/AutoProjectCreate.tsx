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
import AlmRepositoryItem from './AlmRepositoryItem';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import IdentityProviderLink from '../../../components/ui/IdentityProviderLink';
import { getIdentityProviders } from '../../../api/users';
import { getRepositories, provisionProject } from '../../../api/alm-integration';
import { IdentityProvider, LoggedInUser, AlmRepository } from '../../../app/types';
import { ProjectBase } from '../../../api/components';
import { SubmitButton } from '../../../components/ui/buttons';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  currentUser: LoggedInUser;
  onProjectCreate: (project: ProjectBase[]) => void;
}

interface State {
  identityProviders: IdentityProvider[];
  installationUrl?: string;
  installed?: boolean;
  loading: boolean;
  repositories: AlmRepository[];
  selectedRepositories: { [key: string]: AlmRepository | undefined };
  submitting: boolean;
}

export default class AutoProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    identityProviders: [],
    loading: true,
    repositories: [],
    selectedRepositories: {},
    submitting: false
  };

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
    return getRepositories().then(({ almIntegration, repositories }) => {
      if (this.mounted) {
        this.setState({ ...almIntegration, repositories });
      }
    });
  };

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (this.isValid()) {
      const { selectedRepositories } = this.state;
      this.setState({ submitting: true });
      provisionProject({
        repositories: Object.keys(selectedRepositories).filter(key =>
          Boolean(selectedRepositories[key])
        )
      }).then(
        ({ project }) => this.props.onProjectCreate([project]),
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
            this.reloadRepositories();
          }
        }
      );
    }
  };

  isValid = () => {
    return this.state.repositories.some(repo =>
      Boolean(this.state.selectedRepositories[repo.installationKey])
    );
  };

  reloadRepositories = () => {
    this.setState({ loading: true });
    this.fetchRepositories().then(this.stopLoading, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  toggleRepository = (repository: AlmRepository) => {
    this.setState(({ selectedRepositories }) => ({
      selectedRepositories: {
        ...selectedRepositories,
        [repository.installationKey]: selectedRepositories[repository.installationKey]
          ? undefined
          : repository
      }
    }));
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

    const { selectedRepositories, submitting } = this.state;

    return (
      <>
        <p className="alert alert-info width-60 big-spacer-bottom">
          {translateWithParameters(
            'onboarding.create_project.beta_feature_x',
            identityProvider.name
          )}
        </p>
        {this.state.installed ? (
          <form onSubmit={this.handleFormSubmit}>
            <ul>
              {this.state.repositories.map(repo => (
                <li className="big-spacer-bottom" key={repo.installationKey}>
                  <AlmRepositoryItem
                    identityProvider={identityProvider}
                    repository={repo}
                    selected={Boolean(selectedRepositories[repo.installationKey])}
                    toggleRepository={this.toggleRepository}
                  />
                </li>
              ))}
            </ul>
            <SubmitButton disabled={!this.isValid() || submitting}>
              {translate('onboarding.create_project.create_project')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </form>
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
