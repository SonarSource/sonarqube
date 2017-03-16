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
import {
  RECEIVE_COMPONENT_MEASURE,
  RECEIVE_COMPONENT_MEASURES,
  RECEIVE_COMPONENTS_MEASURES
} from './actions';

const byMetricKey = (state = {}, action = {}) => {
  if (action.type === RECEIVE_COMPONENT_MEASURE) {
    return { ...state, [action.metricKey]: action.value };
  }

  if (action.type === RECEIVE_COMPONENT_MEASURES) {
    return { ...state, ...action.measures };
  }

  return state;
};

const reducer = (state = {}, action = {}) => {
  if ([RECEIVE_COMPONENT_MEASURE, RECEIVE_COMPONENT_MEASURES].includes(action.type)) {
    const component = state[action.componentKey];
    return { ...state, [action.componentKey]: byMetricKey(component, action) };
  }

  if (action.type === RECEIVE_COMPONENTS_MEASURES) {
    const newState = { ...state };
    Object.keys(action.componentsWithMeasures).forEach(componentKey => {
      Object.assign(newState, {
        [componentKey]: byMetricKey(state[componentKey], {
          type: RECEIVE_COMPONENT_MEASURES,
          measures: action.componentsWithMeasures[componentKey]
        })
      });
    });
    return newState;
  }

  return state;
};

export default reducer;

export const getComponentMeasure = (state, componentKey, metricKey) => {
  const component = state[componentKey];
  return component && component[metricKey];
};

export const getComponentMeasures = (state, componentKey) => state[componentKey];
