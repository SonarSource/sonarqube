import React from 'react';

export default React.createClass({
  render() {
    let qualityGatesUrl = window.baseUrl + '/quality_gates';

    return (
        <div className="overview-gate">
          <h2 className="overview-title">{window.t('overview.quality_gate')}</h2>
          <p className="overview-gate-warning">
            You should <a href={qualityGatesUrl}>define</a> a quality gate on this project.</p>
        </div>
    );
  }
});
