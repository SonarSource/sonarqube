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
import React from 'react';
import { Link, IndexLink } from 'react-router';
import Spinner from './../components/Spinner';
import MeasureDetailsHeader from './MeasureDetailsHeader';
import MeasureDrilldown from './drilldown/MeasureDrilldown';
import MetricNotFound from './MetricNotFound';
import { getPeriod, getPeriodDate } from '../../../helpers/periods';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default class MeasureDetails extends React.Component {
  mounted: boolean;

  state = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.params.metricKey !== this.props.params.metricKey) {
      this.loadData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  metricExists(): boolean {
    const { metrics } = this.props;
    const { metricKey } = this.props.params;
    const metric = metrics.find(metric => metric.key === metricKey);
    return !!metric;
  }

  loadData() {
    if (this.metricExists()) {
      this.setState({ loading: true });
      const periodIndex = this.props.location.query.period || 1;
      const onLoaded = () => this.mounted && this.setState({ loading: false });
      this.props
        .fetchMeasure(this.props.params.metricKey, Number(periodIndex))
        .then(onLoaded, onLoaded);
    }
  }

  render() {
    if (!this.metricExists()) {
      return <MetricNotFound />;
    }

    if (this.state.loading) {
      return <Spinner />;
    }

    const { component, metric, secondaryMeasure, measure, periods, children } = this.props;

    if (!measure) {
      return <MetricNotFound />;
    }

    const { tab } = this.props.params;
    const periodIndex = this.props.location.query.period || 1;
    const period = getPeriod(periods, Number(periodIndex));
    const periodDate = getPeriodDate(period);

    return (
      <section id="component-measures-details" className="page page-container page-limited">
        <div className="note">
          <IndexLink
            to={{ pathname: '/component_measures', query: { id: component.key } }}
            id="component-measures-back-to-all-measures"
            className="text-muted">
            {translate('component_measures.all_measures')}
          </IndexLink>
          {!!metric.domain &&
            <span>
              {' / '}
              <Link
                to={{
                  pathname: `/component_measures/domain/${metric.domain}`,
                  query: { id: component.key }
                }}
                className="text-muted">
                {translateWithParameters('component_measures.domain_measures', metric.domain)}
              </Link>
            </span>}
        </div>

        <MeasureDetailsHeader
          measure={measure}
          metric={metric}
          secondaryMeasure={secondaryMeasure}
          leakPeriod={period}
        />

        {measure &&
          <MeasureDrilldown
            component={component}
            metric={metric}
            tab={tab}
            leakPeriod={period}
            leakPeriodDate={periodDate}>
            {children}
          </MeasureDrilldown>}
      </section>
    );
  }
}
