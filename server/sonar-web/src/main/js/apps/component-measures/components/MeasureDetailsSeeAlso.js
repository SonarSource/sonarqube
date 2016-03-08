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
import { Link } from 'react-router';

import Spinner from './Spinner';
import { getLeakValue, formatLeak } from '../utils';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import { formatMeasure } from '../../../helpers/measures';

export default class MeasureDetailsSeeAlso extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      measures: [],
      fetching: true
    };
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.handleClick = this.handleClick.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    window.addEventListener('keydown', this.handleKeyDown);
    window.addEventListener('click', this.handleClick);
    this.fetchMeasure();
  }

  componentDidUpdate (nextProps, nextState, nextContext) {
    if ((nextProps.metric !== this.props.metric) ||
        (nextContext.component !== this.context.component)) {
      this.fetchMeasure();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
    window.removeEventListener('keydown', this.handleKeyDown);
    window.removeEventListener('click', this.handleClick);
  }

  handleKeyDown (e) {
    // escape
    if (e.keyCode === 27) {
      this.props.onClose();
    }
  }

  handleClick () {
    this.props.onClose();
  }

  fetchMeasure () {
    const { metric } = this.props;
    const { component, metrics } = this.context;
    const sameDomainMetrics = metrics.filter(candidate => candidate.domain === metric.domain);
    const metricKeys = sameDomainMetrics
        .filter(m => !m.hidden)
        .filter(m => m.type !== 'DATA' && m.type !== 'DISTRIB')
        .filter(m => m.key !== metric.key)
        .map(m => m.key);

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
        const sortedMeasures = _.sortBy(measures, measure => measure.metric.name);

        this.setState({
          measures: sortedMeasures,
          periods: r.periods,
          fetching: false
        });
      }
    });
  }

  render () {
    const { measures, fetching, periods } = this.state;
    const { component } = this.context;

    if (fetching) {
      return (
          <div className="measure-details-see-also">
            <Spinner/>
          </div>
      );
    }

    const leakPeriod = getLeakPeriod(periods);
    const hasLeak = !!leakPeriod;

    return (
        <div className="measure-details-see-also">
          <ul className="domain-measures">
            {measures.map(measure => (
                <li key={measure.metric.key}>
                  <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                    <div className="domain-measures-name">
                      <span>{measure.metric.name}</span>
                    </div>
                    <div className="domain-measures-value">
                      {measure.value != null && (
                          <span>
                          {formatMeasure(measure.value, measure.metric.type)}
                        </span>
                      )}
                    </div>
                    {hasLeak && (
                        <div className="domain-measures-value domain-measures-leak">
                          {measure.leak != null && (
                              <span>
                                {formatLeak(measure.leak, measure.metric)}
                              </span>
                          )}
                        </div>
                    )}
                  </Link>
                </li>
            ))}
          </ul>
        </div>
    );
  }
}

MeasureDetailsSeeAlso.contextTypes = {
  component: React.PropTypes.object,
  metrics: React.PropTypes.array
};

