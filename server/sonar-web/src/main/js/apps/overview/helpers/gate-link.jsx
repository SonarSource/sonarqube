import React from 'react';

export default React.createClass({
  render() {
    const url = `${baseUrl}/quality_gates/show/${this.props.gate}`;
    return <a href={url}>{this.props.children}</a>;
  }
});
