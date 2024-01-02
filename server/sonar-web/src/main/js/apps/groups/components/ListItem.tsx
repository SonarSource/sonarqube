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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem,
} from '../../../components/controls/ActionsDropdown';
import { translate } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import EditMembers from './EditMembers';

export interface ListItemProps {
  group: Group;
  onDelete: (group: Group) => void;
  onEdit: (group: Group) => void;
  onEditMembers: () => void;
}

export default function ListItem(props: ListItemProps) {
  const { group } = props;

  return (
    <tr data-id={group.name}>
      <td className="width-20">
        <strong className="js-group-name">{group.name}</strong>
        {group.default && <span className="little-spacer-left">({translate('default')})</span>}
      </td>

      <td className="thin text-middle text-right little-padded-right">{group.membersCount}</td>
      <td className="little-padded-left">
        {!group.default && <EditMembers group={group} onEdit={props.onEditMembers} />}
      </td>

      <td className="width-40">
        <span className="js-group-description">{group.description}</span>
      </td>

      <td className="thin nowrap text-right">
        {!group.default && (
          <ActionsDropdown>
            <ActionsDropdownItem className="js-group-update" onClick={() => props.onEdit(group)}>
              {translate('update_details')}
            </ActionsDropdownItem>
            <ActionsDropdownDivider />
            <ActionsDropdownItem
              className="js-group-delete"
              destructive={true}
              onClick={() => props.onDelete(group)}
            >
              {translate('delete')}
            </ActionsDropdownItem>
          </ActionsDropdown>
        )}
      </td>
    </tr>
  );
}
