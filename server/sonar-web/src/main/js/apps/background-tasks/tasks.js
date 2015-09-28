import React from 'react';
import {getProjectUrl} from '../../helpers/Url';
import QualifierIcon from '../../components/shared/qualifier-icon';
import PendingIcon from '../../components/shared/pending-icon';
import {STATUSES} from './constants';

export default React.createClass({
  propTypes: {
    tasks: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  onTaskCanceled (task, e) {
    e.preventDefault();
    this.props.onTaskCanceled(task);
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
        inner = <i className="icon-test-status-ok"/>;
        break;
      case STATUSES.FAILED:
        inner = <i className="icon-test-status-error"/>;
        break;
      case STATUSES.CANCELED:
        inner = <i className="icon-test-status-skipped"/>;
        break;
      default:
        inner = '';
    }
    return <td className="thin spacer-right">{inner}</td>;
  },

  renderTaskComponent(task) {
    if (!task.componentKey) {
      return <td><span className="note">{task.id}</span></td>;
    }

    let qualifier = task.type === 'REPORT' ? 'TRK' : null;
    return (
        <td>
          <span className="little-spacer-right">
            <QualifierIcon qualifier={qualifier}/>
          </span>
          <a href={getProjectUrl(task.componentKey)}>{task.componentName}</a>
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
    let inner = task.executionTimeMs ? `${task.executionTimeMs} ms` : '';
    return <td className="thin nowrap text-right">{inner}</td>;
  },

  isAnotherDay(a, b) {
    return !moment(a).isSame(moment(b), 'day');
  },

  renderCancelButton(task) {
    if (task.status === STATUSES.PENDING) {
      return (
          <a onClick={this.onTaskCanceled.bind(this, task)} className="icon-delete" title="Cancel Task"
             data-toggle="tooltip" href="#"></a>
      );
    } else {
      return null;
    }
  },

  renderLogsLink(task) {
    if (task.logs) {
      let url = `${window.baseUrl}/api/ce/logs?taskId=${task.id}`;
      return <a target="_blank" href={url}>Logs</a>;
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
          {this.renderTaskDate(task, 'finishedAt', 'LTS')}
          {this.renderTaskExecutionTime(task)}
          <td className="thin nowrap text-right">
            {this.renderLogsLink(task)}
            {this.renderCancelButton(task)}
          </td>
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
            <th>Submitted</th>
            <th>Started</th>
            <th>Finished</th>
            <th>Duration</th>
            <th>&nbsp;</th>
          </tr>
          </thead>
          <tbody>{tasks}</tbody>
        </table>
    );
  }
});
