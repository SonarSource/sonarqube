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
import { sortBy } from 'lodash';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import Select from '../../../components/controls/Select';
import { SubmitButton } from '../../../components/ui/buttons';
import { LoggedInUser, Organization } from '../../../app/types';
import { fetchMyOrganizations } from '../../account/organizations/actions';
import { getMyOrganizations, Store } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import { createProject } from '../../../api/components';
import DeferredSpinner from '../../../components/common/DeferredSpinner';

interface StateProps {
  userOrganizations: Organization[];
}

interface DispatchProps {
  fetchMyOrganizations: () => Promise<void>;
}

interface OwnProps {
  currentUser: LoggedInUser;
  onProjectCreate: (projectKeys: string[]) => void;
}

type Props = OwnProps & StateProps & DispatchProps;

interface State {
  projectName: string;
  projectKey: string;
  selectedOrganization: string;
  submitting: boolean;
}

export class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectName: '',
      projectKey: '',
      selectedOrganization:
        props.userOrganizations.length === 1 ? props.userOrganizations[0].key : '',
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
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

  handleOrganizationSelect = ({ value }: { value: string }) => {
    this.setState({ selectedOrganization: value });
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
          <div className="form-field">
            <label htmlFor="select-organization">
              {translate('onboarding.create_project.organization')}
              <em className="mandatory">*</em>
            </label>
            <Select
              autoFocus={true}
              className="input-super-large"
              clearable={false}
              id="select-organization"
              onChange={this.handleOrganizationSelect}
              options={sortBy(this.props.userOrganizations, o => o.name.toLowerCase()).map(
                organization => ({
                  label: organization.name,
                  value: organization.key
                })
              )}
              required={true}
              value={this.state.selectedOrganization}
            />
            <Link className="big-spacer-left js-new-org" to="/create-organization">
              {translate('onboarding.create_project.create_new_org')}
            </Link>
          </div>
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
          <SubmitButton disabled={!this.isValid() || submitting}>
            {translate('create')}
          </SubmitButton>
          <DeferredSpinner className="spacer-left" loading={submitting} />
        </form>
      </>
    );
  }
}

const mapDispatchToProps = ({
  fetchMyOrganizations
} as any) as DispatchProps;

const mapStateToProps = (state: Store): StateProps => {
  return {
    userOrganizations: getMyOrganizations(state)
  };
};
export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ManualProjectCreate);
