/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { debounce, findLast, maxBy, minBy, sortBy } from 'lodash';
import ProjectActivityGraphsHeader from './ProjectActivityGraphsHeader';
import GraphsZoom from './GraphsZoom';
import GraphsHistory from './GraphsHistory';
import { get, save } from '../../../helpers/storage';
import {
  datesQueryChanged,
  generateSeries,
  getDisplayedHistoryMetrics,
  getSeriesMetricType,
  historyQueryChanged,
  isCustomGraph,
  PROJECT_ACTIVITY_GRAPH,
  PROJECT_ACTIVITY_GRAPH_CUSTOM,
  splitSeriesInGraphs,
  MeasureHistory,
  Query,
  Serie,
  Point,
  ParsedAnalysis
} from '../utils';
import { Metric } from '../../../app/types';

interface Props {
  analyses: ParsedAnalysis[];
  leakPeriodDate?: Date;
  loading: boolean;
  measuresHistory: MeasureHistory[];
  metrics: Metric[];
  query: Query;
  updateQuery: (changes: Partial<Query>) => void;
}

interface State {
  graphStartDate?: Date;
  graphEndDate?: Date;
  series: Serie[];
  graphs: Serie[][];
}

const MAX_GRAPH_NB = 2;
const MAX_SERIES_PER_GRAPH = 3;

export default class ProjectActivityGraphs extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const series = generateSeries(
      props.measuresHistory,
      props.query.graph,
      props.metrics,
      getDisplayedHistoryMetrics(props.query.graph, props.query.customMetrics)
    );
    this.state = {
      series,
      graphs: splitSeriesInGraphs(series, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH),
      ...this.getStateZoomDates(undefined, props, series)
    };
    this.updateQueryDateRange = debounce(this.updateQueryDateRange, 500);
  }

  componentWillReceiveProps(nextProps: Props) {
    let newSeries;
    let newGraphs;
    if (
      nextProps.measuresHistory !== this.props.measuresHistory ||
      historyQueryChanged(this.props.query, nextProps.query)
    ) {
      newSeries = generateSeries(
        nextProps.measuresHistory,
        nextProps.query.graph,
        nextProps.metrics,
        getDisplayedHistoryMetrics(nextProps.query.graph, nextProps.query.customMetrics)
      );
      newGraphs = splitSeriesInGraphs(newSeries, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH);
    }

    const newDates = this.getStateZoomDates(this.props, nextProps, newSeries);

    if (newSeries || newDates) {
      let newState = {} as State;
      if (newSeries) {
        newState.series = newSeries;
      }
      if (newGraphs) {
        newState.graphs = newGraphs;
      }
      if (newDates) {
        newState = { ...newState, ...newDates };
      }
      this.setState(newState);
    }
  }

  getStateZoomDates = (props: Props | undefined, nextProps: Props, newSeries?: Serie[]) => {
    const newDates = {
      from: nextProps.query.from || undefined,
      to: nextProps.query.to || undefined
    };
    if (!props || datesQueryChanged(props.query, nextProps.query)) {
      return { graphEndDate: newDates.to, graphStartDate: newDates.from };
    }

    if (newDates.to === undefined && newDates.from === undefined && newSeries !== undefined) {
      const series = newSeries ? newSeries : this.state.series;
      const firstValid = minBy(
        series.map(serie => serie.data.find(p => Boolean(p.y || p.y === 0))),
        'x'
      );
      const lastValid = maxBy<Point>(
        series.map(serie => findLast(serie.data, p => Boolean(p.y || p.y === 0))!),
        'x'
      );
      return {
        graphEndDate: lastValid ? lastValid.x : newDates.to,
        graphStartDate: firstValid ? firstValid.x : newDates.from
      };
    }
    return null;
  };

  getMetricsTypeFilter = () => {
    if (this.state.graphs.length < MAX_GRAPH_NB) {
      return undefined;
    }
    return this.state.graphs
      .filter(graph => graph.length < MAX_SERIES_PER_GRAPH)
      .map(graph => graph[0].type);
  };

  addCustomMetric = (metric: string) => {
    const customMetrics = [...this.props.query.customMetrics, metric];
    save(PROJECT_ACTIVITY_GRAPH_CUSTOM, customMetrics.join(','));
    this.props.updateQuery({ customMetrics });
  };

  removeCustomMetric = (removedMetric: string) => {
    const customMetrics = this.props.query.customMetrics.filter(metric => metric !== removedMetric);
    save(PROJECT_ACTIVITY_GRAPH_CUSTOM, customMetrics.join(','));
    this.props.updateQuery({ customMetrics });
  };

  updateGraph = (graph: string) => {
    save(PROJECT_ACTIVITY_GRAPH, graph);
    if (isCustomGraph(graph) && this.props.query.customMetrics.length <= 0) {
      const customGraphs = get(PROJECT_ACTIVITY_GRAPH_CUSTOM);
      this.props.updateQuery({ graph, customMetrics: customGraphs ? customGraphs.split(',') : [] });
    } else {
      this.props.updateQuery({ graph, customMetrics: [] });
    }
  };

  updateGraphZoom = (graphStartDate?: Date, graphEndDate?: Date) => {
    if (graphEndDate !== undefined && graphStartDate !== undefined) {
      const msDiff = Math.abs(graphEndDate.valueOf() - graphStartDate.valueOf());
      // 12 hours minimum between the two dates
      if (msDiff < 1000 * 60 * 60 * 12) {
        return;
      }
    }

    this.setState({ graphStartDate, graphEndDate });
    this.updateQueryDateRange([graphStartDate, graphEndDate]);
  };

  updateSelectedDate = (selectedDate?: Date) => this.props.updateQuery({ selectedDate });

  updateQueryDateRange = (dates: Array<Date | undefined>) => {
    if (dates[0] === undefined || dates[1] === undefined) {
      this.props.updateQuery({ from: dates[0], to: dates[1] });
    } else {
      const sortedDates = sortBy(dates);
      this.props.updateQuery({ from: sortedDates[0], to: sortedDates[1] });
    }
  };

  render() {
    const { leakPeriodDate, loading, metrics, query } = this.props;
    const { graphEndDate, graphStartDate, series } = this.state;

    return (
      <div className="project-activity-layout-page-main-inner boxed-group boxed-group-inner">
        <ProjectActivityGraphsHeader
          addCustomMetric={this.addCustomMetric}
          graph={query.graph}
          metrics={metrics}
          metricsTypeFilter={this.getMetricsTypeFilter()}
          removeCustomMetric={this.removeCustomMetric}
          selectedMetrics={this.props.query.customMetrics}
          updateGraph={this.updateGraph}
        />
        <GraphsHistory
          analyses={this.props.analyses}
          eventFilter={query.category}
          graph={query.graph}
          graphEndDate={graphEndDate}
          graphStartDate={graphStartDate}
          graphs={this.state.graphs}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          measuresHistory={this.props.measuresHistory}
          removeCustomMetric={this.removeCustomMetric}
          selectedDate={this.props.query.selectedDate}
          series={series}
          updateGraphZoom={this.updateGraphZoom}
          updateSelectedDate={this.updateSelectedDate}
        />
        <GraphsZoom
          graphEndDate={graphEndDate}
          graphStartDate={graphStartDate}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          metricsType={getSeriesMetricType(series)}
          series={series}
          showAreas={['coverage', 'duplications'].includes(query.graph)}
          updateGraphZoom={this.updateGraphZoom}
        />
      </div>
    );
  }
}
