import React from 'react';

export default React.createClass({
  render: function () {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    const formatted = window.formatMeasure(this.props.value, 'RATING');
    const className = 'rating rating-' + formatted;
    return <span className={className}>{formatted}</span>;
  }
});
