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

import * as React from 'react';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { deleteApplication, editApplication, refreshApplication } from '../../api/application';
import addGlobalSuccessMessage from '../../app/utils/addGlobalSuccessMessage';
import { Application, ApplicationProject } from '../../types/application';
import { Branch } from '../../types/branch-like';
import ApplicationBranches from './ApplicationBranches';
import ApplicationDetailsProjects from './ApplicationDetailsProjects';
import EditForm from './EditForm';

interface Props {
  application: Application;
  canRecompute: boolean | undefined;
  onAddProject: (project: ApplicationProject) => void;
  onDelete: (key: string) => void;
  onEdit: (key: string, name: string, description: string) => void;
  onRemoveProject: (projectKey: string) => void;
  onUpdateBranches: (branches: Branch[]) => void;
  pathname: string;
  single: boolean | undefined;
}

interface State {
  editing: boolean;
  loading: boolean;
}

export default class ApplicationDetails extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    editing: false,
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componenWillUnmount() {
    this.mounted = false;
  }

  handleRefreshClick = () => {
    this.setState({ loading: true });
    refreshApplication(this.props.application.key).then(() => {
      addGlobalSuccessMessage(translate('application_console.refresh_started'));
      this.stopLoading();
    }, this.stopLoading);
  };

  handleDelete = async () => {
    await deleteApplication(this.props.application.key);
    this.props.onDelete(this.props.application.key);
  };

  handleEditClick = () => {
    this.setState({ editing: true });
  };

  handleEditFormClose = () => {
    this.setState({ editing: false });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { loading } = this.state;
    const { application } = this.props;
    const canDelete = !this.props.single;
    return (
      <div className="boxed-group portfolios-console-details" id="view-details">
        <div className="boxed-group-actions">
          <Button
            className="little-spacer-right"
            id="view-details-edit"
            onClick={this.handleEditClick}>
            {translate('edit')}
          </Button>
          {this.props.canRecompute && (
            <Button
              className="little-spacer-right"
              disabled={loading}
              onClick={this.handleRefreshClick}>
              {loading && <i className="little-spacer-right spinner" />}
              {translate('application_console.recompute')}
            </Button>
          )}
          {canDelete && (
            <ConfirmButton
              confirmButtonText={translate('delete')}
              isDestructive={true}
              modalBody={translateWithParameters(
                'application_console.do_you_want_to_delete',
                application.name
              )}
              modalHeader={translate('application_console.delete_application')}
              onConfirm={this.handleDelete}>
              {({ onClick }) => (
                <Button className="button-red" id="view-details-delete" onClick={onClick}>
                  {translate('delete')}
                </Button>
              )}
            </ConfirmButton>
          )}
        </div>

        <header className="boxed-group-header" id="view-details-header">
          <h2 className="text-limited" title={application.name}>
            {application.name}
          </h2>
        </header>

        <div className="boxed-group-inner" id="view-details-content">
          <div className="big-spacer-bottom">
            {application.description && (
              <div className="little-spacer-bottom">{application.description}</div>
            )}
            <div className="subtitle">
              {translate('key')}: {application.key}
              <Link
                className="spacer-left"
                to={{ pathname: '/dashboard', query: { id: application.key } }}>
                {translate('application_console.open_dashbard')}
              </Link>
            </div>
          </div>

          <ApplicationDetailsProjects
            onAddProject={this.props.onAddProject}
            onRemoveProject={this.props.onRemoveProject}
            application={this.props.application}
          />

          <ApplicationBranches
            application={this.props.application}
            onUpdateBranches={this.props.onUpdateBranches}
          />
        </div>

        {this.state.editing && (
          <EditForm
            header={translate('portfolios.edit_application')}
            onChange={editApplication}
            onClose={this.handleEditFormClose}
            onEdit={this.props.onEdit}
            application={application}
          />
        )}
      </div>
    );
  }
}
