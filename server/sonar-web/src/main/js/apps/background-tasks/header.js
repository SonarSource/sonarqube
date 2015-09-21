import React from 'react';

export default React.createClass({
  render() {
    return (
        <header className="page-header">
          <h1 className="page-title">Background Tasks</h1>
          <p className="page-description">The server is in charge to process reports submitted by batch analyses. This
            page allows to monitor the queue of pending reports to process, and gives access to the history of past
            analyses.</p>
        </header>

    );
  }
});
