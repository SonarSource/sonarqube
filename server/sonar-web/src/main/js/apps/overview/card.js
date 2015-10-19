import React from 'react';

export default React.createClass({
  render() {
    return <li className="overview-card">{this.props.children}</li>;
  }
});
