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
import React from 'react';

import Spinner from './Spinner';
import MeasureDetailsHeader from './MeasureDetailsHeader';
import MeasureDrilldown from './MeasureDrilldown';

import { enhanceWithLeak } from '../utils';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakPeriod, getPeriodDate, getPeriodLabel } from '../../../helpers/periods';

export default class MeasureDetails extends React.Component {
  state = {};

  componentWillMount () {
    const { metrics } = this.props;
    const { metricKey } = this.props.params;

    this.metric = metrics.find(metric => metric.key === metricKey);

    if (!this.metric) {
      const { router, component } = this.context;

      router.replace({
        pathname: '/',
        query: { id: component.key }
      });
    }
  }

  componentDidMount () {
    this.mounted = true;
    this.fetchMeasure();
  }

  componentDidUpdate (nextProps, nextState, nextContext) {
    if ((nextProps.params.metricKey !== this.props.params.metricKey) ||
        (nextContext.component !== this.context.component)) {
      this.fetchMeasure();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchMeasure () {
    const { metricKey } = this.props.params;
    const { component } = this.context;

    getMeasuresAndMeta(
        component.key,
        [metricKey],
        { additionalFields: 'periods' }
    ).then(r => {
      const measures = r.component.measures;

      if (this.mounted && measures.length === 1) {
        const measure = enhanceWithLeak(measures[0]);

        this.setState({
          measure,
          periods: r.periods
        });
      }
    });
  }

  render () {
    const { measure, periods } = this.state;

    if (!measure) {
      return <Spinner/>;
    }

    const { tab } = this.props.params;
    const leakPeriod = getLeakPeriod(periods);
    const leakPeriodLabel = getPeriodLabel(leakPeriod);
    const leakPeriodDate = getPeriodDate(leakPeriod);

    return (
        <div className="measure-details">
          <MeasureDetailsHeader
              measure={measure}
              metric={this.metric}
              leakPeriodLabel={leakPeriodLabel}/>

          {measure && (
              <MeasureDrilldown
                  metric={this.metric}
                  tab={tab}
                  leakPeriodDate={leakPeriodDate}>
                {this.props.children}
              </MeasureDrilldown>
          )}
        </div>
    );
  }
}

MeasureDetails.contextTypes = {
  component: React.PropTypes.object,
  router: React.PropTypes.object
};
