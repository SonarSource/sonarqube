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
import { uniq } from 'lodash';
import { RECEIVE_CURRENT_USER, SKIP_ONBOARDING, SET_HOMEPAGE } from './actions';
import { CurrentUser } from '../../app/types';

interface UsersByLogin {
  [login: string]: any;
}

const usersByLogin = (state: UsersByLogin = {}, action: any = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return { ...state, [action.user.login]: action.user };
  } else {
    return state;
  }
};

type UserLogins = string[];

const userLogins = (state: UserLogins = [], action: any = {}) => {
  if (action.type === RECEIVE_CURRENT_USER) {
    return uniq([...state, action.user.login]);
  } else {
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
export const getUserByLogin = (state: State, login: string) => state.usersByLogin[login];
export const getUsersByLogins = (state: State, logins: string[]) =>
  logins.map(login => getUserByLogin(state, login));
