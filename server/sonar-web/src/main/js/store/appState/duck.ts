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
interface AppState {
  adminPages?: any[];
  authenticationError: boolean;
  authorizationError: boolean;
  organizationsEnabled: boolean;
  qualifiers?: string[];
}

interface SetAppStateAction {
  type: 'SET_APP_STATE';
  appState: AppState;
}

interface SetAdminPagesAction {
  type: 'SET_ADMIN_PAGES';
  adminPages: any[];
}

interface RequireAuthorizationAction {
  type: 'REQUIRE_AUTHORIZATION';
}

export type Action = SetAppStateAction | SetAdminPagesAction | RequireAuthorizationAction;

export function setAppState(appState: AppState): SetAppStateAction {
  return {
    type: 'SET_APP_STATE',
    appState
  };
}

export function setAdminPages(adminPages: any[]): SetAdminPagesAction {
  return { type: 'SET_ADMIN_PAGES', adminPages };
}

export function requireAuthorization(): RequireAuthorizationAction {
  return { type: 'REQUIRE_AUTHORIZATION' };
}

const defaultValue: AppState = {
  authenticationError: false,
  authorizationError: false,
  organizationsEnabled: false
};

export default function(state: AppState = defaultValue, action: Action): AppState {
  if (action.type === 'SET_APP_STATE') {
    return { ...state, ...action.appState };
  }

  if (action.type === 'SET_ADMIN_PAGES') {
    return { ...state, adminPages: action.adminPages };
  }

  if (action.type === 'REQUIRE_AUTHORIZATION') {
    return { ...state, authorizationError: true };
  }

  return state;
}

export function getRootQualifiers(state: AppState): string[] | undefined {
  return state.qualifiers;
}
