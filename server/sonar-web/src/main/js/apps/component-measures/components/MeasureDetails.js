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
import MeasureDrilldown from './MeasureDrilldown';
import { getMeasures } from '../../../api/measures';
import { formatMeasure } from '../../../helpers/measures';

export default class MeasureDetails extends React.Component {
  state = {};

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

    getMeasures(component.key, [metricKey]).then(measures => {
      if (this.mounted && measures.length === 1) {
        this.setState({ measure: measures[0] });
      }
    });
  }

  render () {
    const { metricKey, tab } = this.props.params;
    const { metrics, children } = this.props;
    const { measure } = this.state;
    const metric = metrics.find(metric => metric.key === metricKey);
    const finalTab = tab || 'tree';

    if (!measure) {
      return <Spinner/>;
    }

    return (
        <div className="measure-details">
          <h2 className="measure-details-metric">
            {metric.name}
          </h2>

          {measure && (
              <div className="measure-details-value">
                {measure.value != null && (
                    formatMeasure(measure.value, metric.type)
                )}
              </div>
          )}

          {measure && (
              <MeasureDrilldown metric={metric} tab={finalTab}>
                {children}
              </MeasureDrilldown>
          )}
        </div>
    );
  }
}

MeasureDetails.contextTypes = {
  component: React.PropTypes.object
};
