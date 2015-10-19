import React from 'react';

export default React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = window.formatMeasure(this.props.value, 'RATING');
    let className = 'rating rating-' + formatted;
    return <span className={className}>{formatted}</span>;
  }
});
