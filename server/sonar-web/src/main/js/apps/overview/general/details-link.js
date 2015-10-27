import React from 'react';
import classNames from 'classnames';

export default React.createClass({
  handleClick(e) {
    e.preventDefault();
    this.props.onRoute(this.props.linkTo);
  },

  render() {
    let classes = classNames('overview-card', 'overview-card-section', {
      'active': this.props.active
    });
    return <li className={classes}>
      <a onClick={this.handleClick}>{window.t('overview.domain', this.props.linkTo)}</a>
    </li>;
  }
});
