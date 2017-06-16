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
import moment from 'moment';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import ProjectActivityApp from './ProjectActivityApp';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { getComponent } from '../../../store/rootReducer';
import { getAllTimeMachineData } from '../../../api/time-machine';
import { getMetrics } from '../../../api/metrics';
import * as api from '../../../api/projectActivity';
import * as actions from '../actions';
import { GRAPHS_METRICS, parseQuery, serializeQuery, serializeUrlQuery } from '../utils';
import type { RawQuery } from '../../../helpers/query';
import type { Analysis, MeasureHistory, Metric, Paging, Query } from '../types';

type Props = {
  location: { pathname: string, query: RawQuery },
  project: { configuration?: { showHistory: boolean }, key: string, leakPeriodDate: string },
  router: { push: ({ pathname: string, query?: RawQuery }) => void }
};

export type State = {
  analyses: Array<Analysis>,
  analysesLoading: boolean,
  graphLoading: boolean,
  loading: boolean,
  metrics: Array<Metric>,
  measuresHistory: Array<MeasureHistory>,
  paging?: Paging,
  query: Query
};

class ProjectActivityAppContainer extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      analyses: [],
      analysesLoading: false,
      graphLoading: true,
      loading: true,
      measuresHistory: [],
      metrics: [],
      query: parseQuery(props.location.query)
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.firstLoadData();
    const elem = document.querySelector('html');
    elem && elem.classList.add('dashboard-page');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      const query = parseQuery(this.props.location.query);
      if (query.graph !== this.state.query.graph) {
        this.updateGraphData(query.graph);
      }
      this.setState({ query });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const elem = document.querySelector('html');
    elem && elem.classList.remove('dashboard-page');
  }

  addCustomEvent = (analysis: string, name: string, category?: string): Promise<*> =>
    api
      .createEvent(analysis, name, category)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.addCustomEvent(analysis, event)),
        throwGlobalError
      );

  addVersion = (analysis: string, version: string): Promise<*> =>
    this.addCustomEvent(analysis, version, 'VERSION');

  changeEvent = (event: string, name: string): Promise<*> =>
    api
      .changeEvent(event, name)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.changeEvent(analysis, event)),
        throwGlobalError
      );

  deleteAnalysis = (analysis: string): Promise<*> =>
    api
      .deleteAnalysis(analysis)
      .then(
        () => this.mounted && this.setState(actions.deleteAnalysis(analysis)),
        throwGlobalError
      );

  deleteEvent = (analysis: string, event: string): Promise<*> =>
    api
      .deleteEvent(event)
      .then(
        () => this.mounted && this.setState(actions.deleteEvent(analysis, event)),
        throwGlobalError
      );

  fetchActivity = (
    project: string,
    p: number,
    ps: number,
    additional?: {
      [string]: string
    }
  ): Promise<{ analyses: Array<Analysis>, paging: Paging }> => {
    const parameters = { project, p, ps };
    return api.getProjectActivity({ ...parameters, ...additional }).catch(throwGlobalError);
  };

  fetchMeasuresHistory = (metrics: Array<string>): Promise<Array<MeasureHistory>> =>
    getAllTimeMachineData(this.props.project.key, metrics).then(
      ({ measures }) =>
        measures.map(measure => ({
          metric: measure.metric,
          history: measure.history.map(analysis => ({
            date: moment(analysis.date).toDate(),
            value: analysis.value
          }))
        })),
      throwGlobalError
    );

  fetchMetrics = (): Promise<Array<Metric>> => getMetrics().catch(throwGlobalError);

  loadAllActivities = (
    project: string,
    prevResult?: { analyses: Array<Analysis>, paging: Paging }
  ): Promise<{ analyses: Array<Analysis>, paging: Paging }> => {
    if (
      prevResult &&
      prevResult.paging.pageIndex * prevResult.paging.pageSize >= prevResult.paging.total
    ) {
      return Promise.resolve(prevResult);
    }
    const nextPage = prevResult ? prevResult.paging.pageIndex + 1 : 1;
    return this.fetchActivity(project, nextPage, 500).then(result => {
      if (!prevResult) {
        return this.loadAllActivities(project, result);
      }
      return this.loadAllActivities(project, {
        analyses: prevResult.analyses.concat(result.analyses),
        paging: result.paging
      });
    });
  };

  firstLoadData() {
    const { query } = this.state;
    const graphMetrics = GRAPHS_METRICS[query.graph];
    Promise.all([
      this.fetchActivity(query.project, 1, 100, serializeQuery(query)),
      this.fetchMetrics(),
      this.fetchMeasuresHistory(graphMetrics)
    ]).then(response => {
      if (this.mounted) {
        this.setState({
          analyses: response[0].analyses,
          analysesLoading: true,
          graphLoading: false,
          loading: false,
          metrics: response[1],
          measuresHistory: response[2],
          paging: response[0].paging
        });

        this.loadAllActivities(query.project).then(({ analyses, paging }) => {
          if (this.mounted) {
            this.setState({
              analyses,
              analysesLoading: false,
              paging
            });
          }
        });
      }
    });
  }

  updateGraphData = (graph: string) => {
    this.setState({ graphLoading: true });
    return this.fetchMeasuresHistory(
      GRAPHS_METRICS[graph]
    ).then((measuresHistory: Array<MeasureHistory>) =>
      this.setState({ graphLoading: false, measuresHistory })
    );
  };

  updateQuery = (newQuery: Query) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeUrlQuery({
          ...this.state.query,
          ...newQuery
        }),
        id: this.props.project.key
      }
    });
  };

  render() {
    return (
      <ProjectActivityApp
        addCustomEvent={this.addCustomEvent}
        addVersion={this.addVersion}
        analyses={this.state.analyses}
        analysesLoading={this.state.analysesLoading}
        changeEvent={this.changeEvent}
        deleteAnalysis={this.deleteAnalysis}
        deleteEvent={this.deleteEvent}
        graphLoading={this.state.graphLoading}
        loading={this.state.loading}
        metrics={this.state.metrics}
        measuresHistory={this.state.measuresHistory}
        project={this.props.project}
        query={this.state.query}
        updateQuery={this.updateQuery}
      />
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  project: getComponent(state, ownProps.location.query.id)
});

export default connect(mapStateToProps)(withRouter(ProjectActivityAppContainer));
