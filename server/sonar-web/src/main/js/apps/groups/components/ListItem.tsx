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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { omitNil } from 'sonar-ui-common/helpers/request';
import DeleteForm from './DeleteForm';
import EditMembers from './EditMembers';
import Form from './Form';

interface Props {
  group: T.Group;
  onDelete: (name: string) => Promise<void>;
  onEdit: (data: { description?: string; id: number; name?: string }) => Promise<void>;
  onEditMembers: () => void;
  organization: string | undefined;
}

interface State {
  deleteForm: boolean;
  editForm: boolean;
}

export default class ListItem extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { deleteForm: false, editForm: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDeleteClick = () => {
    this.setState({ deleteForm: true });
  };

  handleEditClick = () => {
    this.setState({ editForm: true });
  };

  closeDeleteForm = () => {
    if (this.mounted) {
      this.setState({ deleteForm: false });
    }
  };

  closeEditForm = () => {
    if (this.mounted) {
      this.setState({ editForm: false });
    }
  };

  handleDeleteFormSubmit = () => {
    return this.props.onDelete(this.props.group.name);
  };

  handleEditFormSubmit = ({ name, description }: { name: string; description: string }) => {
    const { group } = this.props;
    return this.props.onEdit({
      description,
      id: group.id,
      // pass `name` only if it has changed, otherwise the WS fails
      ...omitNil({ name: name !== group.name ? name : undefined })
    });
  };

  render() {
    const { group } = this.props;

    return (
      <tr data-id={group.id}>
        <td className=" width-20">
          <strong className="js-group-name">{group.name}</strong>
          {group.default && <span className="little-spacer-left">({translate('default')})</span>}
        </td>

        <td className="width-10">
          <div className="display-flex-center">
            <span className="spacer-right">{group.membersCount}</span>
            {!group.default && (
              <EditMembers
                group={group}
                onEdit={this.props.onEditMembers}
                organization={this.props.organization}
              />
            )}
          </div>
        </td>

        <td className="width-40">
          <span className="js-group-description">{group.description}</span>
        </td>

        <td className="thin nowrap text-right">
          {!group.default && (
            <ActionsDropdown>
              <ActionsDropdownItem className="js-group-update" onClick={this.handleEditClick}>
                {translate('update_details')}
              </ActionsDropdownItem>
              <ActionsDropdownDivider />
              <ActionsDropdownItem
                className="js-group-delete"
                destructive={true}
                onClick={this.handleDeleteClick}>
                {translate('delete')}
              </ActionsDropdownItem>
            </ActionsDropdown>
          )}
        </td>

        {this.state.deleteForm && (
          <DeleteForm
            group={group}
            onClose={this.closeDeleteForm}
            onSubmit={this.handleDeleteFormSubmit}
          />
        )}

        {this.state.editForm && (
          <Form
            confirmButtonText={translate('update_verb')}
            group={group}
            header={translate('groups.update_group')}
            onClose={this.closeEditForm}
            onSubmit={this.handleEditFormSubmit}
          />
        )}
      </tr>
    );
  }
}
