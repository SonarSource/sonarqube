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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import Avatar from '../../../../components/ui/Avatar';
import { translate } from '../../../../helpers/l10n';

export default class UserHolder extends React.Component {
  static propTypes = {
    user: React.PropTypes.object.isRequired,
    permissions: React.PropTypes.array.isRequired,
    selectedPermission: React.PropTypes.string,
    permissionsOrder: React.PropTypes.array.isRequired,
    onToggle: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate(nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleClick(permission, e) {
    e.preventDefault();
    e.target.blur();
    this.props.onToggle(this.props.user, permission);
  }

  render() {
    const { selectedPermission } = this.props;
    const permissionCells = this.props.permissionsOrder.map(p => (
      <td
        key={p.key}
        className="text-center text-middle"
        style={{ backgroundColor: p.key === selectedPermission ? '#d9edf7' : 'transparent' }}>
        <button className="button-clean" onClick={this.handleClick.bind(this, p.key)}>
          {this.props.permissions.includes(p.key)
            ? <i className="icon-checkbox icon-checkbox-checked" />
            : <i className="icon-checkbox" />}
        </button>
      </td>
    ));

    const { user } = this.props;

    const isCreator = user.login === '<creator>';

    return (
      <tr>
        <td className="nowrap">
          {!isCreator &&
            <Avatar email={user.email} size={36} className="text-middle big-spacer-right" />}
          <div className="display-inline-block text-middle">
            <div>
              <strong>{user.name}</strong>
              {!isCreator && <span className="note spacer-left">{user.login}</span>}
            </div>
            {!isCreator && <div className="little-spacer-top">{user.email}</div>}
            {isCreator &&
              <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
                {translate('permission_templates.project_creators.explanation')}
              </div>}
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }
}
