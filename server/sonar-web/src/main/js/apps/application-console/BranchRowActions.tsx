/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { without } from 'lodash';
import * as React from 'react';
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { deleteApplicationBranch } from '../../api/application';
import { Application } from '../../types/application';
import CreateBranchForm from './CreateBranchForm';
import { ApplicationBranch } from './utils';

interface Props {
  application: Application;
  branch: ApplicationBranch;
  onUpdateBranches: (branches: Array<ApplicationBranch>) => void;
}

interface State {
  isUpdating: boolean;
}

export default class BranchRowActions extends React.PureComponent<Props, State> {
  state: State = { isUpdating: false };

  handleDelete = () => {
    const { application, branch } = this.props;
    return deleteApplicationBranch(application.key, branch.name).then(() => {
      this.props.onUpdateBranches(without(application.branches, branch));
    });
  };

  handleUpdate = (newBranchName: string) => {
    this.props.onUpdateBranches(
      this.props.application.branches.map(branch => {
        if (branch.name === this.props.branch.name) {
          branch.name = newBranchName;
        }
        return branch;
      })
    );
  };

  handleCloseForm = () => {
    this.setState({ isUpdating: false });
  };

  handleUpdateClick = () => {
    this.setState({ isUpdating: true });
  };

  render() {
    return (
      <>
        <ConfirmButton
          confirmButtonText={translate('delete')}
          isDestructive={true}
          modalBody={translateWithParameters(
            'application_console.branches.delete.warning_x',
            this.props.branch.name
          )}
          modalHeader={translate('application_console.branches.delete')}
          onConfirm={this.handleDelete}>
          {({ onClick }) => (
            <ActionsDropdown>
              <ActionsDropdownItem onClick={this.handleUpdateClick}>
                {translate('edit')}
              </ActionsDropdownItem>
              <ActionsDropdownItem destructive={true} onClick={onClick}>
                {translate('delete')}
              </ActionsDropdownItem>
            </ActionsDropdown>
          )}
        </ConfirmButton>

        {this.state.isUpdating && (
          <CreateBranchForm
            application={this.props.application}
            branch={this.props.branch}
            enabledProjectsKey={this.props.application.projects
              .filter(p => p.enabled)
              .map(p => p.key)}
            onClose={this.handleCloseForm}
            onUpdate={this.handleUpdate}
          />
        )}
      </>
    );
  }
}
