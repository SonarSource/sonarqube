import React from 'react';

export class DomainHeader extends React.Component {
  render () {
    return <h2 className="overview-title">{this.props.title}</h2>;
  }
}
