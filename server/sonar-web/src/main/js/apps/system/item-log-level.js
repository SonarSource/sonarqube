import React from 'react';
import { setLogLevel } from '../../api/system';

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

export default React.createClass({
  onChange() {
    let newValue = React.findDOMNode(this.refs.select).value;
    setLogLevel(newValue);
  },

  render() {
    let options = LOG_LEVELS.map(level => {
      return <option key={level} value={level}>{level}</option>;
    });
    return <select ref="select"
                   onChange={this.onChange}
                   defaultValue={this.props.value}>{options}</select>;
  }
});
