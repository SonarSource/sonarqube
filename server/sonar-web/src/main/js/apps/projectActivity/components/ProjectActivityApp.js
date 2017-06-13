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
import Helmet from 'react-helmet';
import moment from 'moment';
import ProjectActivityPageHeader from './ProjectActivityPageHeader';
import ProjectActivityAnalysesList from './ProjectActivityAnalysesList';
import ProjectActivityGraphs from './ProjectActivityGraphs';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import * as api from '../../../api/projectActivity';
import * as actions from '../actions';
import { getAllTimeMachineData } from '../../../api/time-machine';
import { getMetrics } from '../../../api/metrics';
import { GRAPHS_METRICS, parseQuery, serializeQuery, serializeUrlQuery } from '../utils';
import { translate } from '../../../helpers/l10n';
import './projectActivity.css';
import type { Analysis, LeakPeriod, MeasureHistory, Metric, Query, Paging } from '../types';
import type { RawQuery } from '../../../helpers/query';

type Props = {
  location: { pathname: string, query: RawQuery },
  project: { configuration?: { showHistory: boolean }, key: string },
  router: { push: ({ pathname: string, query?: RawQuery }) => void }
};

export type State = {
  analyses: Array<Analysis>,
  leakPeriod?: LeakPeriod,
  loading: boolean,
  measures: Array<*>,
  metrics: Array<Metric>,
  measuresHistory: Array<MeasureHistory>,
  paging?: Paging,
  query: Query
};

export default class ProjectActivityApp extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      analyses: [],
      loading: true,
      measures: [],
      measuresHistory: [],
      metrics: [],
      query: parseQuery(props.location.query)
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.handleQueryChange();
    const elem = document.querySelector('html');
    elem && elem.classList.add('dashboard-page');
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      this.handleQueryChange();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const elem = document.querySelector('html');
    elem && elem.classList.remove('dashboard-page');
  }

  fetchActivity = (
    query: Query,
    additional?: {}
  ): Promise<{ analyses: Array<Analysis>, paging: Paging }> => {
    const parameters = {
      ...serializeQuery(query),
      ...additional
    };
    return api.getProjectActivity(parameters).catch(throwGlobalError);
  };

  fetchMetrics = (): Promise<Array<Metric>> => getMetrics().catch(throwGlobalError);

  fetchMeasuresHistory = (metrics: Array<string>): Promise<Array<MeasureHistory>> =>
    getAllTimeMachineData(this.props.project.key, metrics)
      .then(({ measures }) =>
        measures.map(measure => ({
          metric: measure.metric,
          history: measure.history.map(analysis => ({
            date: moment(analysis.date).toDate(),
            value: analysis.value
          }))
        }))
      )
      .catch(throwGlobalError);

  fetchMoreActivity = () => {
    const { paging, query } = this.state;
    if (!paging) {
      return;
    }

    this.setState({ loading: true });
    this.fetchActivity(query, { p: paging.pageIndex + 1 }).then(({ analyses, paging }) => {
      if (this.mounted) {
        this.setState((state: State) => ({
          analyses: state.analyses ? state.analyses.concat(analyses) : analyses,
          loading: false,
          paging
        }));
      }
    });
  };

  addCustomEvent = (analysis: string, name: string, category?: string): Promise<*> =>
    api
      .createEvent(analysis, name, category)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.addCustomEvent(analysis, event))
      )
      .catch(throwGlobalError);

  addVersion = (analysis: string, version: string): Promise<*> =>
    this.addCustomEvent(analysis, version, 'VERSION');

  deleteEvent = (analysis: string, event: string): Promise<*> =>
    api
      .deleteEvent(event)
      .then(() => this.mounted && this.setState(actions.deleteEvent(analysis, event)))
      .catch(throwGlobalError);

  changeEvent = (event: string, name: string): Promise<*> =>
    api
      .changeEvent(event, name)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.changeEvent(analysis, event))
      )
      .catch(throwGlobalError);

  deleteAnalysis = (analysis: string): Promise<*> =>
    api
      .deleteAnalysis(analysis)
      .then(() => this.mounted && this.setState(actions.deleteAnalysis(analysis)))
      .catch(throwGlobalError);

  getMetricType = () => {
    const metricKey = GRAPHS_METRICS[this.state.query.graph][0];
    const metric = this.state.metrics.find(metric => metric.key === metricKey);
    return metric ? metric.type : 'INT';
  };

  handleQueryChange() {
    const query = parseQuery(this.props.location.query);
    const graphMetrics = GRAPHS_METRICS[query.graph];
    this.setState({ loading: true, query });

    Promise.all([
      this.fetchActivity(query),
      this.fetchMetrics(),
      this.fetchMeasuresHistory(graphMetrics)
    ]).then(response => {
      if (this.mounted) {
        this.setState({
          analyses: response[0].analyses,
          loading: false,
          metrics: response[1],
          measuresHistory: response[2],
          paging: response[0].paging
        });
      }
    });
  }

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
    const { query } = this.state;
    const { configuration } = this.props.project;
    const canAdmin = configuration ? configuration.showHistory : false;

    return (
      <div id="project-activity" className="page page-limited">
        <Helmet title={translate('project_activity.page')} />

        <ProjectActivityPageHeader category={query.category} updateQuery={this.updateQuery} />

        <div className="layout-page project-activity-page">
          <ProjectActivityAnalysesList
            addCustomEvent={this.addCustomEvent}
            addVersion={this.addVersion}
            analyses={this.state.analyses}
            canAdmin={canAdmin}
            changeEvent={this.changeEvent}
            deleteAnalysis={this.deleteAnalysis}
            deleteEvent={this.deleteEvent}
            fetchMoreActivity={this.fetchMoreActivity}
            paging={this.state.paging}
          />

          <ProjectActivityGraphs
            analyses={this.state.analyses}
            leakPeriod={this.state.leakPeriod}
            loading={this.state.loading}
            measuresHistory={this.state.measuresHistory}
            metricsType={this.getMetricType()}
            project={this.props.project.key}
            query={query}
            updateQuery={this.updateQuery}
          />
        </div>
      </div>
    );
  }
}
