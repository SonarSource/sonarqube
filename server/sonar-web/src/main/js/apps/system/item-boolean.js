import React from 'react';

export default React.createClass({
  render() {
    if (this.props.value) {
      return <i className="icon-check"/>;
    } else {
      return <i className="icon-delete"/>;
    }
  }
});
