import React from 'react';

export default React.createClass({
  render() {
    if (!this.props.severity) {
      return null;
    }
    var className = 'icon-severity-' + this.props.severity.toLowerCase();
    return <i className={className}></i>;
  }
});
