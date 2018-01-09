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
import { uniq, keyBy } from 'lodash';
import { RECEIVE_CURRENT_USER, RECEIVE_USER, SKIP_ONBOARDING, SET_HOMEPAGE } from './actions';
import { actions as membersActions } from '../organizationsMembers/actions';
import { CurrentUser } from '../../app/types';

interface UsersByLogin {
  [login: string]: any;
}

const usersByLogin = (state: UsersByLogin = {}, action: any = {}) => {
  switch (action.type) {
    case RECEIVE_CURRENT_USER:
    case RECEIVE_USER:
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

type UserLogins = string[];

const userLogins = (state: UserLogins = [], action: any = {}) => {
  switch (action.type) {
    case RECEIVE_CURRENT_USER:
    case RECEIVE_USER:
      return uniq([...state, action.user.login]);
    case membersActions.RECEIVE_MEMBERS:
    case membersActions.RECEIVE_MORE_MEMBERS:
      return uniq([...state, action.members.map((member: any) => member.login)]);
    case membersActions.ADD_MEMBER: {
      return uniq([...state, action.member.login]).sort();
    }
    default:
      return state;
  }
};

const currentUser = (state: CurrentUser | null = null, action: any = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return action.user;
  }
  if (action.type === SKIP_ONBOARDING) {
    return state ? { ...state, showOnboardingTutorial: false } : null;
  }
  if (action.type === SET_HOMEPAGE) {
    return state && { ...state, homepage: action.homepage };
  }
  return state;
};

interface State {
  usersByLogin: UsersByLogin;
  userLogins: UserLogins;
  currentUser: CurrentUser | null;
}

export default combineReducers({ usersByLogin, userLogins, currentUser });

export const getCurrentUser = (state: State) => state.currentUser!;
export const getUserLogins = (state: State) => state.userLogins;
export const getUserByLogin = (state: State, login: string) => state.usersByLogin[login];
export const getUsersByLogins = (state: State, logins: string[]) =>
  logins.map(login => getUserByLogin(state, login));
export const getUsers = (state: State) => getUsersByLogins(state, getUserLogins(state));
