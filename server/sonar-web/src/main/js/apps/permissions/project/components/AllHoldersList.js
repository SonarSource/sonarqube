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
// @flow
import React from 'react';
import { without } from 'lodash';
import SearchForm from '../../shared/components/SearchForm';
import HoldersList from '../../shared/components/HoldersList';
import { translate } from '../../../../helpers/l10n';
import { PERMISSIONS_ORDER_BY_QUALIFIER } from '../constants';

/*::
type Props = {|
  component: {
    configuration?: {
      canApplyPermissionTemplate: boolean,
      canUpdateProjectVisibilityToPrivate: boolean
    },
    key: string,
    organization: string,
    qualifier: string,
    visibility: string
  },
  filter: string,
  grantPermissionToGroup: (group: string, permission: string) => void,
  grantPermissionToUser: (user: string, permission: string) => void,
  groups: Array<{
    name: string,
    permissions: Array<string>
  }>,
  onFilterChange: string => void,
  onPermissionSelect: (string | void) => void,
  onQueryChange: string => void,
  query: string,
  revokePermissionFromGroup: (group: string, permission: string) => void,
  revokePermissionFromUser: (user: string, permission: string) => void,
  selectedPermission: ?string,
  visibility: string,
  users: Array<{
    login: string,
    name: string,
    permissions: Array<string>
  }>
|};
*/

export default class AllHoldersList extends React.PureComponent {
  /*:: props: Props; */

  handleToggleUser = (user /*: Object */, permission /*: string */) => {
    const hasPermission = user.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromUser(user.login, permission);
    } else {
      this.props.grantPermissionToUser(user.login, permission);
    }
  };

  handleToggleGroup = (group /*: Object */, permission /*: string */) => {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromGroup(group.name, permission);
    } else {
      this.props.grantPermissionToGroup(group.name, permission);
    }
  };

  handleSelectPermission = (permission /*: string | void */) => {
    this.props.onPermissionSelect(permission);
  };

  render() {
    let order = PERMISSIONS_ORDER_BY_QUALIFIER[this.props.component.qualifier];
    if (this.props.visibility === 'public') {
      order = without(order, 'user', 'codeviewer');
    }

    const permissions = order.map(p => ({
      key: p,
      name: translate('projects_role', p),
      description: translate('projects_role', p, 'desc')
    }));

    return (
      <HoldersList
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        users={this.props.users}
        groups={this.props.groups}
        onSelectPermission={this.handleSelectPermission}
        onToggleUser={this.handleToggleUser}
        onToggleGroup={this.handleToggleGroup}>
        <SearchForm
          query={this.props.query}
          filter={this.props.filter}
          onSearch={this.props.onQueryChange}
          onFilter={this.props.onFilterChange}
        />
      </HoldersList>
    );
  }
}
