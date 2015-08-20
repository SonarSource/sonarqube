import React from 'react';

export default React.createClass({
  propTypes:{
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired,
    loadMore: React.PropTypes.func.isRequired
  },

  render() {
    let hasMore = this.props.total > this.props.count;
    let loadMoreLink = <a onClick={this.props.loadMore} className="spacer-left" href="#">show more</a>;
    return (
        <footer className="spacer-top note text-center">
          {this.props.count}/{this.props.total} shown
          {hasMore ? loadMoreLink : null}
        </footer>
    );
  }
});
