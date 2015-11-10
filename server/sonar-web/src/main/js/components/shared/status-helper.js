import React from 'react';
import StatusIcon from './status-icon';

export default React.createClass({
  render: function () {
    if (!this.props.status) {
      return null;
    }
    var resolution;
    if (this.props.resolution) {
      resolution = ' (' + window.t('issue.resolution', this.props.resolution) + ')';
    }
    return (
        <span>
            <StatusIcon status={this.props.status}/>
          &nbsp;
          {window.t('issue.status', this.props.status)}
          {resolution}
          </span>
    );
  }
});
