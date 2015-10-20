import React from 'react';

export default React.createClass({
  render() {
    return <ul className="overview-cards">{this.props.children}</ul>;
  }
});
