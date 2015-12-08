import React from 'react';
import ItemBoolean from './item-boolean';
import ItemObject from './item-object';
import ItemLogLevel from './item-log-level';

export default React.createClass({
  render() {
    if (this.props.name === 'Logs Level') {
      return <ItemLogLevel value={this.props.value}/>;
    }

    let rawValue = this.props.value;
    let formattedValue;
    switch (typeof this.props.value) {
      case 'boolean':
        formattedValue = <ItemBoolean value={rawValue}/>;
        break;
      case 'object':
        formattedValue = <ItemObject value={rawValue}/>;
        break;
      default:
        formattedValue = <code>{rawValue}</code>;
    }
    return formattedValue;
  }
});
