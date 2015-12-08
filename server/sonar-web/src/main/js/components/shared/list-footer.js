import classNames from 'classnames';
import React from 'react';


export default React.createClass({
  propTypes: {
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired,
    loadMore: React.PropTypes.func,
    ready: React.PropTypes.bool
  },

  getDefaultProps() {
    return { ready: true };
  },

  canLoadMore() {
    return typeof this.props.loadMore === 'function';
  },

  handleLoadMore(e) {
    e.preventDefault();
    if (this.canLoadMore()) {
      this.props.loadMore();
    }
  },

  renderLoading() {
    return <footer className="spacer-top note text-center">
      {window.t('loading')}
    </footer>;
  },

  render() {
    let hasMore = this.props.total > this.props.count;
    let loadMoreLink = <a onClick={this.handleLoadMore} className="spacer-left" href="#">show more</a>;
    let className = classNames('spacer-top note text-center', { 'new-loading': !this.props.ready });
    return (
        <footer className={className}>
          {this.props.count}/{this.props.total} shown
          {this.canLoadMore() && hasMore ? loadMoreLink : null}
        </footer>
    );
  }
});
