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
import classNames from 'classnames';
import { Checkbox } from '~design-system';
import * as React from 'react';
import { Group } from '../../types/types';

interface Props {
  group: Group;
  checked: boolean;
  onCheck: (name: string, checked: boolean) => void;
}

export default class OrganizationGroupCheckbox extends React.PureComponent<Props> {
  onCheck = (checked: boolean) => {
    const { group } = this.props;
    if (!group.default && group.name) {
      this.props.onCheck(group.name, checked);
    }
  };

  render() {
    const { group } = this.props;
    return (
      <li
        className={classNames('capitalize list-item-checkable-link', { disabled: group.default })}
      >
        <Checkbox checked={this.props.checked} disabled={group.default}
                  onClick={(e: React.MouseEvent<HTMLLabelElement>) => e.stopPropagation()}
                  onCheck={(checked, _) => this.onCheck(checked)} />{' '}
        {group.name}
      </li>
    );
  }
}
