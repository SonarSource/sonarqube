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
import { uniq } from 'lodash';
import { Dispatch, combineReducers } from 'redux';
import { ActionType } from './utils/actions';
import { isLoggedIn } from '../helpers/users';
import * as api from '../api/users';

const enum Actions {
  ReceiveCurrentUser = 'RECEIVE_CURRENT_USER',
  SetCurrentUserSetting = 'SET_CURRENT_USER_SETTING',
  SkipOnboardingAction = 'SKIP_ONBOARDING',
  SetHomePageAction = 'SET_HOMEPAGE'
}

type Action =
  | ActionType<typeof receiveCurrentUser, Actions.ReceiveCurrentUser>
  | ActionType<typeof setCurrentUserSettingAction, Actions.SetCurrentUserSetting>
  | ActionType<typeof setHomePageAction, Actions.SetHomePageAction>
  | ActionType<typeof skipOnboardingAction, Actions.SkipOnboardingAction>;

export interface State {
  usersByLogin: T.Dict<any>;
  userLogins: string[];
  currentUser: T.CurrentUser;
}

export function receiveCurrentUser(user: T.CurrentUser) {
  return { type: Actions.ReceiveCurrentUser, user };
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

function setCurrentUserSettingAction(setting: T.CurrentUserSetting) {
  return { type: Actions.SetCurrentUserSetting, setting };
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

export function setCurrentUserSetting(setting: T.CurrentUserSetting) {
  return (dispatch: Dispatch, getState: () => { users: State }) => {
    const oldSetting = getCurrentUserSetting(getState().users, setting.key);
    dispatch(setCurrentUserSettingAction(setting));
    api.setUserSetting(setting).then(
      () => {},
      () => {
        dispatch(setCurrentUserSettingAction({ ...setting, value: oldSetting || '' }));
      }
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
  if (action.type === Actions.SetCurrentUserSetting && isLoggedIn(state)) {
    let settings: T.CurrentUserSetting[];
    if (state.settings) {
      settings = [...state.settings];
      const index = settings.findIndex(setting => setting.key === action.setting.key);
      if (index === -1) {
        settings.push(action.setting);
      } else {
        settings[index] = action.setting;
      }
    } else {
      settings = [action.setting];
    }
    return { ...state, settings } as T.LoggedInUser;
  }
  return state;
}

export default combineReducers({ usersByLogin, userLogins, currentUser });

export function getCurrentUser(state: State) {
  return state.currentUser;
}

export function getCurrentUserSetting(state: State, key: T.CurrentUserSettingNames) {
  let setting;
  if (isLoggedIn(state.currentUser) && state.currentUser.settings) {
    setting = state.currentUser.settings.find(setting => setting.key === key);
  }
  return setting && setting.value;
}

export function getUserByLogin(state: State, login: string) {
  return state.usersByLogin[login];
}

export function getUsersByLogins(state: State, logins: string[]) {
  return logins.map(login => getUserByLogin(state, login));
}
