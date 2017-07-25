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
import { DISPLAY_HOME } from '../app/actions';
import { REQUEST_MEASURE, RECEIVE_MEASURE } from './actions';

const initialState = {
  metric: undefined,
  secondaryMeasure: undefined,
  measure: undefined,
  periods: undefined
};

export default function appReducer(state = initialState, action = {}) {
  switch (action.type) {
    case DISPLAY_HOME:
      return initialState;
    case REQUEST_MEASURE:
      return { ...state, metric: action.metric };
    case RECEIVE_MEASURE:
      return {
        ...state,
        measure: action.measure,
        secondaryMeasure: action.secondaryMeasure,
        periods: action.periods
      };
    default:
      return state;
  }
}
