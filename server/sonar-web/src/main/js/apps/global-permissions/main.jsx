import $ from 'jquery';
import React from 'react';
import PermissionsList from './permissions-list';

export default React.createClass({
  getInitialState() {
    return { permissions: [] };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  requestPermissions() {
    const url = `${window.baseUrl}/api/permissions/search_global_permissions`;
    $.get(url).done(r => {
      this.setState({ permissions: r.permissions });
    });
  },

  render() {
    return (
        <div className="page">
          <header id="global-permissions-header" className="page-header">
            <h1 className="page-title">{window.t('global_permissions.page')}</h1>
            <p className="page-description">{window.t('global_permissions.page.description')}</p>
          </header>
          <PermissionsList permissions={this.state.permissions}/>
        </div>
    );
  }
});
