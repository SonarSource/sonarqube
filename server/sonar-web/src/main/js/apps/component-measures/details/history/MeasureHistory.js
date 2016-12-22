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
import sortBy from 'lodash/sortBy';
import moment from 'moment';
import React from 'react';

import Spinner from './../../components/Spinner';
import Timeline from '../../../../components/charts/Timeline';
import { getTimeMachineData } from '../../../../api/time-machine';
import { getProjectActivity } from '../../../../api/projectActivity';
import { formatMeasure, getShortType } from '../../../../helpers/measures';
import { translate } from '../../../../helpers/l10n';

const HEIGHT = 500;

function parseValue (value, type) {
  return type === 'RATING' && typeof value === 'string' ? value.charCodeAt(0) - 'A'.charCodeAt(0) + 1 : value;
}

export default class MeasureHistory extends React.Component {
  state = {
    components: [],
    selected: null,
    fetching: true
  };

  componentDidMount () {
    this.mounted = true;
    this.fetchHistory();
  }

  componentDidUpdate (nextProps) {
    if (nextProps.metric !== this.props.metric) {
      this.fetchHistory();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchHistory () {
    const { metric } = this.props;

    Promise.all([
      this.fetchTimeMachineData(metric.key),
      this.fetchEvents()
    ]).then(responses => {
      if (this.mounted) {
        this.setState({
          snapshots: responses[0],
          events: responses[1],
          fetching: false
        });
      }
    });
  }

  fetchTimeMachineData (currentMetric, comparisonMetric) {
    const metricsToRequest = [currentMetric];

    if (comparisonMetric) {
      metricsToRequest.push(comparisonMetric);
    }

    return getTimeMachineData(this.props.component.key, metricsToRequest.join()).then(r => {
      return r[0].cells.map(cell => {
        return {
          date: moment(cell.d).toDate(),
          values: cell.v
        };
      });
    });
  }

  fetchEvents () {
    return getProjectActivity(this.props.component.key, { category: 'VERSION' }).then(({ analyses }) => {
      const events = analyses.map(analysis => {
        const version = analysis.events.find(event => event.category === 'VERSION');
        return { version: version.name, date: moment(analysis.date).toDate() };
      });

      return sortBy(events, 'date');
    });
  }

  renderLineChart (snapshots, metric, index) {
    if (!metric) {
      return null;
    }

    if (snapshots.length < 2) {
      return this.renderWhenNoHistoricalData();
    }

    const data = snapshots.map(snapshot => {
      return {
        x: snapshot.date,
        y: parseValue(snapshot.values[index], metric.type)
      };
    });

    const formatValue = (value) => formatMeasure(value, metric.type);
    const formatYTick = (tick) => formatMeasure(tick, getShortType(metric.type));

    return (
        <div style={{ height: HEIGHT }}>
          <Timeline key={metric.key}
                    data={data}
                    metricType={metric.type}
                    events={this.state.events}
                    height={HEIGHT}
                    interpolate="linear"
                    formatValue={formatValue}
                    formatYTick={formatYTick}
                    leakPeriodDate={this.props.leakPeriodDate}
                    padding={[25, 25, 25, 60]}/>
        </div>
    );
  }

  renderLineCharts () {
    const { metric } = this.props;

    return (
        <div>
          {this.renderLineChart(this.state.snapshots, metric, 0)}
          {this.renderLineChart(this.state.snapshots, this.state.comparisonMetric, 1)}
        </div>
    );
  }

  render () {
    const { fetching, snapshots } = this.state;

    if (fetching) {
      return (
          <div className="measure-details-history">
            <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
              <Spinner/>
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
          {this.renderLineCharts()}
        </div>
    );
  }
}
