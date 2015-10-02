import React from 'react';

export default React.createClass({
  propTypes: {
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired,
    loadMore: React.PropTypes.func
  },

  canLoadMore() {
    return typeof this.props.loadMore === 'function';
  },

  loadMoreProxy(e) {
    e.preventDefault();
    if (this.canLoadMore()) {
      this.props.loadMore();
    }
  },

  render() {
    let hasMore = this.props.total > this.props.count,
        loadMoreLink = <a onClick={this.loadMoreProxy} className="spacer-left" href="#">show more</a>;
    return (
        <footer className="spacer-top note text-center">
          {this.props.count}/{this.props.total} shown
          {this.canLoadMore() && hasMore ? loadMoreLink : null}
        </footer>
    );
  }
});
