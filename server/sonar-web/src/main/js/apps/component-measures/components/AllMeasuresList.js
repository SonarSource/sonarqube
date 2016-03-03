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
import { formatMeasure } from '../../../helpers/measures';
import { translateWithParameters } from '../../../helpers/l10n';

import { getPeriodLabel } from '../../overview/helpers/periods';

export default class ComponentMeasuresApp extends React.Component {
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
        const measures = r.component.measures
            .map(measure => {
              const metric = metrics.find(metric => metric.key === measure.metric);
              const leak = getLeakValue(measure);
              return { ...measure, metric, leak };
            })
            .filter(measure => measure.value != null || measure.leak != null);

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

    const leakLabel = getPeriodLabel(periods, 1);

    return (
        <ul className="component-measures-domains">
          {domains.map((domain, index) => (
              <li key={domain.name}>
                <header className="page-header">
                  <h3 className="page-title">{domain.name}</h3>
                  {index === 0 && (
                      <div className="component-measures-domains-leak-header">
                        {translateWithParameters('overview.leak_period_x', leakLabel)}
                      </div>
                  )}
                </header>

                <ul className="component-measures-domain-measures">
                  {domain.measures.map(measure => (
                      <li key={measure.metric.key}>
                        <div className="component-measures-domain-measures-name">
                          {measure.metric.name}
                        </div>
                        <div className="component-measures-domain-measures-value">
                          {measure.value != null && (
                              <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                                {formatMeasure(measure.value, measure.metric.type)}
                              </Link>
                          )}
                        </div>
                        <div className="component-measures-domain-measures-value component-measures-leak-cell">
                          {measure.leak != null && (
                              <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                                {formatLeak(measure.leak, measure.metric)}
                              </Link>
                          )}
                        </div>
                      </li>
                  ))}
                </ul>
              </li>
          ))}
        </ul>
    );
  }
}

ComponentMeasuresApp.contextTypes = {
  component: React.PropTypes.object
};
