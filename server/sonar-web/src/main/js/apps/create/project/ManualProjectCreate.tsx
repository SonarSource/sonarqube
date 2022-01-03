/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { debounce } from 'lodash';
import * as React from 'react';
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { doesComponentExists } from '../../../api/components';
import './ManualProjectCreate.css';
import { isSonarCloud } from "../../../helpers/system";
import { getBaseUrl } from "sonar-ui-common/helpers/urls";
import OrganizationInput from './OrganizationInput';

interface Props {
  onProjectCreate: (projectKeys: string[]) => void;
  organization?: string;
  userOrganizations?: T.Organization[];
}

interface State {
  projectName: string;
  projectNameChanged: boolean;
  projectNameError?: string;
  projectKey: string;
  projectKeyError?: string;
  selectedOrganization?: T.Organization;
  selectedVisibility?: T.Visibility;
  submitting: boolean;
  touched: boolean;
  validating: boolean;
}

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      projectKey: '',
      projectName: '',
      projectNameChanged: false,
      selectedOrganization: this.getInitialSelectedOrganization(props),
      submitting: false,
      touched: false,
      validating: false
    };
    this.checkFreeKey = debounce(this.checkFreeKey, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkFreeKey = (key: string) => {
    return doesComponentExists({ component: key })
      .then(alreadyExist => {
        if (this.mounted && key === this.state.projectKey) {
          if (!alreadyExist) {
            this.setState({ projectKeyError: undefined, validating: false });
          } else {
            this.setState({
              projectKeyError: translate('onboarding.create_project.project_key.taken'),
              touched: true,
              validating: false
            });
          }
        }
      })
      .catch(() => {
        if (this.mounted && key === this.state.projectKey) {
          this.setState({ projectKeyError: undefined, validating: false });
        }
      });
  };

  canSubmit(state: State) {
    return Boolean(
        state.selectedOrganization
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
      window.location.href =
          getBaseUrl() + `/organizations/${this.state.selectedOrganization.key}/extension/developer/projects`;
    }
  };

  handleOrganizationSelect = ({ key }: T.Organization) => {
    const selectedOrganization = this.getOrganization(key);
    this.setState({
      selectedOrganization
    });
  };

  render() {
    const { selectedOrganization, submitting } = this.state;

    return (
      <div className="create-project-manual">
        <div className="flex-1 huge-spacer-right">
          <form className="manual-project-create" onSubmit={this.handleFormSubmit}>
            {isSonarCloud() &&
            this.props.userOrganizations && (
                <OrganizationInput
                    onChange={this.handleOrganizationSelect}
                    organization={selectedOrganization ? selectedOrganization.key : ''}
                    organizations={this.props.userOrganizations}
                />
            )}
            <SubmitButton disabled={!this.canSubmit(this.state) || submitting}>
              {translate('set_up')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </form>
        </div>
      </div>
    );
  }
}
