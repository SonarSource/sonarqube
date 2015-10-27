import React from 'react';
import classNames from 'classnames';

export default React.createClass({
  render() {
    return <li className="overview-card">{this.props.children}</li>;
  }
});
