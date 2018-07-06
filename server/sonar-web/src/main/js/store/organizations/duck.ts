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
import { combineReducers } from 'redux';
import { omit, uniq, without } from 'lodash';
import { Group, Organization, OrganizationBase } from '../../app/types';

interface ReceiveOrganizationsAction {
  type: 'RECEIVE_ORGANIZATIONS';
  organizations: Organization[];
}

interface ReceiveMyOrganizationsAction {
  type: 'RECEIVE_MY_ORGANIZATIONS';
  organizations: Organization[];
}

interface ReceiveOrganizationGroups {
  type: 'RECEIVE_ORGANIZATION_GROUPS';
  key: string;
  groups: Group[];
}

interface CreateOrganizationAction {
  type: 'CREATE_ORGANIZATION';
  organization: Organization;
}

interface UpdateOrganizationAction {
  type: 'UPDATE_ORGANIZATION';
  key: string;
  changes: {};
}

interface DeleteOrganizationAction {
  type: 'DELETE_ORGANIZATION';
  key: string;
}

type Action =
  | ReceiveOrganizationsAction
  | ReceiveMyOrganizationsAction
  | ReceiveOrganizationGroups
  | CreateOrganizationAction
  | UpdateOrganizationAction
  | DeleteOrganizationAction;

interface ByKeyState {
  [key: string]: Organization;
}

interface GroupsState {
  [key: string]: Group[];
}

type MyState = string[];

interface State {
  byKey: ByKeyState;
  my: MyState;
  groups: GroupsState;
}

export function receiveOrganizations(organizations: Organization[]): ReceiveOrganizationsAction {
  return {
    type: 'RECEIVE_ORGANIZATIONS',
    organizations
  };
}

export function receiveMyOrganizations(
  organizations: Organization[]
): ReceiveMyOrganizationsAction {
  return {
    type: 'RECEIVE_MY_ORGANIZATIONS',
    organizations
  };
}

export function receiveOrganizationGroups(key: string, groups: Group[]): ReceiveOrganizationGroups {
  return {
    type: 'RECEIVE_ORGANIZATION_GROUPS',
    key,
    groups
  };
}

export function createOrganization(organization: Organization): CreateOrganizationAction {
  return {
    type: 'CREATE_ORGANIZATION',
    organization
  };
}

export function updateOrganization(
  key: string,
  changes: OrganizationBase
): UpdateOrganizationAction {
  return {
    type: 'UPDATE_ORGANIZATION',
    key,
    changes
  };
}

export function deleteOrganization(key: string): DeleteOrganizationAction {
  return {
    type: 'DELETE_ORGANIZATION',
    key
  };
}

function onReceiveOrganizations(
  state: ByKeyState,
  action: ReceiveOrganizationsAction | ReceiveMyOrganizationsAction
): ByKeyState {
  const nextState = { ...state };
  action.organizations.forEach(organization => {
    nextState[organization.key] = { ...state[organization.key], ...organization };
  });
  return nextState;
}

function byKey(state: ByKeyState = {}, action: Action) {
  switch (action.type) {
    case 'RECEIVE_ORGANIZATIONS':
    case 'RECEIVE_MY_ORGANIZATIONS':
      return onReceiveOrganizations(state, action);
    case 'CREATE_ORGANIZATION':
      return { ...state, [action.organization.key]: { ...action.organization, isAdmin: true } };
    case 'UPDATE_ORGANIZATION':
      return {
        ...state,
        [action.key]: {
          ...state[action.key],
          key: action.key,
          ...action.changes
        }
      };
    case 'DELETE_ORGANIZATION':
      return omit(state, action.key);
    default:
      return state;
  }
}

function my(state: MyState = [], action: Action) {
  switch (action.type) {
    case 'RECEIVE_MY_ORGANIZATIONS':
      return uniq([...state, ...action.organizations.map(o => o.key)]);
    case 'CREATE_ORGANIZATION':
      return uniq([...state, action.organization.key]);
    case 'DELETE_ORGANIZATION':
      return without(state, action.key);
    default:
      return state;
  }
}

function groups(state: GroupsState = {}, action: Action) {
  if (action.type === 'RECEIVE_ORGANIZATION_GROUPS') {
    return { ...state, [action.key]: action.groups };
  }
  return state;
}

export default combineReducers<State>({ byKey, my, groups });

export function getOrganizationByKey(state: State, key: string): Organization | undefined {
  return state.byKey[key];
}

export function getOrganizationGroupsByKey(state: State, key: string): Group[] {
  return state.groups[key] || [];
}

export function getMyOrganizations(state: State): Organization[] {
  return state.my.map(key => getOrganizationByKey(state, key) as Organization);
}

export function areThereCustomOrganizations(state: State): boolean {
  return Object.keys(state.byKey).length > 1;
}
