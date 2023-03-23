/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem,
} from '../../../components/controls/ActionsDropdown';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import EditMembers from './EditMembers';

export interface ListItemProps {
  group: Group;
  onDelete: (group: Group) => void;
  onEdit: (group: Group) => void;
  onEditMembers: () => void;
  manageProvider: string | undefined;
}

export default function ListItem(props: ListItemProps) {
  const { manageProvider, group } = props;
  const { name, managed, membersCount, description } = group;

  const isManaged = () => {
    return manageProvider !== undefined;
  };

  const isGroupLocal = () => {
    return isManaged() && !managed;
  };

  return (
    <tr data-id={name}>
      <td className="width-20" headers="list-group-name">
        <strong>{name}</strong>
        {group.default && <span className="little-spacer-left">({translate('default')})</span>}
        {isGroupLocal() && <span className="little-spacer-left badge">{translate('local')}</span>}
      </td>

      <td className="group-members display-flex-justify-end" headers="list-group-member">
        <span
          className={classNames({ 'big-padded-right spacer-right': group.default && !isManaged() })}
        >
          {membersCount}
        </span>
        {!group.default && !isManaged() && (
          <EditMembers group={group} onEdit={props.onEditMembers} />
        )}
      </td>

      <td className="width-40" headers="list-group-description">
        <span className="js-group-description">{description}</span>
      </td>

      <td className="thin nowrap text-right" headers="list-group-actions">
        {!group.default && (!isManaged() || isGroupLocal()) && (
          <ActionsDropdown label={translateWithParameters('groups.edit', group.name)}>
            {!isManaged() && (
              <>
                <ActionsDropdownItem
                  className="js-group-update"
                  onClick={() => props.onEdit(group)}
                >
                  {translate('update_details')}
                </ActionsDropdownItem>
                <ActionsDropdownDivider />
              </>
            )}
            {(!isManaged() || isGroupLocal()) && (
              <ActionsDropdownItem
                className="js-group-delete"
                destructive={true}
                onClick={() => props.onDelete(group)}
              >
                {translate('delete')}
              </ActionsDropdownItem>
            )}
          </ActionsDropdown>
        )}
      </td>
    </tr>
  );
}
