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
//@flow
import React from 'react';
import Checkbox from '../../../components/controls/Checkbox';
/*:: import type { OrgGroup } from '../../../store/organizations/duck'; */

/*::
type Props = {
  group: OrgGroup,
  checked: boolean,
  onCheck: (string, boolean) => void
};
*/

export default class OrganizationGroupCheckbox extends React.PureComponent {
  /*:: props: Props; */

  onCheck = (checked /*: boolean */) => {
    const { group } = this.props;
    if (!group.default) {
      this.props.onCheck(group.name, checked);
    }
  };

  toggleCheck = () => {
    this.onCheck(!this.props.checked);
  };

  render() {
    const { group } = this.props;
    return (
      <li
        className="capitalize list-item-checkable-link"
        onClick={this.toggleCheck}
        tabIndex={0}
        role="listitem"
        disabled={group.default}>
        <Checkbox checked={this.props.checked} onCheck={this.onCheck} /> {group.name}
      </li>
    );
  }
}
