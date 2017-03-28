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
import { combineReducers } from 'redux';
import { uniq, keyBy } from 'lodash';
import { RECEIVE_CURRENT_USER } from './actions';
import { actions as membersActions } from '../organizationsMembers/actions';

const usersByLogin = (state = {}, action = {}) => {
  switch (action.type) {
    case RECEIVE_CURRENT_USER:
      return { ...state, [action.user.login]: action.user };
    case membersActions.RECEIVE_MEMBERS:
    case membersActions.RECEIVE_MORE_MEMBERS:
      return { ...state, ...keyBy(action.members, 'login') };
    case membersActions.ADD_MEMBER:
      return { ...state, [action.member.login]: action.member };
    default:
      return state;
  }
};

const userLogins = (state = [], action = {}) => {
  switch (action.type) {
    case RECEIVE_CURRENT_USER:
      return uniq([...state, action.user.login]);
    case membersActions.RECEIVE_MEMBERS:
    case membersActions.RECEIVE_MORE_MEMBERS:
      return uniq([...state, action.members.map(member => member.login)]);
    case membersActions.ADD_MEMBER: {
      return uniq([...state, action.member.login]).sort();
    }
    default:
      return state;
  }
};

const currentUser = (state = null, action = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return action.user;
  }
  return state;
};

export default combineReducers({ usersByLogin, userLogins, currentUser });

export const getCurrentUser = state => state.currentUser;
export const getUserLogins = state => state.userLogins;
export const getUserByLogin = (state, login) => state.usersByLogin[login];
export const getUsersByLogins = (state, logins) =>
  logins.map(login => getUserByLogin(state, login));
export const getUsers = state => getUsersByLogins(state, getUserLogins(state));
