import React from 'react';
import Permission from './permission';

export default React.createClass({
  propTypes:{
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  renderPermissions() {
    return this.props.permissions.map(permission => {
      return <Permission key={permission.key} permission={permission}/>
    });
  },

  render() {
    return <ul id="global-permissions-list">{this.renderPermissions()}</ul>;
  }
});
