import React from 'react';
import CreateView from './create-view';

export default React.createClass({
  propTypes: {
    hasProvisionPermission: React.PropTypes.bool.isRequired
  },

  createProject() {
    new CreateView({
      refresh: this.props.refresh
    }).render();
  },

  renderCreateButton() {
    if (!this.props.hasProvisionPermission) {
      return null;
    }
    return <button onClick={this.createProject}>Create Project</button>;
  },

  render() {
    return (
        <header className="page-header">
          <h1 className="page-title">Projects Management</h1>
          <div className="page-actions">{this.renderCreateButton()}</div>
          <p className="page-description">Use this page to delete multiple projects at once, or to provision projects
            if you would like to configure them before the first analysis. Note that once a project is provisioned, you
            have access to perform all project configurations on it.</p>
        </header>
    );
  }
});
