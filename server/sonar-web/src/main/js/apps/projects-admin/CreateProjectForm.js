/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import Modal from 'react-modal';
import { Link } from 'react-router';
import UpgradeOrganizationBox from '../../components/common/UpgradeOrganizationBox';
import VisibilitySelector from '../../components/common/VisibilitySelector';
import { createProject } from '../../api/components';
import { translate } from '../../helpers/l10n';
import { getProjectUrl } from '../../helpers/urls';
import type { Organization } from '../../store/organizations/duck';

type Props = {|
  onClose: () => void,
  onProjectCreated: () => void,
  onRequestFail: Object => void,
  organization?: Organization
|};

type State = {
  branch: string,
  createdProject?: Object,
  key: string,
  loading: boolean,
  name: string,
  visibility: string
};

export default class CreateProjectForm extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      branch: '',
      key: '',
      loading: false,
      name: '',
      visibility: props.organization ? props.organization.projectVisibility : 'public'
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: Event) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleInputChange = (event: { currentTarget: HTMLInputElement }) => {
    const { name, value } = event.currentTarget;
    this.setState({ [name]: value });
  };

  handleVisibilityChange = (visibility: string) => {
    this.setState({ visibility });
  };

  handleFormSubmit = (event: Event) => {
    event.preventDefault();

    const data: { [string]: string } = {
      name: this.state.name,
      branch: this.state.branch,
      project: this.state.key,
      visibility: this.state.visibility
    };
    if (this.props.organization) {
      data.organization = this.props.organization.key;
    }

    this.setState({ loading: true });
    createProject(data).then(
      response => {
        if (this.mounted) {
          this.setState({ createdProject: response.project, loading: false });
          this.props.onProjectCreated();
        }
      },
      error => {
        if (this.mounted) {
          this.setState({ loading: false });
          this.props.onRequestFail(error);
        }
      }
    );
  };

  render() {
    const { organization } = this.props;
    const { createdProject } = this.state;

    return (
      <Modal
        isOpen={true}
        contentLabel="modal form"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        {createdProject
          ? <div>
              <header className="modal-head">
                <h2>{translate('qualifiers.create.TRK')}</h2>
              </header>

              <div className="modal-body">
                <div className="alert alert-success">
                  Project
                  {' '}
                  <Link to={getProjectUrl(createdProject.key)}>{createdProject.name}</Link>
                  {' '}
                  has been successfully created.
                </div>
              </div>

              <footer className="modal-foot">
                <a href="#" id="create-project-close" onClick={this.handleCancelClick}>
                  {translate('close')}
                </a>
              </footer>
            </div>
          : <form id="create-project-form" onSubmit={this.handleFormSubmit}>
              <header className="modal-head">
                <h2>{translate('qualifiers.create.TRK')}</h2>
              </header>

              <div className="modal-body">
                <div className="modal-field">
                  <label htmlFor="create-project-name">
                    {translate('name')}
                    <em className="mandatory">*</em>
                  </label>
                  <input
                    autoFocus={true}
                    id="create-project-name"
                    maxLength="2000"
                    name="name"
                    onChange={this.handleInputChange}
                    required={true}
                    type="text"
                    value={this.state.name}
                  />
                </div>
                <div className="modal-field">
                  <label htmlFor="create-project-branch">
                    {translate('branch')}
                  </label>
                  <input
                    id="create-project-branch"
                    maxLength="200"
                    name="branch"
                    onChange={this.handleInputChange}
                    type="text"
                    value={this.state.branch}
                  />
                </div>
                <div className="modal-field">
                  <label htmlFor="create-project-key">
                    {translate('key')}
                    <em className="mandatory">*</em>
                  </label>
                  <input
                    id="create-project-key"
                    maxLength="400"
                    name="key"
                    onChange={this.handleInputChange}
                    required={true}
                    type="text"
                    value={this.state.key}
                  />
                </div>
                <div className="modal-field">
                  <label> {translate('visibility')} </label>
                  <VisibilitySelector
                    canTurnToPrivate={
                      organization == null || organization.canUpdateProjectsVisibilityToPrivate
                    }
                    className="little-spacer-top"
                    onChange={this.handleVisibilityChange}
                    visibility={this.state.visibility}
                  />
                  {organization != null &&
                    !organization.canUpdateProjectsVisibilityToPrivate &&
                    <div className="spacer-top">
                      <UpgradeOrganizationBox organization={organization.key} />
                    </div>}
                </div>
              </div>

              <footer className="modal-foot">
                {this.state.loading && <i className="spinner spacer-right" />}
                <button disabled={this.state.loading} id="create-project-submit" type="submit">
                  {translate('create')}
                </button>
                <a href="#" id="create-project-cancel" onClick={this.handleCancelClick}>
                  {translate('cancel')}
                </a>
              </footer>
            </form>}

      </Modal>
    );
  }
}
