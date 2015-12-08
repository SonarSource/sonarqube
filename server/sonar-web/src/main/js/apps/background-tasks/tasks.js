import moment from 'moment';
import React from 'react';

import { getComponentUrl } from '../../helpers/urls';
import QualifierIcon from '../../components/shared/qualifier-icon';
import PendingIcon from '../../components/shared/pending-icon';
import { STATUSES } from './constants';
import { formatDuration } from './helpers';
import { TooltipsMixin } from '../../components/mixins/tooltips-mixin';


export default React.createClass({
  propTypes: {
    tasks: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  mixins: [TooltipsMixin],

  onTaskCanceled (task, e) {
    e.preventDefault();
    this.props.onTaskCanceled(task);
  },

  handleFilter (task, e) {
    e.preventDefault();
    this.props.onFilter(task);
  },

  renderTaskStatus(task) {
    let inner;
    switch (task.status) {
      case STATUSES.PENDING:
        inner = <PendingIcon/>;
        break;
      case STATUSES.IN_PROGRESS:
        inner = <i className="spinner"/>;
        break;
      case STATUSES.SUCCESS:
        inner = <span className="badge badge-success">{window.t('background_task.status.SUCCESS')}</span>;
        break;
      case STATUSES.FAILED:
        inner = <span className="badge badge-danger">{window.t('background_task.status.FAILED')}</span>;
        break;
      case STATUSES.CANCELED:
        inner = <span className="badge badge-muted">{window.t('background_task.status.CANCELED')}</span>;
        break;
      default:
        inner = '';
    }
    return <td className="thin spacer-right" title={window.t('background_task.status', task.status)}
               data-toggle="tooltip">{inner}</td>;
  },

  renderTaskComponent(task) {
    if (!task.componentKey) {
      return <td><span className="note">{task.id}</span></td>;
    }

    return (
        <td>
          <span className="little-spacer-right">
            <QualifierIcon qualifier={task.componentQualifier}/>
          </span>
          <a href={getComponentUrl(task.componentKey)}>{task.componentName}</a>
        </td>
    );
  },

  renderTaskDate(task, field, format = 'LLL') {
    let date = task[field];
    return (
        <td className="thin nowrap text-right">
          {date ? moment(date).format(format) : ''}
        </td>
    );
  },

  renderTaskDay(task, previousTask) {
    let shouldDisplay = !previousTask || this.isAnotherDay(task.submittedAt, previousTask.submittedAt);
    return (
        <td className="thin nowrap text-right">
          {shouldDisplay ? moment(task.submittedAt).format('LL') : ''}
        </td>
    );
  },

  renderTaskExecutionTime(task) {
    return <td className="thin nowrap text-right">{formatDuration(task.executionTimeMs)}</td>;
  },

  isAnotherDay(a, b) {
    return !moment(a).isSame(moment(b), 'day');
  },

  renderFilter(task) {
    if (this.props.options && this.props.options.component) {
      return null;
    }
    return <td className="thin nowrap">
      <a onClick={this.handleFilter.bind(this, task)} className="icon-filter icon-half-transparent spacer-left" href="#"
         title={`Show only "${task.componentName}" tasks`} data-toggle="tooltip"/>
    </td>;
  },

  renderCancelButton(task) {
    if (task.status === STATUSES.PENDING) {
      return (
          <a onClick={this.onTaskCanceled.bind(this, task)} className="icon-delete"
             title={window.t('background_tasks.cancel_task')} data-toggle="tooltip" href="#"></a>
      );
    } else {
      return null;
    }
  },

  renderLogsLink(task) {
    if (task.logs) {
      let url = `${window.baseUrl}/api/ce/logs?taskId=${task.id}`;
      return <a target="_blank" href={url}>{window.t('background_tasks.logs')}</a>;
    } else {
      return null;
    }
  },

  renderTask(task, index, tasks) {
    let previousTask = index > 0 ? tasks[index - 1] : null;
    return (
        <tr key={task.id}>
          {this.renderTaskStatus(task)}
          {this.renderTaskComponent(task)}
          {this.renderTaskDay(task, previousTask)}
          {this.renderTaskDate(task, 'submittedAt', 'LTS')}
          {this.renderTaskDate(task, 'startedAt', 'LTS')}
          {this.renderTaskDate(task, 'executedAt', 'LTS')}
          {this.renderTaskExecutionTime(task)}
          <td className="thin nowrap text-right">
            {this.renderLogsLink(task)}
            {this.renderCancelButton(task)}
          </td>
          {this.renderFilter(task)}
        </tr>
    );
  },

  render() {
    if (!this.props.tasks.length) {
      return null;
    }
    let tasks = this.props.tasks.map(this.renderTask);
    return (
        <table className="data zebra background-tasks">
          <thead>
          <tr>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
            <th>&nbsp;</th>
            <th>{window.t('background_tasks.table.submitted')}</th>
            <th>{window.t('background_tasks.table.started')}</th>
            <th>{window.t('background_tasks.table.finished')}</th>
            <th>{window.t('background_tasks.table.duration')}</th>
            <th>&nbsp;</th>
          </tr>
          </thead>
          <tbody>{tasks}</tbody>
        </table>
    );
  }
});
