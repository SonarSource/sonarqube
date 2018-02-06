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
import EditGroup from './EditGroup';
import EditMembers from './EditMembers';
import { Group } from '../../../app/types';
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  group: Group;
  onDelete: (name: string) => Promise<void>;
  onEdit: (data: { description?: string; id: number; name?: string }) => Promise<void>;
  onEditMembers: () => void;
  organization: string | undefined;
}

export default class ListItem extends React.PureComponent<Props> {
  handleDelete = () => {
    return this.props.onDelete(this.props.group.name);
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
              <EditGroup group={group} onEdit={this.props.onEdit}>
                {({ onClick }) => (
                  <ActionsDropdownItem className="js-group-update" onClick={onClick}>
                    {translate('update_details')}
                  </ActionsDropdownItem>
                )}
              </EditGroup>
              <ActionsDropdownDivider />
              <ConfirmButton
                confirmButtonText={translate('delete')}
                isDestructive={true}
                modalBody={translateWithParameters('groups.delete_group.confirmation', group.name)}
                modalHeader={translate('groups.delete_group')}
                onConfirm={this.handleDelete}>
                {({ onClick }) => (
                  <ActionsDropdownItem
                    className="js-group-delete"
                    destructive={true}
                    onClick={onClick}>
                    {translate('delete')}
                  </ActionsDropdownItem>
                )}
              </ConfirmButton>
            </ActionsDropdown>
          )}
        </td>
      </tr>
    );
  }
}
