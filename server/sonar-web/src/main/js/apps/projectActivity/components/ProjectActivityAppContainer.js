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
import PropTypes from 'prop-types';
import ProjectActivityApp from './ProjectActivityApp';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { getAllTimeMachineData } from '../../../api/time-machine';
import { getAllMetrics } from '../../../api/metrics';
import * as api from '../../../api/projectActivity';
import * as actions from '../actions';
import { getBranchName } from '../../../helpers/branches';
import { parseDate } from '../../../helpers/dates';
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
type Component = {
  breadcrumbs: Array<{ key: string, qualifier: string}>,
  configuration?: { showHistory: boolean },
  key: string,
  leakPeriodDate?: string,
  qualifier: string
};

type Props = {
  branch?: {},
  location: { pathname: string, query: RawQuery },
  component: Component
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

export default class ProjectActivityAppContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

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
  }

  componentDidMount() {
    this.mounted = true;
    if (this.shouldRedirect()) {
      const newQuery = { ...this.state.query, graph: getGraph() };
      if (isCustomGraph(newQuery.graph)) {
        newQuery.customMetrics = getCustomGraph();
      }
      this.context.router.replace({
        pathname: this.props.location.pathname,
        query: {
          ...serializeUrlQuery(newQuery),
          branch: getBranchName(this.props.branch)
        }
      });
    } else {
      this.firstLoadData(this.state.query, this.props.component);
    }
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.location.query !== this.props.location.query) {
      const query = parseQuery(nextProps.location.query);
      if (query.graph !== this.state.query.graph || customMetricsChanged(this.state.query, query)) {
        if (this.state.initialized) {
          this.updateGraphData(query.graph, query.customMetrics);
        } else {
          this.firstLoadData(query, nextProps.component);
        }
      }
      this.setState({ query });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  addCustomEvent = (analysis /*: string */, name /*: string */, category /*: ?string */) =>
    api
      .createEvent(analysis, name, category)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.addCustomEvent(analysis, event))
      );

  addVersion = (analysis /*: string */, version /*: string */) =>
    this.addCustomEvent(analysis, version, 'VERSION');

  changeEvent = (event /*: string */, name /*: string */) =>
    api
      .changeEvent(event, name)
      .then(
        ({ analysis, ...event }) =>
          this.mounted && this.setState(actions.changeEvent(analysis, event))
      );

  deleteAnalysis = (analysis /*: string */) =>
    api.deleteAnalysis(analysis).then(() => {
      if (this.mounted) {
        this.updateGraphData(this.state.query.graph, this.state.query.customMetrics);
        this.setState(actions.deleteAnalysis(analysis));
      }
    });

  deleteEvent = (analysis /*: string */, event /*: string */) =>
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
    const parameters = {
      project,
      p,
      ps,
      branch: getBranchName(this.props.branch)
    };
    return api
      .getProjectActivity({ ...additional, ...parameters })
      .then(({ analyses, paging }) => ({
        analyses: analyses.map(analysis => ({ ...analysis, date: parseDate(analysis.date) })),
        paging
      }));
  };

  fetchMeasuresHistory = (metrics /*: Array<string> */) => {
    if (metrics.length <= 0) {
      return Promise.resolve([]);
    }
    return getAllTimeMachineData(this.props.component.key, metrics, {
      branch: getBranchName(this.props.branch)
    }).then(
      ({ measures }) =>
        measures.map(measure => ({
          metric: measure.metric,
          history: measure.history.map(analysis => ({
            date: parseDate(analysis.date),
            value: analysis.value
          }))
        })),
      () => {}
    );
  };

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

  getTopLevelComponent = (component /*: Component */) => {
    let current = component.breadcrumbs.length - 1;
    while (
      current > 0 &&
      !['TRK', 'VW', 'APP'].includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }
    return component.breadcrumbs[current].key;
  };

  firstLoadData(query /*: Query */, component /*: Component */) {
    const graphMetrics = getHistoryMetrics(query.graph, query.customMetrics);
    const topLevelComponent = this.getTopLevelComponent(component);
    Promise.all([
      this.fetchActivity(topLevelComponent, 1, 100, serializeQuery(query)),
      getAllMetrics(),
      this.fetchMeasuresHistory(graphMetrics)
    ]).then(
      response => {
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

          this.loadAllActivities(topLevelComponent).then(({ analyses, paging }) => {
            if (this.mounted) {
              this.setState({
                analyses,
                analysesLoading: false,
                paging
              });
            }
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ initialized: true, analysesLoading: false, graphLoading: false });
        }
      }
    );
  }

  updateGraphData = (graph /*: string */, customMetrics /*: Array<string> */) => {
    const graphMetrics = getHistoryMetrics(graph, customMetrics);
    this.setState({ graphLoading: true });
    this.fetchMeasuresHistory(graphMetrics).then(
      (measuresHistory /*: Array<MeasureHistory> */) => {
        if (this.mounted) {
          this.setState({ graphLoading: false, measuresHistory });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ graphLoading: false, measuresHistory: [] });
        }
      }
    );
  };

  updateQuery = (newQuery /*: Query */) => {
    const query = serializeUrlQuery({
      ...this.state.query,
      ...newQuery
    });
    this.context.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...query,
        branch: getBranchName(this.props.branch),
        id: this.props.component.key
      }
    });
  };

  shouldRedirect = () => {
    const locationQuery = this.props.location.query;
    if (!locationQuery) {
      return false;
    }
    const filtered = Object.keys(locationQuery).some(
      key => key !== 'id' && locationQuery[key] !== ''
    );

    const graph = getGraph();
    const emptyCustomGraph = isCustomGraph(graph) && getCustomGraph().length <= 0;

    // if there is no filter, but there are saved preferences in the localStorage
    // also don't redirect to custom if there is no metrics selected for it
    return !filtered && graph != null && graph !== DEFAULT_GRAPH && !emptyCustomGraph;
  };

  render() {
    if (this.shouldRedirect()) {
      return null;
    }

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
        initializing={!this.state.initialized}
        metrics={this.state.metrics}
        measuresHistory={this.state.measuresHistory}
        project={this.props.component}
        query={this.state.query}
        updateQuery={this.updateQuery}
      />
    );
  }
}
