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
  ContentCell,
  DestructiveIcon,
  GenericAvatar,
  Note,
  TrashIcon,
  UserGroupIcon,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import Avatar from '../../../components/ui/Avatar';
import { Group, isUser } from '../../../types/quality-gates';
import { UserBase } from '../../../types/users';

export interface PermissionItemProps {
  onClickDelete: (item: UserBase | Group) => void;
  item: UserBase | Group;
}

export default function PermissionItem(props: PermissionItemProps) {
  const { item } = props;
  const { formatMessage } = useIntl();

  return (
    <>
      <ContentCell width={0}>
        {isUser(item) ? (
          <Avatar hash={item.avatar} name={item.name} size="md" />
        ) : (
          <GenericAvatar Icon={UserGroupIcon} name={item.name} size="md" />
        )}
      </ContentCell>

      <ContentCell>
        <div className="sw-flex sw-flex-col">
          <strong className="sw-body-sm-highlight">{item.name}</strong>
          {isUser(item) && <Note>{item.login}</Note>}
        </div>
      </ContentCell>

      <ContentCell>
        <DestructiveIcon
          aria-label={formatMessage({
            id: isUser(item)
              ? 'quality_gates.permissions.remove.user'
              : 'quality_gates.permissions.remove.group',
          })}
          Icon={TrashIcon}
          onClick={() => props.onClickDelete(item)}
          data-testid="permission-delete-button"
        />
      </ContentCell>
    </>
  );
}
