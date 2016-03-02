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
import { getMeasures } from '../../../api/measures';
import { formatMeasure } from '../../../helpers/measures';

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

    getMeasures(component.key, metricKeys).then(measures => {
      if (this.mounted) {
        const measuresWithMetrics = measures
            .map(measure => {
              const metric = metrics.find(metric => metric.key === measure.metric);
              return { ...measure, metric };
            })
            .filter(measure => measure.value != null);

        this.setState({
          measures: measuresWithMetrics,
          fetching: false
        });
      }
    });
  }

  render () {
    const { fetching, measures } = this.state;

    if (fetching) {
      return <Spinner/>;
    }

    const { component } = this.context;
    const domains = _.sortBy(_.pairs(_.groupBy(measures, measure => measure.metric.domain)).map(r => {
      const [name, measures] = r;
      const sortedMeasures = _.sortBy(measures, measure => measure.metric.name);

      return { name, measures: sortedMeasures };
    }), 'name');

    return (
        <ul className="component-measures-domains">
          {domains.map(domain => (
              <li key={domain.name}>
                <h3 className="component-measures-domain-name">{domain.name}</h3>

                <table className="data zebra">
                  <tbody>
                  {domain.measures.map(measure => (
                      <tr key={measure.metric.key}>
                        <td>
                          {measure.metric.name}
                        </td>
                        <td className="thin nowrap text-right">
                          {measure.value != null && (
                              <div style={{ width: 80 }}>
                                <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                                  {formatMeasure(measure.value, measure.metric.type)}
                                </Link>
                              </div>
                          )}
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </li>
          ))}
        </ul>
    );
  }
}

ComponentMeasuresApp.contextTypes = {
  component: React.PropTypes.object
};
