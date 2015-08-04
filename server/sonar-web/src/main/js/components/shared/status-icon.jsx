import React from 'react';

export default React.createClass({
  render() {
    if (!this.props.status) {
      return null;
    }
    var className = 'icon-status-' + this.props.status.toLowerCase();
    return <i className={className}></i>;
  }
});
