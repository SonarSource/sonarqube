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
import { ActionType } from './utils/actions';
import { EditionKey } from '../apps/marketplace/utils';

export const enum Actions {
  SetAppState = 'SET_APP_STATE',
  SetAdminPages = 'SET_ADMIN_PAGES',
  RequireAuthorization = 'REQUIRE_AUTHORIZATION'
}

export type Action =
  | ActionType<typeof setAppState, Actions.SetAppState>
  | ActionType<typeof setAdminPages, Actions.SetAdminPages>
  | ActionType<typeof requireAuthorization, Actions.RequireAuthorization>;

export function setAppState(appState: T.AppState) {
  return { type: Actions.SetAppState, appState };
}

export function setAdminPages(adminPages: T.Extension[]) {
  return { type: Actions.SetAdminPages, adminPages };
}

export function requireAuthorization() {
  return { type: Actions.RequireAuthorization };
}

const defaultValue: T.AppState = {
  authenticationError: false,
  authorizationError: false,
  defaultOrganization: '',
  edition: EditionKey.community,
  organizationsEnabled: false,
  productionDatabase: true,
  qualifiers: [],
  settings: {},
  version: ''
};

export default function(state: T.AppState = defaultValue, action: Action): T.AppState {
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
