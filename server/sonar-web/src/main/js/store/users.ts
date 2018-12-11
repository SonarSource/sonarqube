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
import { listUserSettings, setUserSetting } from '../api/user-settings';
import { isLoggedIn } from '../helpers/users';

const enum Actions {
  ReceiveCurrentUser = 'RECEIVE_CURRENT_USER',
  ReceiveCurrentUserSettings = 'RECEIVE_CURRENT_USER_SETTINGS',
  SkipOnboardingAction = 'SKIP_ONBOARDING',
  SetHomePageAction = 'SET_HOMEPAGE'
}

type Action =
  | ActionType<typeof receiveCurrentUser, Actions.ReceiveCurrentUser>
  | ActionType<typeof receiveCurrentUserSettings, Actions.ReceiveCurrentUserSettings>
  | ActionType<typeof setHomePageAction, Actions.SetHomePageAction>
  | ActionType<typeof skipOnboardingAction, Actions.SkipOnboardingAction>;

export interface State {
  usersByLogin: { [login: string]: any };
  userLogins: string[];
  currentUser: T.CurrentUser;
  currentUserSettings: T.CurrentUserSettings;
}

export function receiveCurrentUser(user: T.CurrentUser) {
  return { type: Actions.ReceiveCurrentUser, user };
}

function receiveCurrentUserSettings(userSettings: T.CurrentUserSettingData[]) {
  return { type: Actions.ReceiveCurrentUserSettings, userSettings };
}

export function fetchCurrentUserSettings() {
  return (dispatch: Dispatch) => {
    listUserSettings().then(
      ({ userSettings }) => dispatch(receiveCurrentUserSettings(userSettings)),
      () => {}
    );
  };
}

export function setCurrentUserSetting(setting: T.CurrentUserSettingData) {
  return (dispatch: Dispatch) => {
    setUserSetting(setting).then(() => dispatch(receiveCurrentUserSettings([setting])), () => {});
  };
}

function skipOnboardingAction() {
  return { type: Actions.SkipOnboardingAction };
}

export function skipOnboarding() {
  return (dispatch: Dispatch) =>
    api
      .skipOnboarding()
      .then(() => dispatch(skipOnboardingAction()), () => dispatch(skipOnboardingAction()));
}

function setHomePageAction(homepage: T.HomePage) {
  return { type: Actions.SetHomePageAction, homepage };
}

export function setHomePage(homepage: T.HomePage) {
  return (dispatch: Dispatch) => {
    api.setHomePage(homepage).then(
      () => {
        dispatch(setHomePageAction(homepage));
      },
      () => {}
    );
  };
}

function usersByLogin(state: State['usersByLogin'] = {}, action: Action): State['usersByLogin'] {
  if (action.type === Actions.ReceiveCurrentUser && isLoggedIn(action.user)) {
    return { ...state, [action.user.login]: action.user };
  } else {
    return state;
  }
}

function userLogins(state: State['userLogins'] = [], action: Action): State['userLogins'] {
  if (action.type === Actions.ReceiveCurrentUser && isLoggedIn(action.user)) {
    return uniq([...state, action.user.login]);
  } else {
    return state;
  }
}

function currentUser(
  state: State['currentUser'] = { isLoggedIn: false },
  action: Action
): State['currentUser'] {
  if (action.type === Actions.ReceiveCurrentUser) {
    return action.user;
  }
  if (action.type === Actions.SkipOnboardingAction && isLoggedIn(state)) {
    return { ...state, showOnboardingTutorial: false } as T.LoggedInUser;
  }
  if (action.type === Actions.SetHomePageAction && isLoggedIn(state)) {
    return { ...state, homepage: action.homepage } as T.LoggedInUser;
  }
  return state;
}

function currentUserSettings(
  state: State['currentUserSettings'] = {},
  action: Action
): State['currentUserSettings'] {
  if (action.type === Actions.ReceiveCurrentUserSettings) {
    const newState = { ...state };
    action.userSettings.forEach((item: T.CurrentUserSettingData) => {
      newState[item.key] = item.value;
    });
    return newState;
  }
  return state;
}

export default combineReducers({ usersByLogin, userLogins, currentUser, currentUserSettings });

export function getCurrentUser(state: State) {
  return state.currentUser;
}

export function getCurrentUserSettings(state: State) {
  return state.currentUserSettings;
}

export function getUserByLogin(state: State, login: string) {
  return state.usersByLogin[login];
}

export function getUsersByLogins(state: State, logins: string[]) {
  return logins.map(login => getUserByLogin(state, login));
}
