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
import { getCustomGraph, getGraph } from '../../../helpers/storage';
import {
  customMetricsChanged,
  DEFAULT_GRAPH,
  getHistoryMetrics,
  isCustomGraph,
  parseQuery,
  serializeQuery,
  serializeUrlQuery
} from '../utils';
/*:: import type { RawQuery } from '../../../helpers/query'; */
/*:: import type { Analysis, MeasureHistory, Metric, Paging, Query } from '../types'; */

/*::
type Props = {
  location: { pathname: string, query: RawQuery },
  project: {
    configuration?: { showHistory: boolean },
    key: string,
    leakPeriodDate: string,
    qualifier: string
  },
  router: {
    push: ({ pathname: string, query?: RawQuery }) => void,
    replace: ({ pathname: string, query?: RawQuery }) => void
  }
};
*/

/*::
export type State = {
  analyses: Array<Analysis>,
  analysesLoading: boolean,
  graphLoading: boolean,
  initialized: boolean,
  metrics: Array<Metric>,
  measuresHistory: Array<MeasureHistory>,
  paging?: Paging,
  query: Query
};
*/

class ProjectActivityAppContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      analyses: [],
      analysesLoading: false,
      graphLoading: true,
      initialized: false,
      measuresHistory: [],
      metrics: [],
      query: parseQuery(props.location.query)
    };

    if (this.shouldRedirect()) {
      const newQuery = { ...this.state.query, graph: getGraph() };
      if (isCustomGraph(newQuery.graph)) {
        newQuery.customMetrics = getCustomGraph();
      }
      this.props.router.replace({
        pathname: props.location.pathname,
        query: serializeUrlQuery(newQuery)
      });
    }
  }

  componentDidMount() {
    this.mounted = true;
    const elem = document.querySelector('html');
    elem && elem.classList.add('dashboard-page');
    if (!this.shouldRedirect()) {
      this.firstLoadData(this.state.query);
    }
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.location.query !== this.props.location.query) {
      const query = parseQuery(nextProps.location.query);
      if (query.graph !== this.state.query.graph || customMetricsChanged(this.state.query, query)) {
        if (this.state.initialized) {
          this.updateGraphData(query.graph, query.customMetrics);
        } else {
          this.firstLoadData(query);
        }
      }
      this.setState({ query });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    const elem = document.querySelector('html');
    elem && elem.classList.remove('dashboard-page');
  }

  addCustomEvent = (analysis /*: string */, name /*: string */, category /*: ?string */) =>
    api
      .createEvent(analysis, name, category)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.addCustomEvent(analysis, event))
      );

  addVersion = (analysis /*: string */, version /*: string */ /*: Promise<*> */) =>
    this.addCustomEvent(analysis, version, 'VERSION');

  changeEvent = (event /*: string */, name /*: string */ /*: Promise<*> */) =>
    api
      .changeEvent(event, name)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.changeEvent(analysis, event))
      );

  deleteAnalysis = (analysis /*: string */ /*: Promise<*> */) =>
    api.deleteAnalysis(analysis).then(() => {
      if (this.mounted) {
        this.updateGraphData(this.state.query.graph, this.state.query.customMetrics);
        this.setState(actions.deleteAnalysis(analysis));
      }
    });

  deleteEvent = (analysis /*: string */, event /*: string */ /*: Promise<*> */) =>
    api
      .deleteEvent(event)
      .then(() => this.mounted && this.setState(actions.deleteEvent(analysis, event)));

  fetchActivity = (
    project /*: string */,
    p /*: number */,
    ps /*: number */,
    additional /*: ?{
      [string]: string
    } */
  ) => {
    const parameters = { project, p, ps };
    return api
      .getProjectActivity({ ...parameters, ...additional })
      .then(({ analyses, paging }) => ({
        analyses: analyses.map(analysis => ({ ...analysis, date: moment(analysis.date).toDate() })),
        paging
      }));
  };

  fetchMeasuresHistory = (metrics /*: Array<string> */ /*: Promise<Array<MeasureHistory>> */) => {
    if (metrics.length <= 0) {
      return Promise.resolve([]);
    }
    return getAllTimeMachineData(this.props.project.key, metrics).then(
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
  };

  fetchMetrics = () => /*: Promise<Array<Metric>> */ getMetrics().catch(throwGlobalError);

  loadAllActivities = (
    project /*: string */,
    prevResult /*: ?{ analyses: Array<Analysis>, paging: Paging } */
  ) => {
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

  firstLoadData(query /*: Query */) {
    const graphMetrics = getHistoryMetrics(query.graph, query.customMetrics);
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
          initialized: true,
          measuresHistory: response[2],
          metrics: response[1],
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

  updateGraphData = (graph /*: string */, customMetrics /*: Array<string> */) => {
    const graphMetrics = getHistoryMetrics(graph, customMetrics);
    this.setState({ graphLoading: true });
    this.fetchMeasuresHistory(graphMetrics).then((measuresHistory /*: Array<MeasureHistory> */) =>
      this.setState({ graphLoading: false, measuresHistory })
    );
  };

  updateQuery = (newQuery /*: Query */) => {
    const query = serializeUrlQuery({
      ...this.state.query,
      ...newQuery
    });
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...query,
        id: this.props.project.key
      }
    });
  };

  shouldRedirect = () => {
    const locationQuery = this.props.location.query;
    if (locationQuery) {
      const filtered = Object.keys(locationQuery).some(
        key => key !== 'id' && locationQuery[key] !== ''
      );

      const graph = getGraph();
      const emptyCustomGraph = isCustomGraph(graph) && getCustomGraph().length <= 0;

      // if there is no filter, but there are saved preferences in the localStorage
      // also don't redirect to custom if there is no metrics selected for it
      return !filtered && graph != null && graph !== DEFAULT_GRAPH && !emptyCustomGraph;
    }
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
        graphLoading={!this.state.initialized || this.state.graphLoading}
        loading={!this.state.initialized}
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
