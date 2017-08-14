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
// @flow
import { keyBy } from 'lodash';
import { RECEIVE_VALUES } from './actions';

/*::
type State = { [key: string]: {} };
*/

const reducer = (state /*: State */ = {}, action /*: Object */) => {
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

export default reducer;

export const getValue = (state /*: State */, key /*: string */) => state[key];
