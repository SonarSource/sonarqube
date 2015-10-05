import React from 'react';

export default React.createClass({
  render() {
    return (
        <header className="page-header">
          <h1 className="page-title">{window.t('background_tasks.page')}</h1>
          <p className="page-description">{window.t('background_tasks.page.description')}</p>
        </header>
    );
  }
});
