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
import { EVENT_TYPES } from '../utils';
import { translate } from '../../../helpers/l10n';
import type { Analysis } from '../types';
import type { Serie } from '../../../components/charts/AdvancedTimeline';

type Props = {
  analyses: Array<Analysis>,
  eventFilter: string,
  graphEndDate: ?Date,
  graphStartDate: ?Date,
  leakPeriodDate: Date,
  loading: boolean,
  metricsType: string,
  series: Array<Serie>,
  showAreas?: boolean,
  updateGraphZoom: (from: ?Date, to: ?Date) => void
};

export default class StaticGraphs extends React.PureComponent {
  props: Props;

  formatYTick = tick => formatMeasure(tick, getShortType(this.props.metricsType));

  getEvents = () => {
    const { analyses, eventFilter } = this.props;
    const filteredEvents = analyses.reduce((acc, analysis) => {
      if (analysis.events.length <= 0) {
        return acc;
      }
      let event;
      if (eventFilter) {
        event = analysis.events.filter(event => event.category === eventFilter)[0];
      } else {
        event = sortBy(analysis.events, event => EVENT_TYPES.indexOf(event.category))[0];
      }
      if (!event) {
        return acc;
      }
      return acc.concat({
        className: event.category,
        name: event.name,
        date: moment(analysis.date).toDate()
      });
    }, []);
    return sortBy(filteredEvents, 'date');
  };

  hasSeriesData = () => some(this.props.series, serie => serie.data && serie.data.length > 2);

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

    if (!this.hasSeriesData()) {
      return (
        <div className="project-activity-graph-container">
          <div className="note text-center">
            {translate('component_measures.no_history')}
          </div>
        </div>
      );
    }

    const { series } = this.props;
    return (
      <div className="project-activity-graph-container">
        <StaticGraphsLegend series={series} />
        <div className="project-activity-graph">
          <AutoSizer>
            {({ height, width }) => (
              <AdvancedTimeline
                endDate={this.props.graphEndDate}
                events={this.getEvents()}
                height={height}
                width={width}
                interpolate="linear"
                formatYTick={this.formatYTick}
                leakPeriodDate={this.props.leakPeriodDate}
                metricType={this.props.metricsType}
                series={series}
                showAreas={this.props.showAreas}
                startDate={this.props.graphStartDate}
                updateZoom={this.props.updateGraphZoom}
              />
            )}
          </AutoSizer>
        </div>
      </div>
    );
  }
}
