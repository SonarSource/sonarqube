/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { combineReducers, Dispatch } from 'redux';
import * as api from '../api/users';
import { isLoggedIn } from '../helpers/users';
import {
  CurrentUser,
  CurrentUserSetting,
  CurrentUserSettingNames,
  Dict,
  HomePage,
  LoggedInUser
} from '../types/types';
import { ActionType } from './utils/actions';

const enum Actions {
  ReceiveCurrentUser = 'RECEIVE_CURRENT_USER',
  SetCurrentUserSetting = 'SET_CURRENT_USER_SETTING',
  SetHomePageAction = 'SET_HOMEPAGE',
  SetSonarlintAd = 'SET_SONARLINT_AD'
}

type Action =
  | ActionType<typeof receiveCurrentUser, Actions.ReceiveCurrentUser>
  | ActionType<typeof setCurrentUserSettingAction, Actions.SetCurrentUserSetting>
  | ActionType<typeof setHomePageAction, Actions.SetHomePageAction>
  | ActionType<typeof setSonarlintAd, Actions.SetSonarlintAd>;

export interface State {
  usersByLogin: Dict<any>;
  userLogins: string[];
  currentUser: CurrentUser;
}

export function receiveCurrentUser(user: CurrentUser) {
  return { type: Actions.ReceiveCurrentUser, user };
}

export function setHomePageAction(homepage: HomePage) {
  return { type: Actions.SetHomePageAction, homepage };
}

export function setCurrentUserSettingAction(setting: CurrentUserSetting) {
  return { type: Actions.SetCurrentUserSetting, setting };
}

export function setSonarlintAd() {
  return { type: Actions.SetSonarlintAd };
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

export function setCurrentUserSetting(setting: CurrentUserSetting) {
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
  if (action.type === Actions.SetHomePageAction && isLoggedIn(state)) {
    return { ...state, homepage: action.homepage } as LoggedInUser;
  }
  if (action.type === Actions.SetCurrentUserSetting && isLoggedIn(state)) {
    let settings: CurrentUserSetting[];
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
    return { ...state, settings } as LoggedInUser;
  }
  if (action.type === Actions.SetSonarlintAd && isLoggedIn(state)) {
    return { ...state, sonarLintAdSeen: true } as LoggedInUser;
  }
  return state;
}

export default combineReducers({ usersByLogin, userLogins, currentUser });

export function getCurrentUser(state: State) {
  return state.currentUser;
}

export function getCurrentUserSetting(state: State, key: CurrentUserSettingNames) {
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
