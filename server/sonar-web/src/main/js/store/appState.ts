/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { AppState, Extension } from '../types/types';
import { ActionType } from './utils/actions';

export const enum Actions {
  SetAppState = 'SET_APP_STATE',
  SetAdminPages = 'SET_ADMIN_PAGES',
  RequireAuthorization = 'REQUIRE_AUTHORIZATION'
}

export type Action =
  | ActionType<typeof setAppState, Actions.SetAppState>
  | ActionType<typeof setAdminPages, Actions.SetAdminPages>
  | ActionType<typeof requireAuthorization, Actions.RequireAuthorization>;

export function setAppState(appState: AppState) {
  return { type: Actions.SetAppState, appState };
}

export function setAdminPages(adminPages: Extension[]) {
  return { type: Actions.SetAdminPages, adminPages };
}

export function requireAuthorization() {
  return { type: Actions.RequireAuthorization };
}

const defaultValue: AppState = {
  authenticationError: false,
  authorizationError: false,
  edition: undefined,
  productionDatabase: true,
  qualifiers: [],
  settings: {},
  version: ''
};

export default function(state: AppState = defaultValue, action: Action): AppState {
  if (action.type === Actions.SetAppState) {
    return { ...state, ...action.appState };
  }
  if (action.type === Actions.SetAdminPages) {
    return { ...state, adminPages: action.adminPages };
  }
  if (action.type === Actions.RequireAuthorization) {
    return { ...state, authorizationError: true };
  }
  return state;
}
