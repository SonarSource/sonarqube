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
// @flow
import React from 'react';
import { minBy } from 'lodash';
import * as PropTypes from 'prop-types';
import AutoSizer from 'react-virtualized/dist/commonjs/AutoSizer';
import PreviewGraphTooltips from './PreviewGraphTooltips';
import AdvancedTimeline from '../charts/AdvancedTimeline';
import {
  DEFAULT_GRAPH,
  getDisplayedHistoryMetrics,
  generateSeries,
  getSeriesMetricType,
  hasHistoryDataValue,
  splitSeriesInGraphs
} from '../../apps/projectActivity/utils';
import { getCustomGraph, getGraph } from '../../helpers/storage';
import { formatMeasure, getShortType } from '../../helpers/measures';
/*:: import type { Serie } from '../charts/AdvancedTimeline'; */
/*:: import type { History, Metric } from '../../apps/overview/types'; */

/*::
type Props = {
  branch?: string,
  history: ?History,
  metrics: { [string]: Metric },
  project: string,
  renderWhenEmpty?: () => void
};
*/

/*::
type State = {
  customMetrics: Array<string>,
  graph: string,
  selectedDate: ?Date,
  series: Array<Serie>,
  tooltipIdx: ?number,
  tooltipXPos: ?number
};
*/

const GRAPH_PADDING = [4, 0, 4, 0];
const MAX_GRAPH_NB = 1;
const MAX_SERIES_PER_GRAPH = 3;

export default class PreviewGraph extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: State; */

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props /*: Props */) {
    super(props);
    const graph = getGraph();
    const customMetrics = getCustomGraph();
    const series = splitSeriesInGraphs(
      this.getSeries(props.history, graph, customMetrics, props.metrics),
      MAX_GRAPH_NB,
      MAX_SERIES_PER_GRAPH
    );
    this.state = {
      customMetrics,
      graph,
      selectedDate: null,
      series: series.length > 0 ? series[0] : [],
      tooltipIdx: null,
      tooltipXPos: null
    };
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.history !== this.props.history || nextProps.metrics !== this.props.metrics) {
      const graph = getGraph();
      const customMetrics = getCustomGraph();
      const series = splitSeriesInGraphs(
        this.getSeries(nextProps.history, graph, customMetrics, nextProps.metrics),
        MAX_GRAPH_NB,
        MAX_SERIES_PER_GRAPH
      );
      this.setState({
        customMetrics,
        graph,
        series: series.length > 0 ? series[0] : []
      });
    }
  }

  formatValue = (tick /*: number | string */) =>
    formatMeasure(tick, getShortType(getSeriesMetricType(this.state.series)));

  getDisplayedMetrics = (graph /*: string */, customMetrics /*: Array<string> */) => {
    const metrics /*: Array<string> */ = getDisplayedHistoryMetrics(graph, customMetrics);
    if (!metrics || metrics.length <= 0) {
      return getDisplayedHistoryMetrics(DEFAULT_GRAPH, customMetrics);
    }
    return metrics;
  };

  getSeries = (
    history /*: ?History */,
    graph /*: string */,
    customMetrics /*: Array<string> */,
    metrics /*: { [string]: Metric } */
  ) => {
    const myHistory = history;
    if (!myHistory) {
      return [];
    }
    const displayedMetrics = this.getDisplayedMetrics(graph, customMetrics);
    const firstValid = minBy(
      displayedMetrics.map(metric => myHistory[metric].find(p => p.value || p.value === 0)),
      'date'
    );
    const measureHistory = displayedMetrics.map(metric => ({
      metric,
      history: firstValid
        ? myHistory[metric].filter(p => p.date >= firstValid.date)
        : myHistory[metric]
    }));
    return generateSeries(measureHistory, graph, metrics, displayedMetrics);
  };

  handleClick = () => {
    this.context.router.push({
      pathname: '/project/activity',
      query: { id: this.props.project, branch: this.props.branch }
    });
  };

  updateTooltip = (
    selectedDate /*: ?Date */,
    tooltipXPos /*: ?number */,
    tooltipIdx /*: ?number */
  ) => this.setState({ selectedDate, tooltipXPos, tooltipIdx });

  renderTimeline() {
    const { graph, selectedDate, series, tooltipIdx, tooltipXPos } = this.state;
    return (
      <AutoSizer disableHeight={true}>
        {({ width }) => (
          <div>
            <AdvancedTimeline
              endDate={null}
              startDate={null}
              height={80}
              width={width}
              hideGrid={true}
              hideXAxis={true}
              interpolate="linear"
              metricType={getSeriesMetricType(series)}
              padding={GRAPH_PADDING}
              series={series}
              showAreas={['coverage', 'duplications'].includes(graph)}
              updateTooltip={this.updateTooltip}
            />
            {selectedDate != null &&
              tooltipXPos != null &&
              tooltipIdx != null && (
                <PreviewGraphTooltips
                  formatValue={this.formatValue}
                  graph={graph}
                  graphWidth={width}
                  metrics={this.props.metrics}
                  selectedDate={selectedDate}
                  series={series}
                  tooltipIdx={tooltipIdx}
                  tooltipPos={tooltipXPos}
                />
              )}
          </div>
        )}
      </AutoSizer>
    );
  }

  render() {
    const { series } = this.state;
    if (!hasHistoryDataValue(series)) {
      return this.props.renderWhenEmpty ? this.props.renderWhenEmpty() : null;
    }

    return (
      <div
        className="overview-analysis-graph big-spacer-bottom spacer-top"
        onClick={this.handleClick}
        tabIndex={0}
        role="link">
        {this.renderTimeline()}
      </div>
    );
  }
}
