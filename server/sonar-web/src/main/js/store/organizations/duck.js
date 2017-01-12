/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { combineReducers } from 'redux';
import keyBy from 'lodash/keyBy';

export type Organization = {
  key: string,
  name: string
};

type ReceiveOrganizationsAction = {
  type: 'RECEIVE_ORGANIZATIONS',
  organizations: Array<Organization>
};

type Action = ReceiveOrganizationsAction;

type ByKeyState = {
  [key: string]: Organization
};

type State = {
  byKey: ByKeyState
};

export const receiveOrganizations = (organizations: Array<Organization>): ReceiveOrganizationsAction => ({
  type: 'RECEIVE_ORGANIZATIONS',
  organizations
});

const byKey = (state: ByKeyState = {}, action: Action) => {
  switch (action.type) {
    case 'RECEIVE_ORGANIZATIONS':
      return { ...state, ...keyBy(action.organizations, 'key') };
    default:
      return state;
  }
};

export default combineReducers({ byKey });

export const getOrganizationByKey = (state: State, key: string): Organization => (
    state.byKey[key]
);

export const areThereCustomOrganizations = (state: State): boolean => (
    Object.keys(state.byKey).length > 1
);
