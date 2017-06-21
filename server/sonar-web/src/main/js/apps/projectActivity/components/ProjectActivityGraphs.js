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
import StaticGraphs from './StaticGraphs';
import { GRAPHS_METRICS, generateCoveredLinesMetric, historyQueryChanged } from '../utils';
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
  filteredSeries: Array<Serie>,
  series: Array<Serie>
};

export default class ProjectActivityGraphs extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    const series = this.getSeries(props.measuresHistory);
    this.state = {
      filteredSeries: this.filterSeries(series, props.query),
      series
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      nextProps.measuresHistory !== this.props.measuresHistory ||
      historyQueryChanged(this.props.query, nextProps.query)
    ) {
      const series = this.getSeries(nextProps.measuresHistory);
      this.setState({
        filteredSeries: this.filterSeries(series, nextProps.query),
        series
      });
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

  filterSeries = (series: Array<Serie>, query: Query): Array<Serie> => {
    if (!query.from && !query.to) {
      return series;
    }
    return series.map(serie => ({
      ...serie,
      data: serie.data.filter(p => {
        const isAfterFrom = !query.from || p.x >= query.from;
        const isBeforeTo = !query.to || p.x <= query.to;
        return isAfterFrom && isBeforeTo;
      })
    }));
  };

  render() {
    const { graph, category } = this.props.query;
    return (
      <div className="project-activity-layout-page-main-inner boxed-group boxed-group-inner">
        <ProjectActivityGraphsHeader graph={graph} updateQuery={this.props.updateQuery} />
        <StaticGraphs
          analyses={this.props.analyses}
          eventFilter={category}
          filteredSeries={this.state.filteredSeries}
          leakPeriodDate={this.props.leakPeriodDate}
          loading={this.props.loading}
          metricsType={this.props.metricsType}
          project={this.props.project}
          series={this.state.series}
          showAreas={['coverage', 'duplications'].includes(graph)}
        />
      </div>
    );
  }
}
