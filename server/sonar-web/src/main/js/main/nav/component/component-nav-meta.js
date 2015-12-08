import moment from 'moment';
import React from 'react';
import PendingIcon from '../../../components/shared/pending-icon';

export default React.createClass({
  render() {
    let metaList = [];
    let canSeeBackgroundTasks = this.props.conf.showBackgroundTasks;
    let backgroundTasksUrl = `${baseUrl}/project/background_tasks?id=${encodeURIComponent(this.props.component.key)}`;

    if (this.props.isInProgress) {
      let tooltip = canSeeBackgroundTasks ?
          window.tp('component_navigation.status.in_progress.admin', backgroundTasksUrl) :
          window.t('component_navigation.status.in_progress');
      metaList.push(
          <li key="isInProgress" data-toggle="tooltip" title={tooltip}>
            <i className="spinner" style={{ marginTop: '-1px' }}/>
            {' '}
            <span className="text-info">{window.t('background_task.status.IN_PROGRESS')}</span>
          </li>
      );
    } else if (this.props.isPending) {
      let tooltip = canSeeBackgroundTasks ?
          window.tp('component_navigation.status.pending.admin', backgroundTasksUrl) :
          window.t('component_navigation.status.pending');
      metaList.push(
          <li key="isPending" data-toggle="tooltip" title={tooltip}>
            <PendingIcon/> <span>{window.t('background_task.status.PENDING')}</span>
          </li>
      );
    } else if (this.props.isFailed) {
      let tooltip = canSeeBackgroundTasks ?
          window.tp('component_navigation.status.failed.admin', backgroundTasksUrl) :
          window.t('component_navigation.status.failed');
      metaList.push(
          <li key="isFailed" data-toggle="tooltip" title={tooltip}>
            <span className="badge badge-danger">{window.t('background_task.status.FAILED')}</span>
          </li>
      );
    }

    if (this.props.snapshotDate) {
      metaList.push(<li key="snapshotDate">{moment(this.props.snapshotDate).format('LLL')}</li>);
    }

    if (this.props.version) {
      metaList.push(<li key="version">Version {this.props.version}</li>);
    }

    return (
        <div className="navbar-right navbar-context-meta">
          <ul className="list-inline">{metaList}</ul>
        </div>
    );
  }
});
