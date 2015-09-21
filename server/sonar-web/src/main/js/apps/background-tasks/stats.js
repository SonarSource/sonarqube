import React from 'react';

export default React.createClass({
  renderInProgressDuration() {
    if (!this.props.inProgressDuration) {
      return null;
    }
    return (
        <span className="huge-spacer-left">
          <i className="spinner spacer-right" style={{ verticalAlign: 'text-top' }}/>
          <span className="emphasised-measure">{this.props.inProgressDuration} ms</span>
        </span>
    );
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <span>
            <span className="emphasised-measure">{this.props.pendingCount}</span> pending
          </span>
          <span className="huge-spacer-left">
            <span className="emphasised-measure">{this.props.failuresCount}</span> failures
          </span>
          {this.renderInProgressDuration()}
        </section>

    );
  }
});
