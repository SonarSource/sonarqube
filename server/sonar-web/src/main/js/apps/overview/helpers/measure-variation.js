import React from 'react';
import { formatMeasureVariation } from '../../../helpers/measures';


export default React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = formatMeasureVariation(this.props.value, this.props.type);
    return <span>{formatted}</span>;
  }
});
