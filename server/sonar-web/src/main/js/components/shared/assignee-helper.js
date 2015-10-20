import React from 'react';
import Avatar from './avatar';

export default class Assignee extends React.Component {
  render () {
    let avatar = this.props.user ?
        <span className="spacer-right"><Avatar email={this.props.user.email} size={16}/></span> : null;
    let name = this.props.user ? this.props.user.name : window.t('unassigned');
    return <span>{avatar}{name}</span>;
  }
}
