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
import { RECEIVE_METRICS } from './actions';
/*:: import type { Metric } from './actions'; */

/*::
type StateByKey = { [string]: Metric };
type StateKeys = Array<string>;
type State = { byKey: StateByKey, keys: StateKeys };
*/

const byKey = (state /*: StateByKey */ = {}, action = {}) => {
  if (action.type === RECEIVE_METRICS) {
    return keyBy(action.metrics, 'key');
  }
  return state;
};

const keys = (state /*: StateKeys */ = [], action = {}) => {
  if (action.type === RECEIVE_METRICS) {
    return action.metrics.map(f => f.key);
  }

  return state;
};

export default combineReducers({ byKey, keys });

export const getMetrics = (state /*: State */) => state.byKey;
export const getMetricByKey = (state /*: State */, key /*: string */) => state.byKey[key];
export const getMetricsKey = (state /*: State */) => state.keys;
