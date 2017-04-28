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
import { sortBy } from 'lodash';
import moment from 'moment';
import React from 'react';
import Spinner from './../../components/Spinner';
import Timeline from '../../../../components/charts/Timeline';
import { getTimeMachineData } from '../../../../api/time-machine';
import { getProjectActivity } from '../../../../api/projectActivity';
import { formatMeasure, getShortType } from '../../../../helpers/measures';
import { translate } from '../../../../helpers/l10n';

const HEIGHT = 500;

export default class MeasureHistory extends React.PureComponent {
  state = {
    components: [],
    selected: null,
    fetching: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchHistory();
  }

  componentDidUpdate(nextProps) {
    if (nextProps.metric !== this.props.metric) {
      this.fetchHistory();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchHistory() {
    const { metric } = this.props;

    Promise.all([this.fetchTimeMachineData(metric.key), this.fetchEvents()]).then(responses => {
      if (this.mounted) {
        this.setState({
          snapshots: responses[0],
          events: responses[1],
          fetching: false
        });
      }
    });
  }

  fetchTimeMachineData(currentMetric, comparisonMetric) {
    const metricsToRequest = [currentMetric];

    if (comparisonMetric) {
      metricsToRequest.push(comparisonMetric);
    }

    return getTimeMachineData(this.props.component.key, metricsToRequest).then(r => {
      if (r.measures.length === 0) {
        return [];
      }
      return r.measures[0].history.map(analysis => ({
        date: moment(analysis.date).toDate(),
        value: analysis.value
      }));
    });
  }

  fetchEvents() {
    if (this.props.component.qualifier !== 'TRK') {
      return Promise.resolve([]);
    }

    return getProjectActivity(this.props.component.key, {
      category: 'VERSION'
    }).then(({ analyses }) => {
      const events = analyses.map(analysis => {
        const version = analysis.events.find(event => event.category === 'VERSION');
        return { version: version.name, date: moment(analysis.date).toDate() };
      });

      return sortBy(events, 'date');
    });
  }

  renderLineChart(snapshots, metric) {
    if (!metric) {
      return null;
    }

    if (snapshots.length < 2) {
      return this.renderWhenNoHistoricalData();
    }

    const data = snapshots.map(snapshot => {
      return {
        x: snapshot.date,
        y: metric.type === 'LEVEL' ? snapshot.value : Number(snapshot.value)
      };
    });

    const formatValue = value => formatMeasure(value, metric.type);
    const formatYTick = tick => formatMeasure(tick, getShortType(metric.type));

    return (
      <div style={{ height: HEIGHT }}>
        <Timeline
          basisCurve={false}
          key={metric.key}
          data={data}
          metricType={metric.type}
          events={this.state.events}
          height={HEIGHT}
          interpolate="linear"
          formatValue={formatValue}
          formatYTick={formatYTick}
          leakPeriodDate={this.props.leakPeriodDate}
          padding={[25, 25, 25, 60]}
        />
      </div>
    );
  }

  render() {
    const { fetching, snapshots } = this.state;

    if (fetching) {
      return (
        <div className="measure-details-history">
          <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
            <Spinner />
          </div>
        </div>
      );
    }

    if (!snapshots || snapshots.length < 2) {
      return (
        <div className="measure-details-history">
          <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
            {translate('component_measures.no_history')}
          </div>
        </div>
      );
    }

    return (
      <div className="measure-details-history">
        {this.renderLineChart(this.state.snapshots, this.props.metric)}
      </div>
    );
  }
}
