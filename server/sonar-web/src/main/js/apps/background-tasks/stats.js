import React from 'react';

export default React.createClass({
  onFailuresClick(e) {
    e.preventDefault();
    this.props.showFailures();
  },

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

  renderFailures() {
    if (this.props.failuresCount == null) {
      return null;
    }
    if (this.props.failuresCount > 0) {
      return <span><a onClick={this.onFailuresClick} className="emphasised-measure" href="#">{this.props.failuresCount}</a> failures</span>;
    } else {
      return <span><span className="emphasised-measure">{this.props.failuresCount}</span> failures</span>;
    }
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <span>
            <span className="emphasised-measure">{this.props.pendingCount}</span> pending
          </span>
          <span className="huge-spacer-left">
            {this.renderFailures()}
          </span>
          {this.renderInProgressDuration()}
        </section>

    );
  }
});
