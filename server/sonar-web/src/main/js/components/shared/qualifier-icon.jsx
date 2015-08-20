import React from 'react';

export default React.createClass({
  render() {
    if (!this.props.qualifier) {
      return null;
    }
    var className = 'icon-qualifier-' + this.props.qualifier.toLowerCase();
    return <i className={className}/>;
  }
});
