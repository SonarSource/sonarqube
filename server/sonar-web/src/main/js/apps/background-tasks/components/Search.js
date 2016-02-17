/*
 * SonarQube
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
import React from 'react';

import StatusFilter from './StatusFilter';
import TypesFilter from './TypesFilter';
import CurrentsFilter from './CurrentsFilter';
import DateFilter from './DateFilter';
import { translate } from '../../../helpers/l10n';

export default React.createClass({

  onSearchFormSubmit(e) {
    e.preventDefault();
    this.onSearch();
  },

  onSearch() {
    let searchInput = this.refs.searchInput;
    let query = searchInput.value;
    this.props.onSearch(query);
  },

  renderSearchBox() {
    if (this.props.component) {
      // do not render search form on the project-level page
      return null;
    }
    return (
        <li>
          <h6 className="bt-search-form-label">
            Component
          </h6>

          <input onChange={this.onSearch}
                 value={this.props.query}
                 ref="searchInput"
                 className="js-search input-large"
                 type="search"
                 placeholder="Search"/>
        </li>
    );
  },

  refresh(e) {
    e.preventDefault();
    this.props.onRefresh();
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <ul className="bt-search-form">
            <li>
              <h6 className="bt-search-form-label">
                Status
              </h6>
              <StatusFilter
                  value={this.props.status}
                  onChange={this.props.onStatusChange}/>
            </li>
            {this.props.types.length > 1 && (
                <li>
                  <h6 className="bt-search-form-label">
                    Type
                  </h6>
                  <TypesFilter
                      value={this.props.taskType}
                      onChange={this.props.onTypeChange}
                      types={this.props.types}/>
                </li>
            )}
            <li>
              <h6 className="bt-search-form-label">
                Only Latest Analysis
              </h6>
              <CurrentsFilter
                  value={this.props.currents}
                  onChange={this.props.onCurrentsChange}/>
            </li>
            <li>
              <h6 className="bt-search-form-label">
                Date
              </h6>
              <DateFilter
                  value={this.props.date}
                  onChange={this.props.onDateChange}/>
            </li>

            {this.renderSearchBox()}

            <li className="bt-search-form-right">
              <button
                  ref="reloadButton"
                  onClick={this.refresh}
                  disabled={this.props.fetching}>
                {translate('reload')}
              </button>
            </li>
          </ul>
        </section>
    );
  }
});
