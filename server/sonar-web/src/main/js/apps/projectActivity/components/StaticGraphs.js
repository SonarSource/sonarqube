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
import moment from 'moment';
import { some, sortBy } from 'lodash';
import AdvancedTimeline from '../../../components/charts/AdvancedTimeline';
import StaticGraphsLegend from './StaticGraphsLegend';
import ResizeHelper from '../../../components/common/ResizeHelper';
import { formatMeasure, getShortType } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import type { Analysis, MeasureHistory } from '../types';

type Props = {
  analyses: Array<Analysis>,
  loading: boolean,
  measuresHistory: Array<MeasureHistory>,
  metricsType: string
};

export default class StaticGraphs extends React.PureComponent {
  props: Props;

  getEvents = () => {
    const events = this.props.analyses.reduce((acc, analysis) => {
      return acc.concat(
        analysis.events.map(event => ({
          className: event.category,
          name: event.name,
          date: moment(analysis.date).toDate()
        }))
      );
    }, []);
    return sortBy(events, 'date');
  };

  getSeries = () =>
    sortBy(this.props.measuresHistory, 'metric').map(measure => ({
      name: measure.metric,
      data: measure.history.map(analysis => ({
        x: analysis.date,
        y: this.props.metricsType === 'LEVEL' ? analysis.value : Number(analysis.value)
      }))
    }));

  hasHistoryData = () =>
    some(this.props.measuresHistory, measure => measure.history && measure.history.length > 2);

  render() {
    const { loading } = this.props;

    if (loading) {
      return (
        <div className="project-activity-graph-container">
          <div className="text-center">
            <i className="spinner" />
          </div>
        </div>
      );
    }

    if (!this.hasHistoryData()) {
      return (
        <div className="project-activity-graph-container">
          <div className="note text-center">
            {translate('component_measures.no_history')}
          </div>
        </div>
      );
    }

    const { metricsType } = this.props;
    const formatValue = value => formatMeasure(value, metricsType);
    const formatYTick = tick => formatMeasure(tick, getShortType(metricsType));
    const series = this.getSeries();
    return (
      <div className="project-activity-graph-container">
        <StaticGraphsLegend series={series} />
        <div className="project-activity-graph">
          <ResizeHelper>
            <AdvancedTimeline
              basisCurve={false}
              series={series}
              metricType={metricsType}
              events={this.getEvents()}
              interpolate="linear"
              formatValue={formatValue}
              formatYTick={formatYTick}
              leakPeriodDate={this.props.leakPeriodDate}
              padding={[25, 25, 30, 60]}
            />
          </ResizeHelper>
        </div>
      </div>
    );
  }
}
