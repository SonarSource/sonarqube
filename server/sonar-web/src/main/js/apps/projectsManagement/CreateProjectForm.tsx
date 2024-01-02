/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import { createProject } from '../../api/project-management';
import { getValue } from '../../api/settings';
import Link from '../../components/common/Link';
import VisibilitySelector from '../../components/common/VisibilitySelector';
import Modal from '../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../components/controls/buttons';
import { Alert } from '../../components/ui/Alert';
import MandatoryFieldMarker from '../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../helpers/l10n';
import { getProjectUrl } from '../../helpers/urls';
import { Visibility } from '../../types/component';
import { GlobalSettingKeys } from '../../types/settings';

interface Props {
  defaultProjectVisibility?: Visibility;
  onClose: () => void;
  onProjectCreated: () => void;
}

interface State {
  createdProject?: { key: string; name: string };
  key: string;
  loading: boolean;
  name: string;
  visibility?: Visibility;
  // add index declaration to be able to do `this.setState({ [name]: value });`
  [x: string]: any;
  mainBranchName: string;
}

export default class CreateProjectForm extends React.PureComponent<Props, State> {
  closeButton?: HTMLElement | null;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      key: '',
      loading: false,
      name: '',
      visibility: props.defaultProjectVisibility,
      mainBranchName: 'main',
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchMainBranchName();
  }

  componentDidUpdate() {
    // wrap with `setTimeout` because of https://github.com/reactjs/react-modal/issues/338
    setTimeout(() => {
      if (this.closeButton) {
        this.closeButton.focus();
      }
    }, 0);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMainBranchName = async () => {
    const mainBranchName = await getValue({ key: GlobalSettingKeys.MainBranchName });

    if (this.mounted && mainBranchName.value !== undefined) {
      this.setState({ mainBranchName: mainBranchName.value });
    }
  };

  handleInputChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { name, value } = event.currentTarget;
    this.setState({ [name]: value });
  };

  handleVisibilityChange = (visibility: Visibility) => {
    this.setState({ visibility });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { name, key, mainBranchName, visibility } = this.state;

    const data = {
      name,
      project: key,
      mainBranch: mainBranchName,
      visibility,
    };

    this.setState({ loading: true });
    createProject(data).then(
      (response) => {
        if (this.mounted) {
          this.setState({ createdProject: response.project, loading: false });
          this.props.onProjectCreated();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  render() {
    const { defaultProjectVisibility } = this.props;
    const { createdProject } = this.state;
    const header = translate('qualifiers.create.TRK');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        {createdProject ? (
          <div>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>

            <div className="modal-body">
              <Alert variant="success">
                <FormattedMessage
                  defaultMessage={translate(
                    'projects_management.project_has_been_successfully_created',
                  )}
                  id="projects_management.project_has_been_successfully_created"
                  values={{
                    project: (
                      <Link to={getProjectUrl(createdProject.key)}>{createdProject.name}</Link>
                    ),
                  }}
                />
              </Alert>
            </div>

            <footer className="modal-foot">
              <ResetButtonLink
                id="create-project-close"
                innerRef={(node) => (this.closeButton = node)}
                onClick={this.props.onClose}
              >
                {translate('close')}
              </ResetButtonLink>
            </footer>
          </div>
        ) : (
          <form id="create-project-form" onSubmit={this.handleFormSubmit}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>

            <div className="modal-body">
              <MandatoryFieldsExplanation className="modal-field" />
              <div className="modal-field">
                <label htmlFor="create-project-name">
                  {translate('onboarding.create_project.display_name')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  autoFocus
                  id="create-project-name"
                  maxLength={2000}
                  name="name"
                  onChange={this.handleInputChange}
                  required
                  type="text"
                  value={this.state.name}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-project-key">
                  {translate('onboarding.create_project.project_key')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  id="create-project-key"
                  maxLength={400}
                  name="key"
                  onChange={this.handleInputChange}
                  required
                  type="text"
                  value={this.state.key}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-project-main-branch-name">
                  {translate('onboarding.create_project.main_branch_name')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  id="create-project-main-branch-name"
                  maxLength={400}
                  name="mainBranchName"
                  onChange={this.handleInputChange}
                  required
                  type="text"
                  value={this.state.mainBranchName}
                />
              </div>
              <div className="modal-field">
                <label>{translate('visibility')}</label>
                <VisibilitySelector
                  canTurnToPrivate={defaultProjectVisibility !== undefined}
                  className="little-spacer-top"
                  onChange={this.handleVisibilityChange}
                  visibility={this.state.visibility}
                />
              </div>
            </div>

            <footer className="modal-foot">
              {this.state.loading && <i className="spinner spacer-right" />}
              <SubmitButton disabled={this.state.loading} id="create-project-submit">
                {translate('create')}
              </SubmitButton>
              <ResetButtonLink id="create-project-cancel" onClick={this.props.onClose}>
                {translate('cancel')}
              </ResetButtonLink>
            </footer>
          </form>
        )}
      </Modal>
    );
  }
}
