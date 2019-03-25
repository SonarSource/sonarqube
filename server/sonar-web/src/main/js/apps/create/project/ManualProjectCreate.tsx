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
import * as React from 'react';
import * as classNames from 'classnames';
import OrganizationInput from './OrganizationInput';
import { createProject } from '../../../api/components';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton } from '../../../components/ui/buttons';
import ProjectKeyInput from '../components/ProjectKeyInput';
import VisibilitySelector from '../../../components/common/VisibilitySelector';
import UpgradeOrganizationBox from '../components/UpgradeOrganizationBox';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import './ManualProjectCreate.css';

interface Props {
  currentUser: T.LoggedInUser;
  fetchMyOrganizations?: () => Promise<void>;
  onProjectCreate: (projectKeys: string[]) => void;
  organization?: string;
  userOrganizations?: T.Organization[];
}

interface State {
  projectName?: string;
  projectNameChanged: boolean;
  projectKey?: string;
  selectedOrganization?: T.Organization;
  selectedVisibility?: T.Visibility;
  submitting: boolean;
}

type ValidState = State & Required<Pick<State, 'projectKey' | 'projectName'>>;

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectNameChanged: false,
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

  canChoosePrivate = (selectedOrganization: T.Organization | undefined) => {
    return Boolean(selectedOrganization && selectedOrganization.subscription === 'PAID');
  };

  canSubmit(state: State): state is ValidState {
    return Boolean(
      state.projectKey && state.projectName && (!isSonarCloud() || state.selectedOrganization)
    );
  }

  getInitialSelectedOrganization = (props: Props) => {
    if (props.organization) {
      return this.getOrganization(props.organization);
    } else if (props.userOrganizations && props.userOrganizations.length === 1) {
      return props.userOrganizations[0];
    } else {
      return undefined;
    }
  };

  getOrganization = (organizationKey: string) => {
    return (
      this.props.userOrganizations &&
      this.props.userOrganizations.find(({ key }: T.Organization) => key === organizationKey)
    );
  };

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { state } = this;
    if (this.canSubmit(state)) {
      this.setState({ submitting: true });
      createProject({
        project: state.projectKey,
        name: state.projectName || state.projectKey,
        organization: state.selectedOrganization && state.selectedOrganization.key,
        visibility: this.state.selectedVisibility
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

  handleOrganizationSelect = ({ key }: T.Organization) => {
    const selectedOrganization = this.getOrganization(key);
    let { selectedVisibility } = this.state;

    if (selectedVisibility === undefined) {
      selectedVisibility = this.canChoosePrivate(selectedOrganization) ? 'private' : 'public';
    }

    this.setState({
      selectedOrganization,
      selectedVisibility
    });
  };

  handleOrganizationUpgrade = () => {
    this.props.fetchMyOrganizations!().then(
      () => {
        this.setState(prevState => {
          if (prevState.selectedOrganization) {
            const selectedOrganization = this.getOrganization(prevState.selectedOrganization.key);
            return {
              selectedOrganization
            };
          }
          return null;
        });
      },
      () => {}
    );
  };

  handleProjectNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const projectName = event.currentTarget.value.trim();
    this.setState({ projectName, projectNameChanged: true });
  };

  handleProjectKeyChange = (projectKey?: string) => {
    this.setState(state => ({
      projectKey,
      projectName: state.projectNameChanged ? state.projectName : projectKey || ''
    }));
  };

  handleVisibilityChange = (selectedVisibility: T.Visibility) => {
    this.setState({ selectedVisibility });
  };

  render() {
    const { selectedOrganization, submitting } = this.state;
    const canChoosePrivate = this.canChoosePrivate(selectedOrganization);

    return (
      <div className="create-project">
        <div className="flex-1 huge-spacer-right">
          <form className="manual-project-create" onSubmit={this.handleFormSubmit}>
            {isSonarCloud() && this.props.userOrganizations && (
              <OrganizationInput
                onChange={this.handleOrganizationSelect}
                organization={selectedOrganization ? selectedOrganization.key : ''}
                organizations={this.props.userOrganizations}
              />
            )}
            <ProjectKeyInput
              className="form-field"
              initialValue={this.state.projectKey}
              onChange={this.handleProjectKeyChange}
            />
            <div className="form-field">
              <label htmlFor="project-name">
                <span className="text-middle">
                  <strong>{translate('onboarding.create_project.display_name')}</strong>
                  <em className="mandatory">*</em>
                </span>
                <HelpTooltip
                  className="spacer-left"
                  overlay={translate('onboarding.create_project.display_name.help')}
                />
              </label>
              <div className="little-spacer-top spacer-bottom">
                <input
                  className={'input-super-large'}
                  id="project-name"
                  maxLength={255}
                  minLength={1}
                  onChange={this.handleProjectNameChange}
                  type="text"
                  value={this.state.projectName}
                />
              </div>
              <div className="note abs-width-400">
                {translate('onboarding.create_project.display_name.description')}
              </div>
            </div>
            {isSonarCloud() && selectedOrganization && (
              <div
                className={classNames('visibility-select-wrapper', {
                  open: Boolean(this.state.selectedOrganization)
                })}>
                <VisibilitySelector
                  canTurnToPrivate={canChoosePrivate}
                  onChange={this.handleVisibilityChange}
                  showDetails={true}
                  visibility={canChoosePrivate ? this.state.selectedVisibility : 'public'}
                />
              </div>
            )}
            <SubmitButton disabled={!this.canSubmit(this.state) || submitting}>
              {translate('set_up')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </form>
        </div>

        {isSonarCloud() && selectedOrganization && (
          <div className="create-project-side-sticky">
            <UpgradeOrganizationBox
              className={classNames('animated', { open: !canChoosePrivate })}
              onOrganizationUpgrade={this.handleOrganizationUpgrade}
              organization={selectedOrganization}
            />
          </div>
        )}
      </div>
    );
  }
}
