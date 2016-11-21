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
import { getMeasuresAndMeta } from '../../../api/measures';
import { enhanceWithLeak } from '../utils';
import { getMeasuresAppComponent, getMeasuresAppAllMetrics } from '../../../app/store/rootReducer';

/*
 * Actions
 */

export const REQUEST_MEASURE = 'measuresApp/details/REQUEST_MEASURE';
export const RECEIVE_MEASURE = 'measuresApp/details/RECEIVE_MEASURE';

/*
 * Action Creators
 */

function requestMeasure (metric) {
  return { type: REQUEST_MEASURE, metric };
}

function receiveMeasure (measure, secondaryMeasure, periods) {
  return { type: RECEIVE_MEASURE, measure, secondaryMeasure, periods };
}

/*
 * Workflow
 */

export function fetchMeasure (metricKey, periodIndex = 1) {
  return (dispatch, getState) => {
    const state = getState();
    const component = getMeasuresAppComponent(state);
    const metrics = getMeasuresAppAllMetrics(state);
    const metricsToRequest = [metricKey];

    if (metricKey === 'ncloc') {
      metricsToRequest.push('ncloc_language_distribution');
    }
    if (metricKey === 'function_complexity') {
      metricsToRequest.push('function_complexity_distribution');
    }
    if (metricKey === 'file_complexity') {
      metricsToRequest.push('file_complexity_distribution');
    }

    const metric = metrics.find(m => m.key === metricKey);
    dispatch(requestMeasure(metric));

    getMeasuresAndMeta(
        component.key,
        metricsToRequest,
        { additionalFields: 'periods' }
    ).then(r => {
      const measures = enhanceWithLeak(r.component.measures, periodIndex);
      const measure = measures.find(m => m.metric === metricKey);
      const secondaryMeasure = measures.find(m => m.metric !== metricKey);
      dispatch(receiveMeasure(measure, secondaryMeasure, r.periods));
    });
  };
}
