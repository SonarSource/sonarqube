import React from 'react';


export const QualityProfileLink = React.createClass({
  render() {
    let url = `${baseUrl}/profiles/show?key=${encodeURIComponent(this.props.profile)}`;
    return <a href={url}>{this.props.children}</a>;
  }
});
