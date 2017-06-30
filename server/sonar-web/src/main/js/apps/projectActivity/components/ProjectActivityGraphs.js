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
// @flow
import React from 'react';
import { debounce, findLast, maxBy, minBy, sortBy } from 'lodash';
import ProjectActivityGraphsHeader from './ProjectActivityGraphsHeader';
import GraphsZoom from './GraphsZoom';
import StaticGraphs from './StaticGraphs';
import { datesQueryChanged, generateSeries, historyQueryChanged } from '../utils';
import type { RawQuery } from '../../../helpers/query';
import type { Analysis, MeasureHistory, Query } from '../types';
import type { Serie } from '../../../components/charts/AdvancedTimeline';

type Props = {
  analyses: Array<Analysis>,
  leakPeriodDate: Date,
  loading: boolean,
  measuresHistory: Array<MeasureHistory>,
  metricsType: string,
  project: string,
  query: Query,
  updateQuery: RawQuery => void
};

type State = {
  graphStartDate: ?Date,
  graphEndDate: ?Date,
  series: Array<Serie>
};

export default class ProjectActivityGraphs extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    const series = generateSeries(props.measuresHistory, props.query.graph, props.metricsType);
    this.state = { series, ...this.getStateZoomDates(null, props, series) };
    this.updateQueryDateRange = debounce(this.updateQueryDateRange, 500);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.measuresHistory !== this.props.measuresHistory ||
      historyQueryChanged(this.props.query, nextProps.query)
    ) {
      const series = generateSeries(
        nextProps.measuresHistory,
        nextProps.query.graph,
        nextProps.metricsType
      );

      const newDates = this.getStateZoomDates(this.props, nextProps, series);
      if (newDates) {
        this.setState({ series, ...newDates });
      } else {
        this.setState({ series });
      }
    }
  }

  getStateZoomDates = (props: ?Props, nextProps: Props, series: Array<Serie>) => {
    const newDates = { from: nextProps.query.from || null, to: nextProps.query.to || null };
    if (props && datesQueryChanged(props.query, nextProps.query)) {
      return { graphEndDate: newDates.to, graphStartDate: newDates.from };
    }
    if (newDates.to == null && newDates.from == null) {
      const firstValid = minBy(series.map(serie => serie.data.find(p => p.y || p.y === 0)), 'x');
      const lastValid = maxBy(
        series.map(serie => findLast(serie.data, p => p.y || p.y === 0)),
        'x'
      );
      return {
        graphEndDate: lastValid ? lastValid.x : newDates.to,
        graphStartDate: firstValid ? firstValid.x : newDates.from
      };
    }
    if (!props) {
      return { graphEndDate: newDates.to, graphStartDate: newDates.from };
    }
  };

  updateGraphZoom = (graphStartDate: ?Date, graphEndDate: ?Date) => {
    if (graphEndDate != null && graphStartDate != null) {
      const msDiff = Math.abs(graphEndDate.valueOf() - graphStartDate.valueOf());
      // 12 hours minimum between the two dates
      if (msDiff < 1000 * 60 * 60 * 12) {
        return;
      }
    }

    this.setState({ graphStartDate, graphEndDate });
    this.updateQueryDateRange([graphStartDate, graphEndDate]);
  };

  updateQueryDateRange = (dates: Array<?Date>) => {
    if (dates[0] == null || dates[1] == null) {
      this.props.updateQuery({ from: dates[0], to: dates[1] });
    } else {
      const sortedDates = sortBy(dates);
      this.props.updateQuery({ from: sortedDates[0], to: sortedDates[1] });
    }
  };

  render() {
    const { leakPeriodDate, loading, metricsType, query } = this.props;
    const { series } = this.state;
    return (
      <div className="project-activity-layout-page-main-inner boxed-group boxed-group-inner">
        <ProjectActivityGraphsHeader graph={query.graph} updateQuery={this.props.updateQuery} />
        <StaticGraphs
          analyses={this.props.analyses}
          eventFilter={query.category}
          graphEndDate={this.state.graphEndDate}
          graphStartDate={this.state.graphStartDate}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          metricsType={metricsType}
          project={this.props.project}
          series={series}
          showAreas={['coverage', 'duplications'].includes(query.graph)}
          updateGraphZoom={this.updateGraphZoom}
        />
        <GraphsZoom
          graphEndDate={this.state.graphEndDate}
          graphStartDate={this.state.graphStartDate}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          metricsType={metricsType}
          series={series}
          showAreas={['coverage', 'duplications'].includes(query.graph)}
          updateGraphZoom={this.updateGraphZoom}
        />
      </div>
    );
  }
}
