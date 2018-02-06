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
import { connect } from 'react-redux';
import AllHoldersList from './AllHoldersList';
import {
  loadHolders,
  grantToUser,
  revokeFromUser,
  grantToGroup,
  revokeFromGroup,
  updateFilter,
  updateQuery,
  selectPermission
} from '../store/actions';
import {
  getPermissionsAppUsers,
  getPermissionsAppGroups,
  getPermissionsAppQuery,
  getPermissionsAppFilter,
  getPermissionsAppSelectedPermission
} from '../../../../store/rootReducer';
import { Organization } from '../../../../app/types';
import { PermissionUser, PermissionGroup } from '../../../../api/permissions';

interface OwnProps {
  organization?: Organization;
}

interface StateToProps {
  filter: string;
  groups: PermissionGroup[];
  query: string;
  selectedPermission?: string;
  users: PermissionUser[];
}

interface DispatchToProps {
  grantPermissionToGroup: (groupName: string, permission: string) => void;
  grantPermissionToUser: (login: string, permission: string) => void;
  loadHolders: () => void;
  onFilter: (filter: string) => void;
  onSearch: (query: string) => void;
  onSelectPermission: (permission: string) => void;
  revokePermissionFromGroup: (groupName: string, permission: string) => void;
  revokePermissionFromUser: (login: string, permission: string) => void;
}

const mapStateToProps = (state: any) => ({
  filter: getPermissionsAppFilter(state),
  groups: getPermissionsAppGroups(state),
  query: getPermissionsAppQuery(state),
  selectedPermission: getPermissionsAppSelectedPermission(state),
  users: getPermissionsAppUsers(state)
});

const mapDispatchToProps = (dispatch: Function, ownProps: OwnProps) => {
  const organizationKey = ownProps.organization ? ownProps.organization.key : undefined;
  return {
    grantPermissionToGroup: (groupName: string, permission: string) =>
      dispatch(grantToGroup(groupName, permission, organizationKey)),
    grantPermissionToUser: (login: string, permission: string) =>
      dispatch(grantToUser(login, permission, organizationKey)),
    loadHolders: () => dispatch(loadHolders(organizationKey)),
    onFilter: (filter: string) => dispatch(updateFilter(filter, organizationKey)),
    onSearch: (query: string) => dispatch(updateQuery(query, organizationKey)),
    onSelectPermission: (permission: string) =>
      dispatch(selectPermission(permission, organizationKey)),
    revokePermissionFromGroup: (groupName: string, permission: string) =>
      dispatch(revokeFromGroup(groupName, permission, organizationKey)),
    revokePermissionFromUser: (login: string, permission: string) =>
      dispatch(revokeFromUser(login, permission, organizationKey))
  };
};

export default connect<StateToProps, DispatchToProps, OwnProps>(
  mapStateToProps,
  mapDispatchToProps
)(AllHoldersList);
