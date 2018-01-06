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
// @flow
import { combineReducers } from 'redux';
import { keyBy } from 'lodash';
import { RECEIVE_VALUES } from './actions';

/*::
type SettingsState = { [key: string]: {} };
type ComponentsState = { [key: string]: SettingsState };
export type State = { components: ComponentsState, global: SettingsState };
*/

const componentsSettings = (state /*: ComponentsState */ = {}, action /*: Object */) => {
  if (!action.componentKey) {
    return state;
  }

  const key = action.componentKey;
  if (action.type === RECEIVE_VALUES) {
    const settingsByKey = keyBy(action.settings, 'key');
    return { ...state, [key]: { ...(state[key] || {}), ...settingsByKey } };
  }

  return state;
};

const globalSettings = (state /*: SettingsState */ = {}, action /*: Object */) => {
  if (action.componentKey) {
    return state;
  }

  if (action.type === RECEIVE_VALUES) {
    const settingsByKey = keyBy(action.settings, 'key');
    return { ...state, ...settingsByKey };
  }

  if (action.type === 'SET_APP_STATE') {
    const settingsByKey = {};
    Object.keys(action.appState.settings).forEach(
      key => (settingsByKey[key] = { value: action.appState.settings[key] })
    );
    return { ...state, ...settingsByKey };
  }

  return state;
};

export default combineReducers({ components: componentsSettings, global: globalSettings });

export const getValue = (state /*: State */, key /*: string */, componentKey /*: ?string */) => {
  let settings = state.global;
  if (componentKey) {
    settings = state.components[componentKey];
  }
  return settings && settings[key];
};
