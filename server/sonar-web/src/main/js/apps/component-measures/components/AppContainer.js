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
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import App from './App';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { getComponent, getMetrics, getMetricByKey } from '../../../store/rootReducer';
import { fetchMetrics } from '../../../store/rootActions';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakValue } from '../utils';
import type { Component } from '../types';
import type { Metrics } from '../../../store/metrics/actions';
import type { Measure, MeasureEnhanced } from '../../../components/measure/types';

const mapStateToProps = (state, ownProps) => ({
  component: getComponent(state, ownProps.location.query.id),
  metrics: getMetrics(state)
});

const banQualityGate = (component: Component, measures: Array<Measure>): Array<Measure> => {
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
};

const fetchMeasures = (component: Component, metrics: Metrics) => (
  dispatch,
  getState
): Promise<Array<MeasureEnhanced>> => {
  const metricKeys = metrics.filter(key => {
    const metric = getMetricByKey(getState(), key);
    return !metric.hidden && !['DATA', 'DISTRIB'].includes(metric.type);
  });

  if (metricKeys.length <= 0) {
    return Promise.resolve([]);
  }

  return getMeasuresAndMeta(component.key, metricKeys, { additionalFields: 'periods' }).then(r => {
    const measures: Array<MeasureEnhanced> = banQualityGate(component, r.component.measures)
      .map(measure => {
        const metric = getMetricByKey(getState(), measure.metric);
        const leak = getLeakValue(measure);
        return { value: measure.value, periods: measure.periods, metric, leak };
      })
      .filter(measure => {
        const hasValue = measure.value != null;
        const hasLeakValue = measure.leak != null;
        return hasValue || hasLeakValue;
      });

    const newBugs = measures.find(measure => measure.metric.key === 'new_bugs');
    const applicationPeriods = newBugs ? [{ index: 1 }] : [];
    const periods = component.qualifier === 'APP' ? applicationPeriods : r.periods;
    return { measures, periods };
  }, throwGlobalError);
};

const mapDispatchToProps = { fetchMeasures, fetchMetrics };

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(App));
