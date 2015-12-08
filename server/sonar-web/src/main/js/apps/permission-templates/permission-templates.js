import classNames from 'classnames';
import React from 'react';

import PermissionsHeader from './permissions-header';
import PermissionTemplate from './permission-template';


export default React.createClass({
  propTypes: {
    permissionTemplates: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    topQualifiers: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  render() {
    let permissionTemplates = this.props.permissionTemplates.map(p => {
      return <PermissionTemplate
          key={p.id}
          permissionTemplate={p}
          topQualifiers={this.props.topQualifiers}
          refresh={this.props.refresh}/>;
    });
    let className = classNames('data zebra', { 'new-loading': !this.props.ready });
    return (
        <table id="permission-templates" className={className}>
          <PermissionsHeader permissions={this.props.permissions}/>
          <tbody>{permissionTemplates}</tbody>
        </table>
    );
  }
});
