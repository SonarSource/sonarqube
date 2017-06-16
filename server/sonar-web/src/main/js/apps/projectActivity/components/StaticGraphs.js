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
import { AutoSizer } from 'react-virtualized';
import AdvancedTimeline from '../../../components/charts/AdvancedTimeline';
import StaticGraphsLegend from './StaticGraphsLegend';
import { formatMeasure, getShortType } from '../../../helpers/measures';
import { generateCoveredLinesMetric } from '../utils';
import { translate } from '../../../helpers/l10n';
import type { Analysis, MeasureHistory } from '../types';

type Props = {
  analyses: Array<Analysis>,
  leakPeriodDate: Date,
  loading: boolean,
  measuresHistory: Array<MeasureHistory>,
  metricsType: string
};

export default class StaticGraphs extends React.PureComponent {
  props: Props;

  formatYTick = tick => formatMeasure(tick, getShortType(this.props.metricsType));

  formatValue = value => formatMeasure(value, this.props.metricsType);

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
    sortBy(
      this.props.measuresHistory.map(measure => {
        if (measure.metric === 'uncovered_lines') {
          return generateCoveredLinesMetric(measure, this.props.measuresHistory);
        }
        return {
          name: measure.metric,
          translatedName: translate('metric', measure.metric, 'name'),
          data: measure.history.map(analysis => ({
            x: analysis.date,
            y: this.props.metricsType === 'LEVEL' ? analysis.value : Number(analysis.value)
          }))
        };
      }),
      'name'
    );

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

    const series = this.getSeries();
    return (
      <div className="project-activity-graph-container">
        <StaticGraphsLegend series={series} />
        <div className="project-activity-graph">
          <AutoSizer>
            {({ height, width }) => (
              <AdvancedTimeline
                events={this.getEvents()}
                height={height}
                interpolate="linear"
                formatValue={this.formatValue}
                formatYTick={this.formatYTick}
                leakPeriodDate={this.props.leakPeriodDate}
                metricType={this.props.metricsType}
                padding={[25, 25, 30, 60]}
                series={series}
                showAreas={this.props.showAreas}
                width={width}
              />
            )}
          </AutoSizer>
        </div>
      </div>
    );
  }
}
