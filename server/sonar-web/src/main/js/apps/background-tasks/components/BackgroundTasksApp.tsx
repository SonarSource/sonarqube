/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import Helmet from 'react-helmet';
import { debounce, uniq } from 'lodash';
import { connect } from 'react-redux';
import { InjectedRouter } from 'react-router';
import { Location } from 'history';
import Header from './Header';
import Footer from './Footer';
import StatsContainer from './StatsContainer';
import Search from './Search';
import Tasks from './Tasks';
import { DEFAULT_FILTERS, DEBOUNCE_DELAY, STATUSES, CURRENTS } from '../constants';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import {
  getTypes,
  getActivity,
  getStatus,
  cancelAllTasks,
  cancelTask as cancelTaskAPI
} from '../../../api/ce';
import { updateTask, mapFiltersToParameters, Query } from '../utils';
import { fetchOrganizations } from '../../../store/rootActions';
import { translate } from '../../../helpers/l10n';
import { parseAsDate } from '../../../helpers/query';
import { toShortNotSoISOString } from '../../../helpers/dates';
import '../background-tasks.css';

interface Props {
  component?: { id: string };
  fetchOrganizations: (keys: string[]) => void;
  location: Location;
  router: Pick<InjectedRouter, 'push'>;
}

interface State {
  loading: boolean;
  tasks: T.Task[];
  types?: string[];
  query: string;
  pendingCount: number;
  failingCount: number;
}

class BackgroundTasksApp extends React.PureComponent<Props, State> {
  loadTasksDebounced: () => void;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      failingCount: 0,
      loading: true,
      pendingCount: 0,
      query: '',
      tasks: []
    };
    this.loadTasksDebounced = debounce(this.loadTasks, DEBOUNCE_DELAY);
  }

  componentDidMount() {
    this.mounted = true;

    getTypes().then(
      types => {
        this.setState({ types });
        this.loadTasks();
      },
      () => {}
    );
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.component !== this.props.component ||
      prevProps.location !== this.props.location
    ) {
      this.loadTasksDebounced();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  loadTasks = () => {
    this.setState({ loading: true });

    const status = this.props.location.query.status || DEFAULT_FILTERS.status;
    const taskType = this.props.location.query.taskType || DEFAULT_FILTERS.taskType;
    const currents = this.props.location.query.currents || DEFAULT_FILTERS.currents;
    const minSubmittedAt =
      this.props.location.query.minSubmittedAt || DEFAULT_FILTERS.minSubmittedAt;
    const maxExecutedAt = this.props.location.query.maxExecutedAt || DEFAULT_FILTERS.maxExecutedAt;
    const query = this.props.location.query.query || DEFAULT_FILTERS.query;

    const filters = { status, taskType, currents, minSubmittedAt, maxExecutedAt, query };
    const parameters /*: Object */ = mapFiltersToParameters(filters);

    if (this.props.component) {
      parameters.componentId = this.props.component.id;
    }

    Promise.all([getActivity(parameters), getStatus(parameters.componentId)]).then(responses => {
      if (this.mounted) {
        const [activity, status] = responses;
        const { tasks } = activity;

        const pendingCount = status.pending;
        const failingCount = status.failing;

        const organizations = uniq(tasks.map(task => task.organization).filter(o => o));
        this.props.fetchOrganizations(organizations);

        this.setState({
          tasks,
          pendingCount,
          failingCount,
          loading: false
        });
      }
    }, this.stopLoading);
  };

  handleFilterUpdate = (nextState: Partial<Query>) => {
    const nextQuery = { ...this.props.location.query, ...nextState };

    // remove defaults
    Object.keys(DEFAULT_FILTERS).forEach((key: keyof typeof DEFAULT_FILTERS) => {
      if (nextQuery[key] === DEFAULT_FILTERS[key]) {
        delete nextQuery[key];
      }
    });

    if (nextQuery.minSubmittedAt) {
      nextQuery.minSubmittedAt = toShortNotSoISOString(nextQuery.minSubmittedAt);
    }

    if (nextQuery.maxExecutedAt) {
      nextQuery.maxExecutedAt = toShortNotSoISOString(nextQuery.maxExecutedAt);
    }

    this.props.router.push({
      pathname: this.props.location.pathname,
      query: nextQuery
    });
  };

  handleCancelTask = (task: T.Task) => {
    this.setState({ loading: true });

    return cancelTaskAPI(task.id).then(nextTask => {
      if (this.mounted) {
        this.setState(state => ({
          tasks: updateTask(state.tasks, nextTask),
          loading: false
        }));
      }
    }, this.stopLoading);
  };

  handleFilterTask = (task: T.Task) => {
    this.handleFilterUpdate({ query: task.componentKey });
  };

  handleShowFailing() {
    this.handleFilterUpdate({
      ...DEFAULT_FILTERS,
      status: STATUSES.FAILED,
      currents: CURRENTS.ONLY_CURRENTS
    });
  }

  handleCancelAllPending = () => {
    this.setState({ loading: true });

    cancelAllTasks().then(() => {
      if (this.mounted) {
        this.loadTasks();
      }
    }, this.stopLoading);
  };

  render() {
    const { component } = this.props;
    const { loading, types, tasks, pendingCount, failingCount } = this.state;

    if (!types) {
      return (
        <div className="page page-limited">
          <i className="spinner" />
        </div>
      );
    }

    const status = this.props.location.query.status || DEFAULT_FILTERS.status;
    const taskType = this.props.location.query.taskType || DEFAULT_FILTERS.taskType;
    const currents = this.props.location.query.currents || DEFAULT_FILTERS.currents;
    const minSubmittedAt = parseAsDate(this.props.location.query.minSubmittedAt);
    const maxExecutedAt = parseAsDate(this.props.location.query.maxExecutedAt);
    const query = this.props.location.query.query || '';

    return (
      <div className="page page-limited">
        <Suggestions suggestions="background_tasks" />
        <Helmet title={translate('background_tasks.page')} />
        <Header component={component} />

        <StatsContainer
          component={component}
          failingCount={failingCount}
          onCancelAllPending={this.handleCancelAllPending}
          onShowFailing={this.handleShowFailing}
          pendingCount={pendingCount}
        />

        <Search
          component={component}
          currents={currents}
          loading={loading}
          maxExecutedAt={maxExecutedAt}
          minSubmittedAt={minSubmittedAt}
          onFilterUpdate={this.handleFilterUpdate}
          onReload={this.loadTasksDebounced}
          query={query}
          status={status}
          taskType={taskType}
          types={types}
        />

        <Tasks
          component={component}
          loading={loading}
          onCancelTask={this.handleCancelTask}
          onFilterTask={this.handleFilterTask}
          tasks={tasks}
        />

        <Footer tasks={tasks} />
      </div>
    );
  }
}

const mapDispatchToProps = { fetchOrganizations };

export default connect(
  null,
  mapDispatchToProps
)(BackgroundTasksApp);
