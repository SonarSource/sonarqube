import React from 'react';

export default React.createClass({
  render() {
    return (
        <div className="panel">
          <div className="alert alert-warning">
            {window.t('provisioning.no_analysis')}
          </div>
        </div>
    );
  }
});
