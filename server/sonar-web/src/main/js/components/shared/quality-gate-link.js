import React from 'react';


export const QualityGateLink = React.createClass({
  render() {
    let url = `${baseUrl}/quality_gates/show/${this.props.gate}`;
    return <a href={url}>{this.props.children}</a>;
  }
});
