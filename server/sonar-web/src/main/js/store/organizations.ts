/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { omit, uniq, without } from 'lodash';
import { combineReducers } from 'redux';
import { ActionType } from './utils/actions';

type ReceiveOrganizationsAction =
  | ActionType<typeof receiveOrganizations, 'RECEIVE_ORGANIZATIONS'>
  | ActionType<typeof receiveMyOrganizations, 'RECEIVE_MY_ORGANIZATIONS'>;

type Action =
  | ReceiveOrganizationsAction
  | ActionType<typeof createOrganization, 'CREATE_ORGANIZATION'>
  | ActionType<typeof updateOrganization, 'UPDATE_ORGANIZATION'>
  | ActionType<typeof deleteOrganization, 'DELETE_ORGANIZATION'>;

export interface State {
  byKey: T.Dict<T.Organization>;
  my: string[];
}

export function receiveOrganizations(organizations: T.Organization[]) {
  return { type: 'RECEIVE_ORGANIZATIONS', organizations };
}

export function receiveMyOrganizations(organizations: T.Organization[]) {
  return { type: 'RECEIVE_MY_ORGANIZATIONS', organizations };
}

export function createOrganization(organization: T.Organization) {
  return { type: 'CREATE_ORGANIZATION', organization };
}

export function updateOrganization(key: string, changes: T.OrganizationBase) {
  return { type: 'UPDATE_ORGANIZATION', key, changes };
}

export function deleteOrganization(key: string) {
  return { type: 'DELETE_ORGANIZATION', key };
}

function onReceiveOrganizations(state: State['byKey'], action: ReceiveOrganizationsAction) {
  const nextState = { ...state };
  action.organizations.forEach(organization => {
    nextState[organization.key] = { ...state[organization.key], ...organization };
  });
  return nextState;
}

function byKey(state: State['byKey'] = {}, action: Action): State['byKey'] {
  switch (action.type) {
    case 'RECEIVE_ORGANIZATIONS':
    case 'RECEIVE_MY_ORGANIZATIONS':
      return onReceiveOrganizations(state, action);
    case 'CREATE_ORGANIZATION':
      return {
        ...state,
        [action.organization.key]: {
          ...action.organization,
          actions: { ...(action.organization.actions || {}), admin: true }
        }
      };
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

function my(state: State['my'] = [], action: Action): State['my'] {
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

export default combineReducers({ byKey, my });

export function getOrganizationByKey(state: State, key: string) {
  return state.byKey[key];
}

export function getMyOrganizations(state: State) {
  return state.my.map(key => getOrganizationByKey(state, key));
}

export function areThereCustomOrganizations(state: State) {
  return Object.keys(state.byKey).length > 1;
}
