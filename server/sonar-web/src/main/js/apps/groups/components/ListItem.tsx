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

import {
  ActionsDropdown,
  Badge,
  ContentCell,
  DestructiveIcon,
  ItemButton,
  ItemDangerButton,
  ItemDivider,
  NumericalCell,
  PopupZLevel,
  Spinner,
  TableRow,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import { Image } from '~sonar-aligned/components/common/Image';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useGroupMembersCountQuery } from '../../../queries/group-memberships';
import { Group, Provider } from '../../../types/types';
import DeleteGroupForm from './DeleteGroupForm';
import GroupForm from './GroupForm';
import Members from './Members';

export interface ListItemProps {
  organization: string;
  group: Group;
  manageProvider: Provider | undefined;
}

export default function ListItem(props: Readonly<ListItemProps>) {
  const { organization, manageProvider, group } = props;
  const { name, managed, description } = group;

  const [groupToDelete, setGroupToDelete] = useState<Group | undefined>();
  const [groupToEdit, setGroupToEdit] = useState<Group | undefined>();

  const { data: membersCount, isLoading, refetch } = useGroupMembersCountQuery(organization, group.id);

  const isManaged = () => {
    return manageProvider !== undefined;
  };

  const isGroupLocal = () => {
    return isManaged() && !managed;
  };

  const renderIdentityProviderIcon = (identityProvider: Provider | undefined) => {
    if (identityProvider === undefined || identityProvider === Provider.Scim) {
      return null;
    }

    return (
      <Image
        alt={identityProvider}
        className="sw-ml-2 sw-mr-2"
        height={16}
        src={`/images/alm/${identityProvider}.svg`}
      />
    );
  };

  return (
    <TableRow data-id={name}>
      <ContentCell>
        <div className="sw-typo-semibold">{name}</div>
        {group.default && <span className="sw-ml-1">({translate('default')})</span>}
        {managed && renderIdentityProviderIcon(manageProvider)}
        {isGroupLocal() && <Badge className="sw-ml-1">{translate('local')}</Badge>}
      </ContentCell>

      <NumericalCell>
        <Spinner loading={isLoading}>{membersCount}</Spinner>
        <Members organization={organization} group={group} onEdit={refetch} isManaged={isManaged()} />
      </NumericalCell>

      <ContentCell>{description}</ContentCell>

      <NumericalCell>
        {!group.default && (!isManaged() || isGroupLocal()) && (
          <>
            {isManaged() && isGroupLocal() && (
              <DestructiveIcon
                Icon={TrashIcon}
                className="sw-ml-2"
                aria-label={translateWithParameters('delete_x', name)}
                onClick={() => setGroupToDelete(group)}
                size="small"
              />
            )}
            {!isManaged() && (
              <ActionsDropdown
                allowResizing
                id={`group-actions-${group.name}`}
                ariaLabel={translateWithParameters('groups.edit', group.name)}
                zLevel={PopupZLevel.Global}
              >
                <ItemButton onClick={() => setGroupToEdit(group)}>
                  {translate('update_details')}
                </ItemButton>
                <ItemDivider />
                <ItemDangerButton
                  className="it__quality-profiles__delete"
                  onClick={() => setGroupToDelete(group)}
                >
                  {translate('delete')}
                </ItemDangerButton>
              </ActionsDropdown>
            )}
          </>
        )}
        {groupToDelete && (
          <DeleteGroupForm group={groupToDelete} onClose={() => setGroupToDelete(undefined)} />
        )}
        {groupToEdit && (
          <GroupForm create={false} group={groupToEdit} onClose={() => setGroupToEdit(undefined)} />
        )}
      </NumericalCell>
    </TableRow>
  );
}
