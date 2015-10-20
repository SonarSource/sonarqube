import React from 'react';
import SeverityIcon from './severity-icon';

export default React.createClass({
  render() {
    if (!this.props.severity) {
      return null;
    }
    return <span>
      <span className="spacer-right">
        <SeverityIcon severity={this.props.severity}/>
      </span>
      {window.t('severity', this.props.severity)}
    </span>;
  }
});
