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
import { Dispatch } from 'redux';
import * as api from '../../api/organizations';
import * as actions from '../../store/organizations/duck';
import * as membersActions from '../../store/organizationsMembers/actions';
import { searchUsersGroups, addUserToGroup, removeUserFromGroup } from '../../api/user_groups';
import { receiveUser } from '../../store/users/actions';
import { onFail } from '../../store/rootActions';
import { getOrganizationMembersState } from '../../store/rootReducer';
import { addGlobalSuccessMessage } from '../../store/globalMessages/duck';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Organization, OrganizationMember, OrganizationBase } from '../../app/types';

const PAGE_SIZE = 50;

const onRejected = (dispatch: Dispatch<any>) => (error: any) => {
  onFail(dispatch)(error);
  return Promise.reject(error);
};

export const fetchOrganization = (key: string) => (dispatch: Dispatch<any>) => {
  return Promise.all([api.getOrganization(key), api.getOrganizationNavigation(key)]).then(
    ([organization, navigation]) => {
      if (organization) {
        const organizationWithPermissions = { ...organization, ...navigation };
        dispatch(actions.receiveOrganizations([organizationWithPermissions]));
      }
    },
    onFail(dispatch)
  );
};

export const fetchOrganizationGroups = (organization: string) => (dispatch: Dispatch<any>) => {
  return searchUsersGroups({ organization }).then(response => {
    dispatch(actions.receiveOrganizationGroups(organization, response.groups));
  }, onFail(dispatch));
};

export const createOrganization = (organization: OrganizationBase) => (dispatch: Dispatch<any>) => {
  return api.createOrganization(organization).then((organization: Organization) => {
    dispatch(actions.createOrganization(organization));
    dispatch(
      addGlobalSuccessMessage(translateWithParameters('organization.created', organization.name))
    );
    return organization;
  }, onRejected(dispatch));
};

export const updateOrganization = (key: string, changes: OrganizationBase) => (
  dispatch: Dispatch<any>
) => {
  return api.updateOrganization(key, changes).then(() => {
    dispatch(actions.updateOrganization(key, changes));
    dispatch(addGlobalSuccessMessage(translate('organization.updated')));
  }, onFail(dispatch));
};

export const deleteOrganization = (key: string) => (dispatch: Dispatch<any>) => {
  return api.deleteOrganization(key).then(() => {
    dispatch(actions.deleteOrganization(key));
    dispatch(addGlobalSuccessMessage(translate('organization.deleted')));
  }, onFail(dispatch));
};

const fetchMembers = (
  data: {
    organization: string;
    p?: number;
    ps?: number;
    q?: string;
  },
  dispatch: Dispatch<any>,
  receiveAction: Function
) => {
  dispatch(membersActions.updateState(data.organization, { loading: true }));
  if (data.ps === undefined) {
    data.ps = PAGE_SIZE;
  }
  if (!data.q) {
    data.q = undefined;
  }
  return api.searchMembers(data).then(
    response => {
      dispatch(
        receiveAction(data.organization, response.users, {
          loading: false,
          total: response.paging.total,
          pageIndex: response.paging.pageIndex,
          query: data.q || null
        })
      );
    },
    (error: any) => {
      onFail(dispatch)(error);
      dispatch(membersActions.updateState(data.organization, { loading: false }));
    }
  );
};

export const fetchOrganizationMembers = (key: string, query?: string) => (
  dispatch: Dispatch<any>
) => fetchMembers({ organization: key, q: query }, dispatch, membersActions.receiveMembers);

export const fetchMoreOrganizationMembers = (key: string, query?: string) => (
  dispatch: Dispatch<any>,
  getState: () => any
) =>
  fetchMembers(
    { organization: key, p: getOrganizationMembersState(getState(), key).pageIndex + 1, q: query },
    dispatch,
    membersActions.receiveMoreMembers
  );

export const addOrganizationMember = (key: string, member: OrganizationMember) => (
  dispatch: Dispatch<any>
) => {
  return api
    .addMember({ login: member.login, organization: key })
    .then(user => dispatch(membersActions.addMember(key, user)), onFail(dispatch));
};

export const removeOrganizationMember = (key: string, member: OrganizationMember) => (
  dispatch: Dispatch<any>
) => {
  dispatch(membersActions.removeMember(key, member));
  return api.removeMember({ login: member.login, organization: key }).catch((error: any) => {
    onFail(dispatch)(error);
    dispatch(membersActions.addMember(key, member));
  });
};

export const updateOrganizationMemberGroups = (
  organization: Organization,
  member: OrganizationMember,
  add: string[],
  remove: string[]
) => (dispatch: Dispatch<any>) => {
  dispatch(
    receiveUser({
      ...member,
      groupCount: (member.groupCount || 0) + add.length - remove.length
    })
  );
  const promises = [
    ...add.map(name =>
      addUserToGroup({ name, login: member.login, organization: organization.key })
    ),
    ...remove.map(name =>
      removeUserFromGroup({ name, login: member.login, organization: organization.key })
    )
  ];
  return Promise.all(promises).catch(error => {
    dispatch(receiveUser(member));
    onFail(dispatch)(error);
  });
};
