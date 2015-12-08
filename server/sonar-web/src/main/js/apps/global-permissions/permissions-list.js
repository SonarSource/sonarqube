import classNames from 'classnames';
import React from 'react';

import Permission from './permission';


export default React.createClass({
  propTypes: {
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  renderPermissions() {
    return this.props.permissions.map(permission => {
      return <Permission key={permission.key} permission={permission} project={this.props.project}/>;
    });
  },

  render() {
    let className = classNames({ 'new-loading': !this.props.ready });
    return <ul id="global-permissions-list" className={className}>{this.renderPermissions()}</ul>;
  }
});
