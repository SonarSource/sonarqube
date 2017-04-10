/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import React from 'react';
import RadioToggle from '../../../../components/controls/RadioToggle';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export default class SearchForm extends React.Component {
  static propTypes = {
    query: React.PropTypes.string,
    filter: React.PropTypes.oneOf(['all', 'users', 'groups']),
    onSearch: React.PropTypes.func,
    onFilter: React.PropTypes.func
  };

  componentWillMount() {
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleSearch = this.handleSearch.bind(this);
  }

  handleSubmit(e) {
    e.preventDefault();
    this.handleSearch();
  }

  handleSearch() {
    const { value } = this.refs.searchInput;
    this.props.onSearch(value);
  }

  handleFilter(filter) {
    this.props.onFilter(filter);
  }

  render() {
    const { query, filter } = this.props;

    const filterOptions = [
      { value: 'all', label: 'All' },
      { value: 'users', label: 'Users' },
      { value: 'groups', label: 'Groups' }
    ];

    return (
      <div>

        <RadioToggle
          value={filter}
          options={filterOptions}
          name="users-or-groups"
          onCheck={this.handleFilter.bind(this)}
        />

        <form
          className="search-box display-inline-block text-middle big-spacer-left"
          onSubmit={this.handleSubmit}>
          <button className="search-box-submit button-clean">
            <i className="icon-search" />
          </button>
          <input
            ref="searchInput"
            value={query}
            className="search-box-input"
            style={{ width: 100 }}
            type="search"
            placeholder={translate('search_verb')}
            onChange={this.handleSearch.bind(this)}
          />
          {query.length > 0 &&
            query.length < 3 &&
            <div className="search-box-input-note tooltip bottom fade in">
              <div className="tooltip-inner">
                {translateWithParameters('select2.tooShort', 3)}
              </div>
              <div className="tooltip-arrow" style={{ left: 23 }} />
            </div>}
        </form>
      </div>
    );
  }
}
