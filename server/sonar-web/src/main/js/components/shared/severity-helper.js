import React from 'react';
import SeverityIcon from './severity-icon';

export default React.createClass({
  render() {
    if (!this.props.severity) {
      return null;
    }
    return (
        <span>
            <SeverityIcon severity={this.props.severity}/>
          &nbsp;
          {window.t('severity', this.props.severity)}
          </span>
    );
  }
});
