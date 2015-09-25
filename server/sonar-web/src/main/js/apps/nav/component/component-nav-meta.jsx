import React from 'react';
import PendingIcon from '../../../components/shared/pending-icon';

export default React.createClass({
  render() {
    let metaList = [];

    if (this.props.isInProgress) {
      metaList.push(
          <li key="isInProgress" data-toggle="tooltip" title={window.t('component_navigation.status.in_progress')}>
            <i className="spinner" style={{ marginTop: '-1px' }}/> <span className="text-info">{window.t('background_task.status.IN_PROGRESS')}</span>
          </li>
      );
    } else if (this.props.isPending) {
      metaList.push(
          <li key="isPending" data-toggle="tooltip" title={window.t('component_navigation.status.pending')}>
            <PendingIcon/> <span>{window.t('background_task.status.PENDING')}</span>
          </li>
      );
    } else if (this.props.isFailed) {
      metaList.push(
          <li key="isFailed" data-toggle="tooltip" title={window.t('component_navigation.status.failed')}>
            <i className="icon-test-status-error"/> <span className="text-danger">{window.t('background_task.status.FAILED')}</span>
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
