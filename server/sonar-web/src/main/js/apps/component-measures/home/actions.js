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
import { startFetching, stopFetching } from '../store/statusActions';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakValue } from '../utils';
import { getMeasuresAppComponent, getMeasuresAppAllMetrics } from '../../../store/rootReducer';

export const RECEIVE_MEASURES = 'measuresApp/home/RECEIVE_MEASURES';

export function receiveMeasures(measures, periods) {
  return { type: RECEIVE_MEASURES, measures, periods };
}

function banQualityGate(component, measures) {
  let newMeasures = [...measures];

  if (!['VW', 'SVW', 'APP'].includes(component.qualifier)) {
    newMeasures = newMeasures.filter(measure => measure.metric !== 'alert_status');
  }

  if (component.qualifier === 'APP') {
    newMeasures = newMeasures.filter(
      measure =>
        measure.metric !== 'releasability_rating' && measure.metric !== 'releasability_effort'
    );
  }

  return newMeasures;
}

export function fetchMeasures() {
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
      const measures = banQualityGate(component, r.component.measures)
        .map(measure => {
          const metric = metrics.find(metric => metric.key === measure.metric);
          const leak = getLeakValue(measure);
          return { ...measure, metric, leak };
        })
        .filter(measure => {
          const hasValue = measure.value != null;
          const hasLeakValue = measure.leak != null;
          return hasValue || hasLeakValue;
        });

      const newBugs = measures.find(measure => measure.metric.key === 'new_bugs');

      const applicationPeriods = newBugs ? [{ index: 1 }] : [];
      const periods = component.qualifier === 'APP' ? applicationPeriods : r.periods;

      dispatch(receiveMeasures(measures, periods));
      dispatch(stopFetching());
    });
  };
}
