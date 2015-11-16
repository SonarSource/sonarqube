import $ from 'jquery';
import React from 'react';
import PermissionsList from './permissions-list';

export default React.createClass({
  getInitialState() {
    return { ready: false, permissions: [] };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  requestPermissions() {
    const url = `${window.baseUrl}/api/permissions/search_global_permissions`;
    $.get(url).done(r => {
      this.setState({ ready: true, permissions: r.permissions });
    });
  },

  renderSpinner () {
    if (this.state.ready) {
      return null;
    }
    return <i className="spinner"/>;
  },

  render() {
    return (
        <div className="page">
          <header id="global-permissions-header" className="page-header">
            <h1 className="page-title">{window.t('global_permissions.page')}</h1>
            {this.renderSpinner()}
            <p className="page-description">{window.t('global_permissions.page.description')}</p>
          </header>
          <PermissionsList ready={this.state.ready} permissions={this.state.permissions}/>
        </div>
    );
  }
});
