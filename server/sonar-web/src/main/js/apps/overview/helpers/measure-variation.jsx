import React from 'react';

export default React.createClass({
  render: function () {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    var formatted = window.formatMeasureVariation(this.props.value, this.props.type);
    return <span>{formatted}</span>;
  }
});
