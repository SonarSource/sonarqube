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
//@flow
import React from 'react';
import Avatar from '../../../components/ui/Avatar';
import type { Option } from './UsersSelectSearch';

type Props = {
  value: Option,
  children?: Element | Text
};

const AVATAR_SIZE: number = 20;

export default class UsersSelectSearchValue extends React.PureComponent {
  props: Props;

  render() {
    const user = this.props.value;
    return (
      <div className="Select-value" title={user ? user.name : ''}>
        {user &&
          user.login &&
          <div className="Select-value-label">
            <Avatar hash={user.avatar} email={user.email} name={user.name} size={AVATAR_SIZE} />
            <strong className="spacer-left">{this.props.children}</strong>
            <span className="note little-spacer-left">{user.login}</span>
          </div>}
      </div>
    );
  }
}
