/*
 * SonarQube :: Web
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import React, { Component } from 'react';
import { connect } from 'react-redux';

import { search } from '../actions';
import { translate } from '../../../helpers/l10n';


class Search extends Component {
  componentDidMount () {
    this.focusSearchInput();
  }

  componentDidUpdate () {
    this.focusSearchInput();
  }

  focusSearchInput () {
    if (this.refs.input) {
      this.refs.input.focus();
    }
  }

  handleSearch (e) {
    e.preventDefault();
    const { dispatch, component } = this.props;
    const query = this.refs.input ? this.refs.input.value : '';
    dispatch(search(query, component));
  }

  handleStopSearch (e) {
    e.preventDefault();
    const { dispatch } = this.props;
    dispatch(search(null));
  }

  handleKeyDown (e) {
    const { dispatch } = this.props;

    // "escape" key
    if (e.keyCode === 27) {
      dispatch(search(null));
    }
  }

  render () {
    const { query } = this.props;
    const hasQuery = query != null;

    return (
        <form
            onSubmit={this.handleSearch.bind(this)}
            className="search-box code-search-box">
          {hasQuery && (
              <input
                  ref="input"
                  onChange={this.handleSearch.bind(this)}
                  onKeyDown={this.handleKeyDown.bind(this)}
                  value={query}
                  className="search-box-input"
                  type="search"
                  name="q"
                  placeholder="Search"
                  maxLength="100"
                  autoComplete="off"
                  style={{ visibility: hasQuery ? 'visible': 'hidden' }}/>
          )}
          {!hasQuery && (
              <button className="search-box-submit">
                {translate('search_verb')}
              </button>
          )}
          {hasQuery && (
              <button
                  className="search-box-submit"
                  onClick={this.handleStopSearch.bind(this)}>
                {translate('cancel')}
              </button>
          )}
        </form>
    );
  }
}


export default connect(state => {
  return { query: state.current.searchQuery };
})(Search);
