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
import { IndexLink } from 'react-router';

import Spinner from './../components/Spinner';
import MeasureDetailsHeader from './MeasureDetailsHeader';
import MeasureDrilldown from './drilldown/MeasureDrilldown';

import { getLeakPeriod, getPeriodDate, getPeriodLabel } from '../../../helpers/periods';
import { translate } from '../../../helpers/l10n';

export default class MeasureDetails extends React.Component {
  componentWillMount () {
    const { metrics } = this.props;
    const { metricKey } = this.props.params;
    const metric = metrics.find(metric => metric.key === metricKey);

    if (!metric) {
      const { component } = this.props;
      const { router } = this.context;

      router.replace({
        pathname: '/',
        query: { id: component.key }
      });
    }
  }

  componentDidMount () {
    this.props.fetchMeasure(this.props.params.metricKey);
  }

  componentDidUpdate (nextProps) {
    if (nextProps.params.metricKey !== this.props.params.metricKey) {
      this.props.fetchMeasure(nextProps.params.metricKey);
    }
  }

  render () {
    const { component, metric, secondaryMeasure, measure, periods, children } = this.props;

    if (measure == null) {
      return <Spinner/>;
    }

    const { tab } = this.props.params;
    const leakPeriod = getLeakPeriod(periods);
    const leakPeriodLabel = getPeriodLabel(leakPeriod);
    const leakPeriodDate = getPeriodDate(leakPeriod);

    return (
        <section id="component-measures-details" className="page page-container page-limited">
          <IndexLink
              to={{ pathname: '/', query: { id: component.key } }}
              id="component-measures-back-to-all-measures"
              className="small text-muted">
            {translate('component_measures.back_to_all_measures')}
          </IndexLink>

          <MeasureDetailsHeader
              measure={measure}
              metric={metric}
              secondaryMeasure={secondaryMeasure}
              leakPeriodLabel={leakPeriodLabel}/>

          {measure && (
              <MeasureDrilldown
                  component={component}
                  metric={metric}
                  tab={tab}
                  leakPeriod={leakPeriod}
                  leakPeriodDate={leakPeriodDate}>
                {children}
              </MeasureDrilldown>
          )}
        </section>
    );
  }
}

MeasureDetails.contextTypes = {
  router: React.PropTypes.object
};
