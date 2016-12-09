/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
// @flow
type AppState = {
  authenticationError: boolean,
  authorizationError: boolean,
  qualifiers: ?Array<string>
};

export type Action = {
  type: string,
  appState: AppState
}

export const actions = {
  SET_APP_STATE: 'SET_APP_STATE',
  REQUIRE_AUTHENTICATION: 'REQUIRE_AUTHENTICATION',
  REQUIRE_AUTHORIZATION: 'REQUIRE_AUTHORIZATION'
};

export const setAppState = (appState: AppState): Action => ({
  type: actions.SET_APP_STATE,
  appState
});

export const requireAuthentication = () => ({
  type: actions.REQUIRE_AUTHENTICATION
});

export const requireAuthorization = () => ({
  type: actions.REQUIRE_AUTHORIZATION
});

const defaultValue = {
  authenticationError: false,
  authorizationError: false,
  qualifiers: null
};

export default (state: AppState = defaultValue, action: Action) => {
  if (action.type === actions.SET_APP_STATE) {
    return { ...state, ...action.appState };
  }

  if (action.type === actions.REQUIRE_AUTHENTICATION) {
    return { ...state, authenticationError: true };
  }

  if (action.type === actions.REQUIRE_AUTHORIZATION) {
    return { ...state, authorizationError: true };
  }

  return state;
};

export const getRootQualifiers = (state: AppState) => (
    state.qualifiers
);
