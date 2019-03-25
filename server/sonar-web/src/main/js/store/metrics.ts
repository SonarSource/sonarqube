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
import { keyBy } from 'lodash';
import { combineReducers } from 'redux';
import { ActionType } from './utils/actions';

export function receiveMetrics(metrics: T.Metric[]) {
  return { type: 'RECEIVE_METRICS', metrics };
}

type Action = ActionType<typeof receiveMetrics, 'RECEIVE_METRICS'>;

export type State = { byKey: T.Dict<T.Metric>; keys: string[] };

const byKey = (state: State['byKey'] = {}, action: Action) => {
  if (action.type === 'RECEIVE_METRICS') {
    return keyBy(action.metrics, 'key');
  }
  return state;
};

const keys = (state: State['keys'] = [], action: Action) => {
  if (action.type === 'RECEIVE_METRICS') {
    return action.metrics.map(f => f.key);
  }

  return state;
};

export default combineReducers({ byKey, keys });

export function getMetrics(state: State) {
  return state.byKey;
}

export function getMetricsKey(state: State) {
  return state.keys;
}
