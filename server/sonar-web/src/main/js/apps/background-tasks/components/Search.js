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
 /* @flow */
import React from 'react';

import StatusFilter from './StatusFilter';
import TypesFilter from './TypesFilter';
import CurrentsFilter from './CurrentsFilter';
import DateFilter from './DateFilter';
import { DEFAULT_FILTERS } from './../constants';
import { translate } from '../../../helpers/l10n';

export default class Search extends React.Component {
  static propTypes = {
    loading: React.PropTypes.bool.isRequired,
    status: React.PropTypes.any.isRequired,
    taskType: React.PropTypes.any.isRequired,
    currents: React.PropTypes.any.isRequired,
    query: React.PropTypes.string.isRequired,
    onFilterUpdate: React.PropTypes.func.isRequired,
    onReload: React.PropTypes.func.isRequired
  };

  handleStatusChange (status: string) {
    this.props.onFilterUpdate({ status });
  }

  handleTypeChange (taskType: string) {
    this.props.onFilterUpdate({ taskType });
  }

  handleCurrentsChange (currents: string) {
    this.props.onFilterUpdate({ currents });
  }

  handleDateChange (date: string) {
    this.props.onFilterUpdate(date);
  }

  handleQueryChange (query: string) {
    this.props.onFilterUpdate({ query });
  }

  handleReload (e: any) {
    e.target.blur();
    this.props.onReload();
  }

  handleReset (e: any) {
    e.preventDefault();
    e.target.blur();
    this.props.onFilterUpdate(DEFAULT_FILTERS);
  }

  renderSearchBox () {
    const { component, query } = this.props;

    if (component) {
      // do not render search form on the project-level page
      return null;
    }

    return (
        <li>
          <h6 className="bt-search-form-label">
            Search by Task or Component
          </h6>

          <input
              onChange={e => this.handleQueryChange(e.target.value)}
              value={query}
              ref="searchInput"
              className="js-search input-large"
              type="search"
              placeholder="Search"/>
        </li>
    );
  }

  render () {
    const {
        loading,
        component,
        types,
        status,
        taskType,
        currents,
        minSubmittedAt,
        maxExecutedAt
    } = this.props;

    return (
        <section className="big-spacer-top big-spacer-bottom">
          <ul className="bt-search-form">
            <li>
              <h6 className="bt-search-form-label">
                Status
              </h6>
              <StatusFilter
                  value={status}
                  onChange={this.handleStatusChange.bind(this)}/>
            </li>
            {types.length > 1 && (
                <li>
                  <h6 className="bt-search-form-label">
                    Type
                  </h6>
                  <TypesFilter
                      value={taskType}
                      types={types}
                      onChange={this.handleTypeChange.bind(this)}/>
                </li>
            )}
            {!component && (
                <li>
                  <h6 className="bt-search-form-label">
                    Only Latest Analysis
                  </h6>
                  <CurrentsFilter
                      value={currents}
                      onChange={this.handleCurrentsChange.bind(this)}/>
                </li>
            )}
            <li>
              <h6 className="bt-search-form-label">
                Date
              </h6>
              <DateFilter
                  minSubmittedAt={minSubmittedAt}
                  maxExecutedAt={maxExecutedAt}
                  onChange={this.handleDateChange.bind(this)}/>
            </li>

            {this.renderSearchBox()}

            <li className="bt-search-form-right">
              <button
                  className="js-reload"
                  onClick={this.handleReload.bind(this)}
                  disabled={loading}>
                {translate('reload')}
              </button>
              {' '}
              <button
                  ref="resetButton"
                  onClick={this.handleReset.bind(this)}
                  disabled={loading}>
                {translate('reset_verb')}
              </button>
            </li>
          </ul>
        </section>
    );
  }
}
