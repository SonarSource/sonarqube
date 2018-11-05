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
import OrganizationInput from './OrganizationInput';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton } from '../../../components/ui/buttons';
import { LoggedInUser, Organization } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { createProject } from '../../../api/components';

interface Props {
  currentUser: LoggedInUser;
  onProjectCreate: (projectKeys: string[]) => void;
  organization?: string;
  userOrganizations: Organization[];
}

interface State {
  projectName: string;
  projectKey: string;
  selectedOrganization: string;
  submitting: boolean;
}

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectName: '',
      projectKey: '',
      selectedOrganization: this.getInitialSelectedOrganization(props),
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getInitialSelectedOrganization(props: Props) {
    if (props.organization) {
      return props.organization;
    } else if (props.userOrganizations.length === 1) {
      return props.userOrganizations[0].key;
    } else {
      return '';
    }
  }

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (this.isValid()) {
      const { projectKey, projectName, selectedOrganization } = this.state;
      this.setState({ submitting: true });
      createProject({
        project: projectKey,
        name: projectName,
        organization: selectedOrganization
      }).then(
        ({ project }) => this.props.onProjectCreate([project.key]),
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        }
      );
    }
  };

  handleOrganizationSelect = ({ key }: Organization) => {
    this.setState({ selectedOrganization: key });
  };

  handleProjectNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ projectName: event.currentTarget.value });
  };

  handleProjectKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ projectKey: event.currentTarget.value });
  };

  isValid = () => {
    const { projectKey, projectName, selectedOrganization } = this.state;
    return Boolean(projectKey && projectName && selectedOrganization);
  };

  render() {
    const { submitting } = this.state;
    return (
      <>
        <form onSubmit={this.handleFormSubmit}>
          <OrganizationInput
            onChange={this.handleOrganizationSelect}
            organization={this.state.selectedOrganization}
            organizations={this.props.userOrganizations}
          />
          <div className="form-field">
            <label htmlFor="project-name">
              {translate('onboarding.create_project.project_name')}
              <em className="mandatory">*</em>
            </label>
            <input
              className="input-super-large"
              id="project-name"
              maxLength={400}
              minLength={1}
              onChange={this.handleProjectNameChange}
              required={true}
              type="text"
              value={this.state.projectName}
            />
          </div>
          <div className="form-field">
            <label htmlFor="project-key">
              {translate('onboarding.create_project.project_key')}
              <em className="mandatory">*</em>
            </label>
            <input
              className="input-super-large"
              id="project-key"
              maxLength={400}
              minLength={1}
              onChange={this.handleProjectKeyChange}
              required={true}
              type="text"
              value={this.state.projectKey}
            />
          </div>
          <SubmitButton disabled={!this.isValid() || submitting}>{translate('setup')}</SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      </>
    );
  }
}
