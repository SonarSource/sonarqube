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
import { Dispatch, combineReducers } from 'redux';
import { ActionType } from './utils/actions';
import * as api from '../api/users';
import { CurrentUser, HomePage, LoggedInUser } from '../app/types';
import { isLoggedIn } from '../helpers/users';

export function receiveCurrentUser(user: CurrentUser) {
  return { type: 'RECEIVE_CURRENT_USER', user };
}

function skipOnboardingAction() {
  return { type: 'SKIP_ONBOARDING' };
}

export function skipOnboarding() {
  return (dispatch: Dispatch) =>
    api
      .skipOnboarding()
      .then(() => dispatch(skipOnboardingAction()), () => dispatch(skipOnboardingAction()));
}

function setHomePageAction(homepage: HomePage) {
  return { type: 'SET_HOMEPAGE', homepage };
}

export function setHomePage(homepage: HomePage) {
  return (dispatch: Dispatch) => {
    api.setHomePage(homepage).then(
      () => {
        dispatch(setHomePageAction(homepage));
      },
      () => {}
    );
  };
}

type Action =
  | ActionType<typeof receiveCurrentUser, 'RECEIVE_CURRENT_USER'>
  | ActionType<typeof skipOnboardingAction, 'SKIP_ONBOARDING'>
  | ActionType<typeof setHomePageAction, 'SET_HOMEPAGE'>;

export interface State {
  usersByLogin: { [login: string]: any };
  userLogins: string[];
  currentUser: CurrentUser;
}

function usersByLogin(state: State['usersByLogin'] = {}, action: Action): State['usersByLogin'] {
  if (action.type === 'RECEIVE_CURRENT_USER' && isLoggedIn(action.user)) {
    return { ...state, [action.user.login]: action.user };
  } else {
    return state;
  }
}

function userLogins(state: State['userLogins'] = [], action: Action): State['userLogins'] {
  if (action.type === 'RECEIVE_CURRENT_USER' && isLoggedIn(action.user)) {
    return uniq([...state, action.user.login]);
  } else {
    return state;
  }
}

function currentUser(
  state: State['currentUser'] = { isLoggedIn: false },
  action: Action
): State['currentUser'] {
  if (action.type === 'RECEIVE_CURRENT_USER') {
    return action.user;
  }
  if (action.type === 'SKIP_ONBOARDING' && isLoggedIn(state)) {
    return { ...state, showOnboardingTutorial: false } as LoggedInUser;
  }
  if (action.type === 'SET_HOMEPAGE' && isLoggedIn(state)) {
    return { ...state, homepage: action.homepage } as LoggedInUser;
  }
  return state;
}

export default combineReducers({ usersByLogin, userLogins, currentUser });

export function getCurrentUser(state: State) {
  return state.currentUser;
}

export function getUserByLogin(state: State, login: string) {
  return state.usersByLogin[login];
}

export function getUsersByLogins(state: State, logins: string[]) {
  return logins.map(login => getUserByLogin(state, login));
}
