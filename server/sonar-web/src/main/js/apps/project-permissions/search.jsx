import _ from 'underscore';
import React from 'react';

export default React.createClass({
  propTypes: {
    search: React.PropTypes.func.isRequired
  },

  componentWillMount: function () {
    this.search = _.debounce(this.search, 250);
  },

  onSubmit(e) {
    e.preventDefault();
    this.search();
  },

  search() {
    let q = React.findDOMNode(this.refs.input).value;
    this.props.search(q);
  },

  render() {
    return (
        <div className="panel panel-vertical bordered-bottom spacer-bottom">
          <form onSubmit={this.onSubmit} className="search-box">
            <button className="search-box-submit button-clean">
              <i className="icon-search"></i>
            </button>
            <input onChange={this.search} ref="input" className="search-box-input" type="search" placeholder="Search"/>
          </form>
        </div>
    );
  }
});
