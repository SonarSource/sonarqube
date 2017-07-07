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
import GraphsHistory from './GraphsHistory';
import { getCustomGraph, saveCustomGraph, saveGraph } from '../../../helpers/storage';
import {
  datesQueryChanged,
  isCustomGraph,
  generateSeries,
  getDisplayedHistoryMetrics,
  historyQueryChanged
} from '../utils';
import type { RawQuery } from '../../../helpers/query';
import type { Analysis, MeasureHistory, Metric, Query } from '../types';
import type { Serie } from '../../../components/charts/AdvancedTimeline';

type Props = {
  analyses: Array<Analysis>,
  leakPeriodDate: Date,
  loading: boolean,
  measuresHistory: Array<MeasureHistory>,
  metrics: Array<Metric>,
  metricsType: string,
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
    const series = generateSeries(
      props.measuresHistory,
      props.query.graph,
      props.metricsType,
      getDisplayedHistoryMetrics(props.query.graph, props.query.customMetrics)
    );
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
        nextProps.metricsType,
        getDisplayedHistoryMetrics(nextProps.query.graph, nextProps.query.customMetrics)
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

  addCustomMetric = (metric: string) => {
    const customMetrics = [...this.props.query.customMetrics, metric];
    saveCustomGraph(customMetrics);
    this.props.updateQuery({ customMetrics });
  };

  removeCustomMetric = (removedMetric: string) => {
    const customMetrics = this.props.query.customMetrics.filter(metric => metric !== removedMetric);
    saveCustomGraph(customMetrics);
    this.props.updateQuery({ customMetrics });
  };

  updateGraph = (graph: string) => {
    saveGraph(graph);
    if (isCustomGraph(graph) && this.props.query.customMetrics.length <= 0) {
      this.props.updateQuery({ graph, customMetrics: getCustomGraph() });
    } else {
      this.props.updateQuery({ graph, customMetrics: [] });
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

  updateSelectedDate = (selectedDate: ?Date) => this.props.updateQuery({ selectedDate });

  updateQueryDateRange = (dates: Array<?Date>) => {
    if (dates[0] == null || dates[1] == null) {
      this.props.updateQuery({ from: dates[0], to: dates[1] });
    } else {
      const sortedDates = sortBy(dates);
      this.props.updateQuery({ from: sortedDates[0], to: sortedDates[1] });
    }
  };

  render() {
    const { leakPeriodDate, loading, metrics, metricsType, query } = this.props;
    const { series } = this.state;
    return (
      <div className="project-activity-layout-page-main-inner boxed-group boxed-group-inner">
        <ProjectActivityGraphsHeader
          addCustomMetric={this.addCustomMetric}
          graph={query.graph}
          metrics={metrics}
          selectedMetrics={this.props.query.customMetrics}
          updateGraph={this.updateGraph}
        />
        <GraphsHistory
          analyses={this.props.analyses}
          eventFilter={query.category}
          graph={query.graph}
          graphEndDate={this.state.graphEndDate}
          graphStartDate={this.state.graphStartDate}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          measuresHistory={this.props.measuresHistory}
          metrics={metrics}
          metricsType={metricsType}
          removeCustomMetric={this.removeCustomMetric}
          selectedDate={this.props.query.selectedDate}
          series={series}
          updateGraphZoom={this.updateGraphZoom}
          updateSelectedDate={this.updateSelectedDate}
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
