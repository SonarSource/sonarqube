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
import { map } from 'lodash';
import { Link } from 'react-router';
import { AutoSizer } from 'react-virtualized';
import { generateSeries, GRAPHS_METRICS } from '../../projectActivity/utils';
import { getGraph } from '../../../helpers/storage';
import AdvancedTimeline from '../../../components/charts/AdvancedTimeline';
import type { Serie } from '../../../components/charts/AdvancedTimeline';
import type { History, Metric } from '../types';

type Props = {
  history: History,
  metrics: Array<Metric>,
  project: string
};

type State = {
  graph: string,
  metricsType: string,
  series: Array<Serie>
};

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
      series: this.getSeries(props.history, graph, metricsType)
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

  getSeries = (history: History, graph: string, metricsType: string): Array<Serie> => {
    const measureHistory = map(history, (item, key) => ({
      metric: key,
      history: item.filter(p => p.value != null)
    })).filter(item => GRAPHS_METRICS[graph].indexOf(item.metric) >= 0);
    return generateSeries(measureHistory, graph, metricsType);
  };

  getMetricType = (metrics: Array<Metric>, graph: string) => {
    const metricKey = GRAPHS_METRICS[graph][0];
    const metric = metrics.find(metric => metric.key === metricKey);
    return metric ? metric.type : 'INT';
  };

  render() {
    return (
      <div className="big-spacer-bottom spacer-top">
        <Link
          className="overview-analysis-graph"
          to={{ pathname: '/project/activity', query: { id: this.props.project } }}>
          <AutoSizer disableHeight={true}>
            {({ width }) => (
              <AdvancedTimeline
                endDate={null}
                startDate={null}
                height={80}
                width={width}
                hideGrid={true}
                hideXAxis={true}
                interpolate="linear"
                metricType={this.state.metricsType}
                padding={[4, 0, 4, 0]}
                series={this.state.series}
                showAreas={['coverage', 'duplications'].includes(this.state.graph)}
              />
            )}
          </AutoSizer>
        </Link>
      </div>
    );
  }
}
