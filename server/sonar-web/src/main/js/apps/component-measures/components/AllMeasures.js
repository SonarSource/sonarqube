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
import _ from 'underscore';
import React from 'react';

import Spinner from './Spinner';
import AllMeasuresDomain from './AllMeasuresDomain';
import { getLeakValue } from '../utils';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakPeriod, getLeakPeriodLabel } from '../../../helpers/periods';

export default class AllMeasures extends React.Component {
  state = {
    fetching: true,
    measures: []
  };

  componentDidMount () {
    this.mounted = true;
    this.fetchMeasures();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchMeasures () {
    const { component } = this.context;
    const { metrics } = this.props;
    const metricKeys = metrics
        .filter(metric => !metric.hidden)
        .filter(metric => metric.type !== 'DATA' && metric.type !== 'DISTRIB')
        .map(metric => metric.key);

    getMeasuresAndMeta(component.key, metricKeys, { additionalFields: 'periods' }).then(r => {
      if (this.mounted) {
        const leakPeriod = getLeakPeriod(r.periods);
        const measures = r.component.measures
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

        this.setState({
          measures,
          periods: r.periods,
          fetching: false
        });
      }
    });
  }

  render () {
    const { fetching, measures, periods } = this.state;

    if (fetching) {
      return <Spinner/>;
    }

    const { component } = this.context;
    const domains = _.sortBy(_.pairs(_.groupBy(measures, measure => measure.metric.domain)).map(r => {
      const [name, measures] = r;
      const sortedMeasures = _.sortBy(measures, measure => measure.metric.name);

      return { name, measures: sortedMeasures };
    }), 'name');

    const leakPeriodLabel = getLeakPeriodLabel(periods);

    return (
        <ul className="measures-domains">
          {domains.map((domain, index) => (
              <AllMeasuresDomain
                  key={domain.name}
                  domain={domain}
                  component={component}
                  displayLeakHeader={index === 0}
                  leakPeriodLabel={leakPeriodLabel}/>
          ))}
        </ul>
    );
  }
}

AllMeasures.contextTypes = {
  component: React.PropTypes.object
};
