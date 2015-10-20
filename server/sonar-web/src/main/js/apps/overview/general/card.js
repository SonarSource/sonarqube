import React from 'react';
import classNames from 'classnames';

export default React.createClass({
  handleClick() {
    if (this.props.linkTo) {
      let tab = React.findDOMNode(this);
      this.props.onRoute(this.props.linkTo, tab);
    }
  },

  render() {
    let classes = classNames('overview-card', {
      'overview-card-section': this.props.linkTo,
      'active': this.props.active
    });
    return <li onClick={this.handleClick} className={classes}>{this.props.children}</li>;
  }
});
