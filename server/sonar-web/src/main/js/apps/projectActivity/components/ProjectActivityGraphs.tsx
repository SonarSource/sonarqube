/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { FlagMessage } from 'design-system';
import { debounce, findLast, maxBy, minBy, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import GraphsHeader from '../../../components/activity-graph/GraphsHeader';
import GraphsHistory from '../../../components/activity-graph/GraphsHistory';
import GraphsZoom from '../../../components/activity-graph/GraphsZoom';
import {
  generateSeries,
  getActivityGraph,
  getDisplayedHistoryMetrics,
  getSeriesMetricType,
  isCustomGraph,
  saveActivityGraph,
  splitSeriesInGraphs,
} from '../../../components/activity-graph/utils';
import DocumentationLink from '../../../components/common/DocumentationLink';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  SOFTWARE_QUALITY_RATING_METRICS_MAP,
} from '../../../helpers/constants';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import {
  GraphType,
  MeasureHistory,
  ParsedAnalysis,
  Point,
  Serie,
} from '../../../types/project-activity';
import { Metric } from '../../../types/types';
import { Query, datesQueryChanged, historyQueryChanged } from '../utils';
import { PROJECT_ACTIVITY_GRAPH } from './ProjectActivityApp';

interface Props {
  analyses: ParsedAnalysis[];
  isLegacy?: boolean;
  leakPeriodDate?: Date;
  loading: boolean;
  measuresHistory: MeasureHistory[];
  metrics: Metric[];
  project: string;
  query: Query;
  updateQuery: (changes: Partial<Query>) => void;
}

interface State {
  graphEndDate?: Date;
  graphStartDate?: Date;
  graphs: Serie[][];
  series: Serie[];
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
      getDisplayedHistoryMetrics(props.query.graph, props.query.customMetrics),
    );
    this.state = {
      series,
      graphs: splitSeriesInGraphs(series, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH),
      ...this.getStateZoomDates(undefined, props, series),
    };
    this.updateQueryDateRange = debounce(this.updateQueryDateRange, 500);
  }

  componentDidUpdate(prevProps: Props) {
    let newSeries;
    let newGraphs;
    if (
      prevProps.measuresHistory !== this.props.measuresHistory ||
      historyQueryChanged(prevProps.query, this.props.query)
    ) {
      newSeries = generateSeries(
        this.props.measuresHistory,
        this.props.query.graph,
        this.props.metrics,
        getDisplayedHistoryMetrics(this.props.query.graph, this.props.query.customMetrics),
      );
      newGraphs = splitSeriesInGraphs(newSeries, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH);
    }

    const newDates = this.getStateZoomDates(prevProps, this.props, newSeries);

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

  getStateZoomDates = (prevProps: Props | undefined, props: Props, newSeries?: Serie[]) => {
    const newDates = {
      from: props.query.from || undefined,
      to: props.query.to || undefined,
    };
    if (!prevProps || datesQueryChanged(prevProps.query, props.query)) {
      return { graphEndDate: newDates.to, graphStartDate: newDates.from };
    }

    if (newDates.to === undefined && newDates.from === undefined && newSeries !== undefined) {
      const firstValid = minBy(
        newSeries.map((serie) => serie.data.find((p) => Boolean(p.y || p.y === 0))),
        'x',
      );
      const lastValid = maxBy<Point>(
        newSeries.map((serie) => findLast(serie.data, (p) => Boolean(p.y || p.y === 0))!),
        'x',
      );
      return {
        graphEndDate: lastValid?.x,
        graphStartDate: firstValid?.x,
      };
    }
    return null;
  };

  getMetricsTypeFilter = () => {
    if (this.state.graphs.length < MAX_GRAPH_NB) {
      return undefined;
    }
    return this.state.graphs
      .filter((graph) => graph.length < MAX_SERIES_PER_GRAPH)
      .map((graph) => graph[0].type);
  };

  handleAddCustomMetric = (metric: string) => {
    const customMetrics = [...this.props.query.customMetrics, metric];
    saveActivityGraph(PROJECT_ACTIVITY_GRAPH, this.props.project, GraphType.custom, customMetrics);
    this.props.updateQuery({ customMetrics });
  };

  handleRemoveCustomMetric = (removedMetric: string) => {
    const customMetrics = this.props.query.customMetrics.filter(
      (metric) => metric !== removedMetric,
    );
    saveActivityGraph(PROJECT_ACTIVITY_GRAPH, this.props.project, GraphType.custom, customMetrics);
    this.props.updateQuery({ customMetrics });
  };

  handleUpdateGraph = (graph: GraphType) => {
    saveActivityGraph(PROJECT_ACTIVITY_GRAPH, this.props.project, graph);
    if (isCustomGraph(graph) && this.props.query.customMetrics.length <= 0) {
      const { customGraphs } = getActivityGraph(PROJECT_ACTIVITY_GRAPH, this.props.project);
      this.props.updateQuery({ graph, customMetrics: customGraphs });
    } else {
      this.props.updateQuery({ graph, customMetrics: [] });
    }
  };

  handleUpdateGraphZoom = (graphStartDate?: Date, graphEndDate?: Date) => {
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

  handleUpdateSelectedDate = (selectedDate?: Date) => {
    this.props.updateQuery({ selectedDate });
  };

  updateQueryDateRange = (dates: Array<Date | undefined>) => {
    if (dates[0] === undefined || dates[1] === undefined) {
      this.props.updateQuery({ from: dates[0], to: dates[1] });
    } else {
      const sortedDates = sortBy(dates);
      this.props.updateQuery({ from: sortedDates[0], to: sortedDates[1] });
    }
  };

  hasGaps = (value?: MeasureHistory) => {
    const indexOfFirstMeasureWithValue = value?.history.findIndex((item) => item.value);

    return indexOfFirstMeasureWithValue === -1
      ? false
      : value?.history.slice(indexOfFirstMeasureWithValue).some((item) => item.value === undefined);
  };

  renderQualitiesMetricInfoMessage = () => {
    const { measuresHistory, isLegacy } = this.props;

    const qualityMeasuresHistory = measuresHistory.find((history) =>
      CCT_SOFTWARE_QUALITY_METRICS.includes(history.metric),
    );
    const ratingQualityMeasuresHistory = measuresHistory.find((history) =>
      (Object.keys(SOFTWARE_QUALITY_RATING_METRICS_MAP) as MetricKey[]).includes(history.metric),
    );

    if (
      this.hasGaps(qualityMeasuresHistory) ||
      (!isLegacy && this.hasGaps(ratingQualityMeasuresHistory))
    ) {
      return (
        <FlagMessage variant="info">
          <FormattedMessage
            id="project_activity.graphs.data_table.data_gap"
            tagName="div"
            values={{
              learn_more: (
                <DocumentationLink className="sw-whitespace-nowrap" to={DocLink.CodeAnalysis}>
                  {translate('learn_more')}
                </DocumentationLink>
              ),
            }}
          />
        </FlagMessage>
      );
    }

    return null;
  };

  render() {
    const { analyses, leakPeriodDate, loading, measuresHistory, metrics, query } = this.props;
    const { graphEndDate, graphStartDate, series } = this.state;

    return (
      <div className="sw-px-5 sw-py-4 sw-h-full sw-flex sw-flex-col sw-box-border">
        <GraphsHeader
          onAddCustomMetric={this.handleAddCustomMetric}
          className="sw-mb-4"
          graph={query.graph}
          metrics={metrics}
          metricsTypeFilter={this.getMetricsTypeFilter()}
          onRemoveCustomMetric={this.handleRemoveCustomMetric}
          selectedMetrics={query.customMetrics}
          onUpdateGraph={this.handleUpdateGraph}
        />
        {this.renderQualitiesMetricInfoMessage()}
        <GraphsHistory
          analyses={analyses}
          graph={query.graph}
          graphEndDate={graphEndDate}
          graphStartDate={graphStartDate}
          graphs={this.state.graphs}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          measuresHistory={measuresHistory}
          removeCustomMetric={this.handleRemoveCustomMetric}
          selectedDate={query.selectedDate}
          series={series}
          updateGraphZoom={this.handleUpdateGraphZoom}
          updateSelectedDate={this.handleUpdateSelectedDate}
        />
        <GraphsZoom
          graphEndDate={graphEndDate}
          graphStartDate={graphStartDate}
          leakPeriodDate={leakPeriodDate}
          loading={loading}
          metricsType={getSeriesMetricType(series)}
          series={series}
          showAreas={[GraphType.coverage, GraphType.duplications].includes(query.graph)}
          onUpdateGraphZoom={this.handleUpdateGraphZoom}
        />
      </div>
    );
  }
}
