import React from 'react';
import Checkbox from '../shared/checkbox';

export default React.createClass({
  propTypes: {
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired,
    loadMore: React.PropTypes.func.isRequired
  },

  loadMore(e) {
    e.preventDefault();
    this.props.loadMore();
  },

  renderLoadMoreLink() {
    let hasMore = this.props.total > this.props.count;
    if (!hasMore) {
      return null;
    }
    return <a onClick={this.loadMore} className="spacer-left" href="#">show more</a>;
  },

  render() {
    return (
        <footer className="spacer-top note text-center">
          {this.props.count}/{this.props.total} shown
          {this.renderLoadMoreLink()}
        </footer>
    );
  }
});
