import React from 'react';
import { setLogLevel } from '../../api/system';

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

export default React.createClass({
  getInitialState () {
    return { level: this.props.value };
  },

  onChange() {
    let newValue = this.refs.select.value;
    setLogLevel(newValue).then(() => {
      this.setState({ level: newValue });
    });
  },

  render() {
    let options = LOG_LEVELS.map(level => {
      return <option key={level} value={level}>{level}</option>;
    });
    let warning = this.state.level !== 'INFO' ? (
        <div className="alert alert-danger spacer-top" style={{ wordBreak: 'normal' }}>
          {window.t('system.log_level.warning')}
        </div>
    ) : null;
    return <div>
      <select ref="select"
              onChange={this.onChange}
              value={this.state.level}>{options}</select>
      {warning}
    </div>;
  }
});
