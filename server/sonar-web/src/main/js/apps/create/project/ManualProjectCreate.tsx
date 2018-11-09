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
import { createProject } from '../../../api/components';
import { LoggedInUser, Organization } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import ProjectKeyInput from '../components/ProjectKeyInput';
import ProjectNameInput from '../components/ProjectNameInput';

interface Props {
  currentUser: LoggedInUser;
  onProjectCreate: (projectKeys: string[]) => void;
  organization?: string;
  userOrganizations: Organization[];
}

interface State {
  projectName?: string;
  projectKey?: string;
  selectedOrganization: string;
  submitting: boolean;
}

type ValidState = State & Required<Pick<State, 'projectName' | 'projectKey'>>;

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
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

  canSubmit(state: State): state is ValidState {
    return Boolean(state.projectKey && state.projectName && state.selectedOrganization);
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
    const { state } = this;
    if (this.canSubmit(state)) {
      this.setState({ submitting: true });
      createProject({
        project: state.projectKey,
        name: state.projectName,
        organization: state.selectedOrganization
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

  handleProjectNameChange = (projectName?: string) => {
    this.setState({ projectName });
  };

  handleProjectKeyChange = (projectKey?: string) => {
    this.setState({ projectKey });
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
          <ProjectKeyInput
            className="form-field"
            initialValue={this.state.projectKey}
            onChange={this.handleProjectKeyChange}
          />
          <ProjectNameInput
            className="form-field"
            initialValue={this.state.projectName}
            onChange={this.handleProjectNameChange}
          />
          <SubmitButton disabled={!this.canSubmit(this.state) || submitting}>
            {translate('setup')}
          </SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      </>
    );
  }
}
