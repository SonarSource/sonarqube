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
import { getRepositories, provisionProject } from '../../../api/alm-integration';
import { IdentityProvider, AlmRepository } from '../../../app/types';
import { SubmitButton } from '../../../components/ui/buttons';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  identityProvider: IdentityProvider;
  onProjectCreate: (projectKeys: string[]) => void;
}

interface State {
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
    loading: true,
    repositories: [],
    selectedRepositories: {},
    submitting: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchRepositories();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRepositories = () => {
    getRepositories().then(
      ({ almIntegration, repositories }) => {
        if (this.mounted) {
          this.setState({ ...almIntegration, loading: false, repositories });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (this.isValid()) {
      const { selectedRepositories } = this.state;
      this.setState({ submitting: true });
      provisionProject({
        installationKeys: Object.keys(selectedRepositories).filter(key =>
          Boolean(selectedRepositories[key])
        )
      }).then(
        ({ projects }) => this.props.onProjectCreate(projects.map(project => project.projectKey)),
        () => {
          if (this.mounted) {
            this.setState({ loading: true, submitting: false });
            this.fetchRepositories();
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

  renderContent = () => {
    const { identityProvider } = this.props;
    const { selectedRepositories, submitting } = this.state;

    if (this.state.installed) {
      return (
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
            {translate('create')}
          </SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      );
    }
    return (
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
    );
  };

  render() {
    const { identityProvider } = this.props;
    const { loading } = this.state;

    return (
      <>
        <p className="alert alert-info width-60 big-spacer-bottom">
          {translateWithParameters(
            'onboarding.create_project.beta_feature_x',
            identityProvider.name
          )}
        </p>
        {loading ? <DeferredSpinner /> : this.renderContent()}
      </>
    );
  }
}
