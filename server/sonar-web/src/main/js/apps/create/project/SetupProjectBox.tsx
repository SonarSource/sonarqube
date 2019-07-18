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
import * as classNames from 'classnames';
import { partition } from 'lodash';
import * as React from 'react';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { provisionProject } from '../../../api/alm-integration';

interface Props {
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  onProvisionFail: () => Promise<void>;
  organization: T.Organization;
  selectedRepositories: T.AlmRepository[];
}

interface State {
  submitting: boolean;
}

export default class SetupProjectBox extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { submitting: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  canSubmit = () => {
    return !this.state.submitting && this.props.selectedRepositories.length > 0;
  };

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (this.canSubmit()) {
      const { selectedRepositories } = this.props;
      this.setState({ submitting: true });
      provisionProject({
        installationKeys: selectedRepositories.map(repo => repo.installationKey),
        organization: this.props.organization.key
      }).then(
        ({ projects }) =>
          this.props.onProjectCreate(
            projects.map(project => project.projectKey),
            this.props.organization.key
          ),
        this.handleProvisionFail
      );
    }
  };

  handleProvisionFail = () => {
    return this.props.onProvisionFail().then(() => {
      if (this.mounted) {
        this.setState({ submitting: false });
      }
    });
  };

  render() {
    const { selectedRepositories } = this.props;
    const hasSelectedRepositories = selectedRepositories.length > 0;
    const [privateRepos = [], publicRepos = []] = partition(
      selectedRepositories,
      repo => repo.private
    );
    return (
      <form
        className={classNames('create-project-setup boxed-group', {
          open: hasSelectedRepositories
        })}
        onSubmit={this.handleFormSubmit}>
        <div className="boxed-group-header">
          <h2 className="spacer-top">
            {selectedRepositories.length > 1
              ? translateWithParameters(
                  'onboarding.create_project.x_repositories_selected',
                  selectedRepositories.length
                )
              : translate('onboarding.create_project.1_repository_selected')}
          </h2>
        </div>
        <div className="boxed-group-inner">
          <div className="flex-1">
            {publicRepos.length === 1 && (
              <p>{translate('onboarding.create_project.1_repository_created_as_public')}</p>
            )}
            {publicRepos.length > 1 && (
              <p>
                {translateWithParameters(
                  'onboarding.create_project.x_repository_created_as_public',
                  publicRepos.length
                )}
              </p>
            )}
            {privateRepos.length === 1 && (
              <p>{translate('onboarding.create_project.1_repository_created_as_private')}</p>
            )}
            {privateRepos.length > 1 && (
              <p>
                {translateWithParameters(
                  'onboarding.create_project.x_repository_created_as_private',
                  privateRepos.length
                )}
              </p>
            )}
          </div>
          <div>
            <SubmitButton className="button-large" disabled={this.state.submitting}>
              {translate('set_up')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={this.state.submitting} />
          </div>
        </div>
      </form>
    );
  }
}
