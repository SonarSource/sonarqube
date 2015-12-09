import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { getQueue, getActivity, cancelTask, cancelAllTasks } from '../../api/ce';
import { STATUSES, CURRENTS, DATE, DEBOUNCE_DELAY } from './constants';
import Header from './header';
import Stats from './stats';
import Search from './search';
import Tasks from './tasks';
import ListFooter from '../../components/shared/list-footer';


const PAGE_SIZE = 200;


export default React.createClass({
  getInitialState() {
    return {
      queue: [],
      activity: [],
      activityTotal: 0,
      activityPage: 1,
      statusFilter: STATUSES.ALL,
      currentsFilter: CURRENTS.ALL,
      dateFilter: DATE.ANY,
      searchQuery: ''
    };
  },

  componentDidMount() {
    this.requestData();
    this.requestData = _.debounce(this.requestData, DEBOUNCE_DELAY);
  },

  getComponentFilter() {
    if (this.props.options.component) {
      return { componentId: this.props.options.component.id };
    } else {
      return {};
    }
  },

  getDateFilter() {
    const DATE_FORMAT = 'YYYY-MM-DD';
    let filter = {};
    switch (this.state.dateFilter) {
      case DATE.TODAY:
        filter.minSubmittedAt = moment().startOf('day').format(DATE_FORMAT);
        break;
      case DATE.CUSTOM:
        if (this.state.minDate) {
          filter.minSubmittedAt = moment(this.state.minDate).format(DATE_FORMAT);
        }
        if (this.state.maxDate) {
          filter.maxExecutedAt = moment(this.state.maxDate).format(DATE_FORMAT);
        }
        break;
      default:
      // do nothing
    }
    return filter;
  },

  getCurrentFilters() {
    let filters = {};
    if (this.state.statusFilter !== STATUSES.ALL) {
      filters.status = this.state.statusFilter;
    }
    if (this.state.currentsFilter !== STATUSES.ALL) {
      filters.onlyCurrents = true;
    }
    if (this.state.dateFilter !== DATE.ANY) {
      _.extend(filters, this.getDateFilter());
    }
    if (this.state.searchQuery) {
      _.extend(filters, { componentQuery: this.state.searchQuery });
    }
    return filters;
  },

  requestData() {
    this.requestQueue();
    this.requestActivity();
    this.requestFailures();
  },

  requestQueue() {
    let filters = this.getComponentFilter();
    if (!Object.keys(this.getCurrentFilters()).length) {
      getQueue(filters).done(queue => {
        let tasks = queue.tasks;
        this.setState({
          queue: this.orderTasks(tasks),
          pendingCount: this.countPending(tasks),
          inProgressDuration: this.getInProgressDuration(tasks)
        });
      });
    } else {
      this.setState({ queue: [] });
    }
  },

  requestActivity() {
    let filters = _.extend(
        this.getCurrentFilters(),
        this.getComponentFilter(),
        { p: this.state.activityPage, ps: PAGE_SIZE });
    getActivity(filters).done(activity => {
      let newActivity = activity.paging.pageIndex === 1 ?
          activity.tasks : [].concat(this.state.activity, activity.tasks);
      this.setState({
        activity: this.orderTasks(newActivity),
        activityTotal: activity.paging.total,
        activityPage: activity.paging.pageIndex
      });
    });
  },

  requestFailures() {
    let filters = _.extend(
        this.getComponentFilter(),
        { ps: 1, onlyCurrents: true, status: STATUSES.FAILED });
    getActivity(filters).done(failures => {
      this.setState({ failuresCount: failures.paging.total });
    });
  },

  countPending(tasks) {
    return _.where(tasks, { status: STATUSES.PENDING }).length;
  },

  orderTasks(tasks) {
    return _.sortBy(tasks, task => {
      return -moment(task.submittedAt).unix();
    });
  },

  getInProgressDuration(tasks) {
    let taskInProgress = _.findWhere(tasks, { status: STATUSES.IN_PROGRESS });
    return taskInProgress ? taskInProgress.executionTimeMs : null;
  },

  onStatusChange(newStatus) {
    this.setState({ statusFilter: newStatus, activityPage: 1 }, this.requestData);
  },

  onCurrentsChange(newCurrents) {
    this.setState({ currentsFilter: newCurrents, activityPage: 1 }, this.requestData);
  },

  onDateChange(newDate, minDate, maxDate) {
    this.setState({
      dateFilter: newDate,
      minDate: minDate,
      maxDate: maxDate,
      activityPage: 1
    }, this.requestData);
  },

  onSearch(query) {
    this.setState({ searchQuery: query }, this.requestData);
  },

  loadMore() {
    this.setState({ activityPage: this.state.activityPage + 1 }, this.requestActivity);
  },

  showFailures() {
    this.setState({
      statusFilter: STATUSES.FAILED,
      currentsFilter: CURRENTS.ONLY_CURRENTS,
      activityPage: 1
    }, this.requestActivity);
  },

  onTaskCanceled(task) {
    cancelTask(task.id).then(this.requestData);
  },

  cancelPending() {
    cancelAllTasks().then(this.requestData);
  },

  handleFilter(task) {
    this.onSearch(task.componentKey);
  },

  render() {
    return (
        <div className="page">
          <Header/>

          <Stats
              {...this.props}
              {...this.state}
              cancelPending={this.cancelPending}
              showFailures={this.showFailures}/>

          <Search
              {...this.props}
              {...this.state}
              refresh={this.requestData}
              onStatusChange={this.onStatusChange}
              onCurrentsChange={this.onCurrentsChange}
              onDateChange={this.onDateChange}
              onSearch={this.onSearch}/>

          <Tasks
              {...this.props}
              tasks={[].concat(this.state.queue, this.state.activity)}
              onTaskCanceled={this.onTaskCanceled}
              onFilter={this.handleFilter}/>

          <ListFooter
              count={this.state.queue.length + this.state.activity.length}
              total={this.state.queue.length + this.state.activityTotal}
              loadMore={this.loadMore}/>
        </div>
    );
  }
});
