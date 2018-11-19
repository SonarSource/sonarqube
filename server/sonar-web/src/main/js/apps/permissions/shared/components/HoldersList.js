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
import React from 'react';
import PropTypes from 'prop-types';
import UserHolder from './UserHolder';
import GroupHolder from './GroupHolder';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export default class HoldersList extends React.PureComponent {
  static propTypes = {
    permissions: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    groups: PropTypes.array.isRequired,
    selectedPermission: PropTypes.string,
    showPublicProjectsWarning: PropTypes.bool,
    onSelectPermission: PropTypes.func.isRequired,
    onToggleUser: PropTypes.func.isRequired,
    onToggleGroup: PropTypes.func.isRequired
  };

  static defaultProps = {
    showPublicProjectsWarning: false
  };

  handlePermissionClick = event => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onSelectPermission(event.currentTarget.dataset.key);
  };

  renderTooltip = permission =>
    this.props.showPublicProjectsWarning &&
    (permission.key === 'user' || permission.key === 'codeviewer') ? (
      <div>
        {permission.description}
        <div className="alert alert-warning spacer-top">
          {translate('projects_role.public_projects_warning')}
        </div>
      </div>
    ) : (
      permission.description
    );

  renderTableHeader() {
    const { selectedPermission } = this.props;
    const cells = this.props.permissions.map(p => (
      <th
        key={p.key}
        className="permission-column text-center"
        style={{
          backgroundColor: p.key === selectedPermission ? '#d9edf7' : 'transparent'
        }}>
        <div className="permission-column-inner">
          <Tooltip
            overlay={translateWithParameters('global_permissions.filter_by_x_permission', p.name)}>
            <a data-key={p.key} href="#" onClick={this.handlePermissionClick}>
              {p.name}
            </a>
          </Tooltip>
          <Tooltip overlay={this.renderTooltip(p)}>
            <i className="icon-help little-spacer-left" />
          </Tooltip>
        </div>
      </th>
    ));
    return (
      <thead>
        <tr>
          <td className="nowrap bordered-bottom">{this.props.children}</td>
          {cells}
        </tr>
      </thead>
    );
  }

  renderEmpty() {
    const columns = this.props.permissions.length + 1;
    return (
      <tr>
        <td colSpan={columns}>{translate('no_results_search')}</td>
      </tr>
    );
  }

  render() {
    const users = this.props.users.map(user => (
      <UserHolder
        key={'user-' + user.login}
        user={user}
        permissions={user.permissions}
        selectedPermission={this.props.selectedPermission}
        permissionsOrder={this.props.permissions}
        onToggle={this.props.onToggleUser}
      />
    ));

    const groups = this.props.groups.map(group => (
      <GroupHolder
        key={'group-' + group.id}
        group={group}
        permissions={group.permissions}
        selectedPermission={this.props.selectedPermission}
        permissionsOrder={this.props.permissions}
        onToggle={this.props.onToggleGroup}
      />
    ));

    return (
      <table className="data zebra permissions-table">
        {this.renderTableHeader()}
        <tbody>
          {users.length === 0 && groups.length === 0 && this.renderEmpty()}
          {users}
          {groups}
        </tbody>
      </table>
    );
  }
}
