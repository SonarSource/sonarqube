/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { DeleteButton } from '../../../components/controls/buttons';
import Avatar from '../../../components/ui/Avatar';

interface Props {
  onClickDelete: (user: T.UserBase) => void;
  user: T.UserBase;
}

export default function PermissionItem(props: Props) {
  const { user } = props;

  return (
    <div className="display-flex-center permission-list-item padded">
      <Avatar className="spacer-right" hash={user.avatar} name={user.name} size={32} />

      <div className="overflow-hidden flex-1">
        <strong>{user.name}</strong>
        <div className="note">{user.login}</div>
      </div>

      <DeleteButton onClick={() => props.onClickDelete(user)} />
    </div>
  );
}
