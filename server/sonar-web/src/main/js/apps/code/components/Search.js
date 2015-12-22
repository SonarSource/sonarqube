import _ from 'underscore';
import React, { Component } from 'react';
import { connect } from 'react-redux';

import { search } from '../actions';


class Search extends Component {
  componentDidMount () {
    this.refs.input.focus();
  }

  handleSearch (e) {
    e.preventDefault();
    const { dispatch, component } = this.props;
    const query = this.refs.input.value;
    dispatch(search(query, component));
  }

  render () {
    const { query } = this.props;

    return (
        <form
            onSubmit={this.handleSearch.bind(this)}
            className="search-box code-search-box">
          <button className="search-box-submit button-clean">
            <i className="icon-search"></i>
          </button>
          <input
              ref="input"
              onChange={this.handleSearch.bind(this)}
              value={query}
              className="search-box-input"
              type="search"
              name="q"
              placeholder="Search"
              maxLength="100"
              autoComplete="off"/>
        </form>
    );
  }
}


export default connect(state => {
  return { query: state.current.searchQuery };
})(Search);
