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
import { startFetching, stopFetching } from '../store/statusActions';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import { getLeakValue } from '../utils';
import { getMeasuresAppComponent, getMeasuresAppAllMetrics } from '../../../app/store/rootReducer';

export const RECEIVE_MEASURES = 'measuresApp/home/RECEIVE_MEASURES';

export function receiveMeasures (measures, periods) {
  return { type: RECEIVE_MEASURES, measures, periods };
}

function banQualityGate (component, measures) {
  if (['VW', 'SVW'].includes(component.qualifier)) {
    return measures;
  }
  return measures.filter(measure => measure.metric !== 'alert_status');
}

export function fetchMeasures () {
  return (dispatch, getState) => {
    dispatch(startFetching());

    const state = getState();
    const component = getMeasuresAppComponent(state);
    const metrics = getMeasuresAppAllMetrics(state);

    const metricKeys = metrics
        .filter(metric => !metric.hidden)
        .filter(metric => metric.type !== 'DATA' && metric.type !== 'DISTRIB')
        .map(metric => metric.key);

    getMeasuresAndMeta(component.key, metricKeys, { additionalFields: 'periods' }).then(r => {
      const leakPeriod = getLeakPeriod(r.periods);
      const measures = banQualityGate(component, r.component.measures)
          .map(measure => {
            const metric = metrics.find(metric => metric.key === measure.metric);
            const leak = getLeakValue(measure);
            return { ...measure, metric, leak };
          })
          .filter(measure => {
            const hasValue = measure.value != null;
            const hasLeakValue = !!leakPeriod && measure.leak != null;
            return hasValue || hasLeakValue;
          });

      dispatch(receiveMeasures(measures, r.periods));
      dispatch(stopFetching());
    });
  };
}
