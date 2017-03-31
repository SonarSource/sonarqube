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
// @flow
import * as api from '../../api/organizations';
import * as actions from '../../store/organizations/duck';
import * as membersActions from '../../store/organizationsMembers/actions';
import { searchUsersGroups, addUserToGroup, removeUserFromGroup } from '../../api/user_groups';
import { receiveUser } from '../../store/users/actions';
import { onFail } from '../../store/rootActions';
import { getOrganizationMembersState } from '../../store/rootReducer';
import { addGlobalSuccessMessage } from '../../store/globalMessages/duck';
import { translate, translateWithParameters } from '../../helpers/l10n';
import type { Organization } from '../../store/organizations/duck';
import type { Member } from '../../store/organizationsMembers/actions';

const PAGE_SIZE = 50;

const onRejected = (dispatch: Function) =>
  (error: Object) => {
    onFail(dispatch)(error);
    return Promise.reject();
  };

const onMembersFail = (organization: string, dispatch: Function) =>
  (error: Object) => {
    onFail(dispatch)(error);
    dispatch(membersActions.updateState(organization, { loading: false }));
  };

export const fetchOrganization = (key: string): Function =>
  (dispatch: Function): Promise<*> => {
    const onFulfilled = ([organization, navigation]) => {
      if (organization) {
        const organizationWithPermissions = { ...organization, ...navigation };
        dispatch(actions.receiveOrganizations([organizationWithPermissions]));
      }
    };

    return Promise.all([api.getOrganization(key), api.getOrganizationNavigation(key)]).then(
      onFulfilled,
      onFail(dispatch)
    );
  };

export const fetchOrganizationGroups = (key: string): Function =>
  (dispatch: Function): Promise<*> => {
    return searchUsersGroups('', key).then(
      response => {
        dispatch(actions.receiveOrganizationGroups(key, response.groups));
      },
      onFail(dispatch)
    );
  };

export const createOrganization = (fields: {}): Function =>
  (dispatch: Function): Promise<*> => {
    const onFulfilled = (organization: Organization) => {
      dispatch(actions.createOrganization(organization));
      dispatch(
        addGlobalSuccessMessage(translateWithParameters('organization.created', organization.name))
      );
    };

    return api.createOrganization(fields).then(onFulfilled, onRejected(dispatch));
  };

export const updateOrganization = (key: string, changes: {}): Function =>
  (dispatch: Function): Promise<*> => {
    const onFulfilled = () => {
      dispatch(actions.updateOrganization(key, changes));
      dispatch(addGlobalSuccessMessage(translate('organization.updated')));
    };

    return api.updateOrganization(key, changes).then(onFulfilled, onFail(dispatch));
  };

export const deleteOrganization = (key: string): Function =>
  (dispatch: Function): Promise<*> => {
    const onFulfilled = () => {
      dispatch(actions.deleteOrganization(key));
      dispatch(addGlobalSuccessMessage(translate('organization.deleted')));
    };

    return api.deleteOrganization(key).then(onFulfilled, onFail(dispatch));
  };

const fetchMembers = (
  dispatch: Function,
  receiveAction: Function,
  key: string,
  query?: string,
  page?: number
) => {
  dispatch(membersActions.updateState(key, { loading: true }));
  const data: Object = {
    organization: key,
    ps: PAGE_SIZE
  };
  if (page != null) {
    data.p = page;
  }
  if (query) {
    data.q = query;
  }
  return api.searchMembers(data).then(
    response => {
      dispatch(
        receiveAction(key, response.users, {
          loading: false,
          total: response.paging.total,
          pageIndex: response.paging.pageIndex,
          query: query || null
        })
      );
    },
    onMembersFail(key, dispatch)
  );
};

export const fetchOrganizationMembers = (key: string, query?: string) =>
  (dispatch: Function) => fetchMembers(dispatch, membersActions.receiveMembers, key, query);

export const fetchMoreOrganizationMembers = (key: string, query?: string) =>
  (dispatch: Function, getState: Function) =>
    fetchMembers(
      dispatch,
      membersActions.receiveMoreMembers,
      key,
      query,
      getOrganizationMembersState(getState(), key).pageIndex + 1
    );

export const addOrganizationMember = (key: string, member: Member) =>
  (dispatch: Function) => {
    dispatch(membersActions.addMember(key, member));
    return api.addMember({ login: member.login, organization: key }).catch((error: Object) => {
      onFail(dispatch)(error);
      dispatch(membersActions.removeMember(key, member));
    });
  };

export const removeOrganizationMember = (key: string, member: Member) =>
  (dispatch: Function) => {
    dispatch(membersActions.removeMember(key, member));
    return api.removeMember({ login: member.login, organization: key }).catch((error: Object) => {
      onFail(dispatch)(error);
      dispatch(membersActions.addMember(key, member));
    });
  };

export const updateOrganizationMemberGroups = (
  member: Member,
  add: Array<string>,
  remove: Array<string>
) =>
  (dispatch: Function) => {
    const promises = [
      ...add.map(id => addUserToGroup(id, member.login)),
      ...remove.map(id => removeUserFromGroup(id, member.login))
    ];
    return Promise.all(promises).then(
      () => {
        dispatch(
          receiveUser({ ...member, groupCount: member.groupCount + add.length - remove.length })
        );
      },
      onFail(dispatch)
    );
  };
