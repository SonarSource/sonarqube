import React from 'react';

import { formatMeasure } from '../../helpers/measures';


export const Rating = React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = formatMeasure(this.props.value, 'RATING');
    let className = 'rating rating-' + formatted;
    return <span className={className}>{formatted}</span>;
  }
});
