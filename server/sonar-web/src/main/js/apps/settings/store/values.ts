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
import { combineReducers } from 'redux';
import { keyBy } from 'lodash';
import { ActionType } from '../../../store/utils/actions';
import { Action as AppStateAction, Actions as AppStateActions } from '../../../store/appState';

enum Actions {
  receiveValues = 'RECEIVE_VALUES'
}

type Action = ActionType<typeof receiveValues, Actions.receiveValues>;

type SettingsState = T.Dict<T.SettingValue>;

export interface State {
  components: T.Dict<SettingsState>;
  global: SettingsState;
}

export function receiveValues(
  settings: Array<{ key: string; value?: string }>,
  component?: string
) {
  return { type: Actions.receiveValues, settings, component };
}

function components(state: State['components'] = {}, action: Action) {
  const { component: key } = action;
  if (!key) {
    return state;
  }
  if (action.type === Actions.receiveValues) {
    const settingsByKey = keyBy(action.settings, 'key');
    return { ...state, [key]: { ...(state[key] || {}), ...settingsByKey } };
  }
  return state;
}

function global(state: State['components'] = {}, action: Action | AppStateAction) {
  if (action.type === Actions.receiveValues) {
    if (action.component) {
      return state;
    }
    const settingsByKey = keyBy(action.settings, 'key');
    return { ...state, ...settingsByKey };
  }
  if (action.type === AppStateActions.SetAppState) {
    const settingsByKey: SettingsState = {};
    Object.keys(action.appState.settings).forEach(
      key => (settingsByKey[key] = { key, value: action.appState.settings[key] })
    );
    return { ...state, ...settingsByKey };
  }
  return state;
}

export default combineReducers({ components, global });

export function getValue(
  state: State,
  key: string,
  component?: string
): T.SettingValue | undefined {
  if (component) {
    return state.components[component] && state.components[component][key];
  }
  return state.global[key];
}
