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
import { getRepositories, provisionProject } from '../../../api/alm-integration';
import { AlmApplication, AlmRepository } from '../../../app/types';
import { SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  almApplication: AlmApplication;
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  organization: string;
}

type SelectedRepositories = { [key: string]: AlmRepository | undefined };

interface State {
  loading: boolean;
  repositories: AlmRepository[];
  selectedRepositories: SelectedRepositories;
  submitting: boolean;
}

export default class RemoteRepositories extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, repositories: [], selectedRepositories: {}, submitting: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchRepositories();
  }

  componentDidUpdate(prevProps: Props) {
    const { organization } = this.props;
    if (prevProps.organization !== organization) {
      this.setState({ loading: true });
      this.fetchRepositories();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRepositories = () => {
    const { organization } = this.props;
    return getRepositories({ organization }).then(
      ({ repositories }) => {
        if (this.mounted) {
          this.setState({ loading: false, repositories });
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
        installationKeys: Object.keys(selectedRepositories).filter(key => {
          const repositories = selectedRepositories[key];
          return repositories && !repositories.private;
        }),
        organization: this.props.organization
      }).then(
        ({ projects }) =>
          this.props.onProjectCreate(
            projects.map(project => project.projectKey),
            this.props.organization
          ),
        this.handleProvisionFail
      );
    }
  };

  handleProvisionFail = () => {
    return this.fetchRepositories().then(() => {
      if (this.mounted) {
        this.setState(({ repositories, selectedRepositories }) => {
          const updateSelectedRepositories: SelectedRepositories = {};
          Object.keys(selectedRepositories).forEach(installationKey => {
            const newRepository = repositories.find(r => r.installationKey === installationKey);
            if (newRepository && !newRepository.linkedProjectKey) {
              updateSelectedRepositories[newRepository.installationKey] = newRepository;
            }
          });
          return { selectedRepositories: updateSelectedRepositories, submitting: false };
        });
      }
    });
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

  render() {
    const { loading, selectedRepositories, submitting } = this.state;
    const { almApplication } = this.props;
    return (
      <DeferredSpinner loading={loading}>
        <form onSubmit={this.handleFormSubmit}>
          <div className="form-field">
            <ul>
              {this.state.repositories.map(repo => (
                <li className="big-spacer-bottom" key={repo.installationKey}>
                  <AlmRepositoryItem
                    identityProvider={almApplication}
                    repository={repo}
                    selected={Boolean(selectedRepositories[repo.installationKey])}
                    toggleRepository={this.toggleRepository}
                  />
                </li>
              ))}
            </ul>
          </div>
          <SubmitButton disabled={!this.isValid() || submitting}>{translate('setup')}</SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      </DeferredSpinner>
    );
  }
}
