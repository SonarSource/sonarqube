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
import ProjectActivityGraphsHeader from './ProjectActivityGraphsHeader';
import GraphsZoom from './GraphsZoom';
import StaticGraphs from './StaticGraphs';
import {
  GRAPHS_METRICS,
  datesQueryChanged,
  generateCoveredLinesMetric,
  historyQueryChanged
} from '../utils';
import { translate } from '../../../helpers/l10n';
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
    const series = this.getSeries(props.measuresHistory);
    this.state = {
      graphStartDate: props.query.from || null,
      graphEndDate: props.query.to || null,
      series
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.measuresHistory !== this.props.measuresHistory ||
      historyQueryChanged(this.props.query, nextProps.query)
    ) {
      const series = this.getSeries(nextProps.measuresHistory);
      this.setState({ series });
    }
    if (
      nextProps.query !== this.props.query &&
      datesQueryChanged(this.props.query, nextProps.query)
    ) {
      this.setState({ graphStartDate: nextProps.query.from, graphEndDate: nextProps.query.to });
    }
  }

  getSeries = (measuresHistory: Array<MeasureHistory>): Array<Serie> =>
    measuresHistory.map(measure => {
      if (measure.metric === 'uncovered_lines') {
        return generateCoveredLinesMetric(
          measure,
          measuresHistory,
          GRAPHS_METRICS[this.props.query.graph].indexOf(measure.metric)
        );
      }
      return {
        name: measure.metric,
        translatedName: translate('metric', measure.metric, 'name'),
        style: GRAPHS_METRICS[this.props.query.graph].indexOf(measure.metric),
        data: measure.history.map(analysis => ({
          x: analysis.date,
          y: this.props.metricsType === 'LEVEL' ? analysis.value : Number(analysis.value)
        }))
      };
    });

  updateGraphZoom = (graphStartDate: ?Date, graphEndDate: ?Date) =>
    this.setState({ graphStartDate, graphEndDate });

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
          updateQuery={this.props.updateQuery}
        />
      </div>
    );
  }
}
