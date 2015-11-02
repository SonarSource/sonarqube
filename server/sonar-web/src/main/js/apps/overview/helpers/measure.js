import React from 'react';
import { formatMeasure } from '../../../helpers/measures';

export default React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = formatMeasure(this.props.value, this.props.type);
    return <span>{formatted}</span>;
  }
});
