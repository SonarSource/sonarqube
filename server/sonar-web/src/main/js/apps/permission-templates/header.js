import React from 'react';
import CreateView from './create-view';

export default React.createClass({
  onCreate(e) {
    e.preventDefault();
    new CreateView({
      refresh: this.props.refresh
    }).render();
  },

  renderSpinner () {
    if (this.props.ready) {
      return null;
    }
    return <i className="spinner"/>;
  },

  render() {
    return (
        <header id="project-permissions-header" className="page-header">
          <h1 className="page-title">{window.t('permission_templates.page')}</h1>
          {this.renderSpinner()}
          <div className="page-actions">
            <button onClick={this.onCreate}>Create</button>
          </div>
          <p className="page-description">{window.t('roles.page.description')}</p>
        </header>
    );
  }
});
