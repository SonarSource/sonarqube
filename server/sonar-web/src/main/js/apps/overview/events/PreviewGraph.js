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
import { minBy } from 'lodash';
import { AutoSizer } from 'react-virtualized';
import { generateSeries, GRAPHS_METRICS_DISPLAYED } from '../../projectActivity/utils';
import { getGraph } from '../../../helpers/storage';
import AdvancedTimeline from '../../../components/charts/AdvancedTimeline';
import PreviewGraphTooltips from './PreviewGraphTooltips';
import { formatMeasure, getShortType } from '../../../helpers/measures';
import type { Serie } from '../../../components/charts/AdvancedTimeline';
import type { History, Metric } from '../types';

type Props = {
  history: ?History,
  metrics: Array<Metric>,
  project: string,
  router: { replace: ({ pathname: string, query?: {} }) => void }
};

type State = {
  graph: string,
  metricsType: string,
  selectedDate: ?Date,
  series: Array<Serie>,
  tooltipIdx: ?number,
  tooltipXPos: ?number
};

const GRAPH_PADDING = [4, 0, 4, 0];

export default class PreviewGraph extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    const graph = getGraph();
    const metricsType = this.getMetricType(props.metrics, graph);
    this.state = {
      graph,
      metricsType,
      selectedDate: null,
      series: this.getSeries(props.history, graph, metricsType),
      tooltipIdx: null,
      tooltipXPos: null
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.history !== this.props.history || nextProps.metrics !== this.props.metrics) {
      const graph = getGraph();
      const metricsType = this.getMetricType(nextProps.metrics, graph);
      this.setState({
        graph,
        metricsType,
        series: this.getSeries(nextProps.history, graph, metricsType)
      });
    }
  }

  formatValue = (tick: number | string) =>
    formatMeasure(tick, getShortType(this.state.metricsType));

  getDisplayedMetrics = (graph: string): Array<string> => {
    const metrics: Array<string> = GRAPHS_METRICS_DISPLAYED[graph];
    if (!metrics || metrics.length <= 0) {
      return GRAPHS_METRICS_DISPLAYED['overview'];
    }
    return metrics;
  };

  getSeries = (history: ?History, graph: string, metricsType: string) => {
    const myHistory = history;
    if (!myHistory) {
      return [];
    }
    const metrics = this.getDisplayedMetrics(graph);
    const firstValid = minBy(
      metrics.map(metric => myHistory[metric].find(p => p.value || p.value === 0)),
      'date'
    );
    const measureHistory = metrics.map(metric => ({
      metric,
      history: firstValid
        ? myHistory[metric].filter(p => p.date >= firstValid.date)
        : myHistory[metric]
    }));
    return generateSeries(measureHistory, graph, metricsType, metrics);
  };

  getMetricType = (metrics: Array<Metric>, graph: string) => {
    const metricKey = this.getDisplayedMetrics(graph)[0];
    const metric = metrics.find(metric => metric.key === metricKey);
    return metric ? metric.type : 'INT';
  };

  handleClick = () => {
    this.props.router.replace({ pathname: '/project/activity', query: { id: this.props.project } });
  };

  updateTooltip = (selectedDate: ?Date, tooltipXPos: ?number, tooltipIdx: ?number) =>
    this.setState({ selectedDate, tooltipXPos, tooltipIdx });

  render() {
    const { graph, selectedDate, series, tooltipIdx, tooltipXPos } = this.state;
    return (
      <div
        className="overview-analysis-graph big-spacer-bottom spacer-top"
        onClick={this.handleClick}
        tabIndex={0}
        role="link">
        <AutoSizer disableHeight={true}>
          {({ width }) =>
            <div>
              <AdvancedTimeline
                endDate={null}
                startDate={null}
                height={80}
                width={width}
                hideGrid={true}
                hideXAxis={true}
                interpolate="linear"
                metricType={this.state.metricsType}
                padding={GRAPH_PADDING}
                series={series}
                showAreas={['coverage', 'duplications'].includes(graph)}
                updateTooltip={this.updateTooltip}
              />
              {selectedDate != null &&
                tooltipXPos != null &&
                tooltipIdx != null &&
                <PreviewGraphTooltips
                  formatValue={this.formatValue}
                  graph={graph}
                  graphWidth={width}
                  metrics={this.props.metrics}
                  selectedDate={selectedDate}
                  series={series}
                  tooltipIdx={tooltipIdx}
                  tooltipPos={tooltipXPos}
                />}
            </div>}
        </AutoSizer>
      </div>
    );
  }
}
