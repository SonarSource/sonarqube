import React from 'react';

export default React.createClass({
  render() {
    const version = this.props.version ? `Version ${this.props.version}` : null;
    const snapshotDate = this.props.snapshotDate ? moment(this.props.snapshotDate).format('LLL') : null;
    return (
        <div className="navbar-right navbar-context-meta">
          {version} {snapshotDate}
        </div>
    );
  }
});
