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
import * as api from '../../../../api/permissions';
import { parseError } from '../../../../helpers/request';
import {
  raiseError,
  REQUEST_HOLDERS,
  RECEIVE_HOLDERS_SUCCESS,
  UPDATE_QUERY,
  UPDATE_FILTER,
  SELECT_PERMISSION,
  GRANT_PERMISSION_TO_USER,
  REVOKE_PERMISSION_TO_USER,
  GRANT_PERMISSION_TO_GROUP,
  REVOKE_PERMISSION_FROM_GROUP
} from '../../shared/store/actions';
import {
  getPermissionsAppQuery,
  getPermissionsAppFilter,
  getPermissionsAppSelectedPermission
} from '../../../../store/rootReducer';

/*::
type Dispatch = Object => void;
*/
/*::
type GetState = () => Object;
*/

export const loadHolders = (organization /*: ?string */) => (
  dispatch /*: Dispatch */,
  getState /*: GetState */
) => {
  const query = getPermissionsAppQuery(getState());
  const filter = getPermissionsAppFilter(getState());
  const selectedPermission = getPermissionsAppSelectedPermission(getState());

  dispatch({ type: REQUEST_HOLDERS, query });

  const requests = [];

  if (filter !== 'groups') {
    requests.push(api.getGlobalPermissionsUsers(query, selectedPermission, organization));
  } else {
    requests.push(Promise.resolve([]));
  }

  if (filter !== 'users') {
    requests.push(api.getGlobalPermissionsGroups(query, selectedPermission, organization));
  } else {
    requests.push(Promise.resolve([]));
  }

  return Promise.all(requests)
    .then(responses =>
      dispatch({
        type: RECEIVE_HOLDERS_SUCCESS,
        users: responses[0],
        groups: responses[1],
        query
      })
    )
    .catch(e => {
      return parseError(e).then(message => dispatch(raiseError(message)));
    });
};

export const updateQuery = (query /*: string */ = '', organization /*: ?string */) => (
  dispatch /*: Dispatch */
) => {
  dispatch({ type: UPDATE_QUERY, query });
  dispatch(loadHolders(organization));
};

export const updateFilter = (filter /*: string */, organization /*: ?string */) => (
  dispatch /*: Dispatch */
) => {
  dispatch({ type: UPDATE_FILTER, filter });
  dispatch(loadHolders(organization));
};

export const selectPermission = (permission /*: string */, organization /*: ?string */) => (
  dispatch /*: Dispatch */,
  getState /*: GetState */
) => {
  const selectedPermission = getPermissionsAppSelectedPermission(getState());
  if (selectedPermission !== permission) {
    dispatch({ type: SELECT_PERMISSION, permission });
  } else {
    dispatch({ type: SELECT_PERMISSION, permission: null });
  }
  dispatch(loadHolders(organization));
};

export const grantToUser = (
  login /*: string */,
  permission /*: string */,
  organization /*: ?string */
) => (dispatch /*: Dispatch */) => {
  api
    .grantPermissionToUser(null, login, permission, organization)
    .then(() => {
      dispatch({ type: GRANT_PERMISSION_TO_USER, login, permission });
    })
    .catch(e => {
      return parseError(e).then(message => dispatch(raiseError(message)));
    });
};

export const revokeFromUser = (
  login /*: string */,
  permission /*: string */,
  organization /*: ?string */
) => (dispatch /*: Dispatch */) => {
  api
    .revokePermissionFromUser(null, login, permission, organization)
    .then(() => {
      dispatch({ type: REVOKE_PERMISSION_TO_USER, login, permission });
    })
    .catch(e => {
      return parseError(e).then(message => dispatch(raiseError(message)));
    });
};

export const grantToGroup = (
  groupName /*: string */,
  permission /*: string */,
  organization /*: ?string */
) => (dispatch /*: Dispatch */) => {
  api
    .grantPermissionToGroup(null, groupName, permission, organization)
    .then(() => {
      dispatch({
        type: GRANT_PERMISSION_TO_GROUP,
        groupName,
        permission
      });
    })
    .catch(e => {
      return parseError(e).then(message => dispatch(raiseError(message)));
    });
};

export const revokeFromGroup = (
  groupName /*: string */,
  permission /*: string */,
  organization /*: ?string */
) => (dispatch /*: Dispatch */) => {
  api
    .revokePermissionFromGroup(null, groupName, permission, organization)
    .then(() => {
      dispatch({
        type: REVOKE_PERMISSION_FROM_GROUP,
        groupName,
        permission
      });
    })
    .catch(e => {
      return parseError(e).then(message => dispatch(raiseError(message)));
    });
};
