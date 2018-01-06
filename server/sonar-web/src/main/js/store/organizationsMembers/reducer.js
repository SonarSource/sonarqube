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
import { uniq } from 'lodash';
import { actions } from './actions';

export const getOrganizationMembersLogins = (state, organization) => {
  if (organization && state[organization]) {
    return state[organization].members || [];
  }
  return [];
};

export const getOrganizationMembersState = (state, organization) =>
  organization && state[organization] ? state[organization] : {};

const organizationMembers = (state = {}, action = {}) => {
  const members = state.members || [];
  switch (action.type) {
    case actions.UPDATE_STATE:
      return { ...state, ...action.stateChanges };
    case actions.RECEIVE_MEMBERS:
      return {
        ...state,
        ...action.stateChanges,
        members: action.members.map(member => member.login)
      };
    case actions.RECEIVE_MORE_MEMBERS:
      return {
        ...state,
        ...action.stateChanges,
        members: uniq(members.concat(action.members.map(member => member.login)))
      };
    case actions.ADD_MEMBER: {
      const withNew = [...members, action.member.login].sort();
      return {
        ...state,
        total: state.total + 1,
        members: withNew
      };
    }
    case actions.REMOVE_MEMBER: {
      const withoutDeleted = state.members.filter(login => login !== action.member.login);
      if (withoutDeleted.length === state.members.length) {
        return state;
      }
      return {
        ...state,
        total: state.total - 1,
        members: withoutDeleted
      };
    }
    default:
      return state;
  }
};

const organizationsMembers = (state = {}, action = {}) => {
  const organization = state[action.organization] || {};
  switch (action.type) {
    case actions.UPDATE_STATE:
    case actions.RECEIVE_MEMBERS:
    case actions.RECEIVE_MORE_MEMBERS:
    case actions.ADD_MEMBER:
    case actions.REMOVE_MEMBER:
      return {
        ...state,
        [action.organization]: organizationMembers(organization, action)
      };
    default:
      return state;
  }
};

export default organizationsMembers;
