/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/* @flow */
import React from 'react';
import PropTypes from 'prop-types';
import StatusFilter from './StatusFilter';
import TypesFilter from './TypesFilter';
import CurrentsFilter from './CurrentsFilter';
import DateFilter from './DateFilter';
import { DEFAULT_FILTERS } from './../constants';
import SearchBox from '../../../components/controls/SearchBox';
import { translate } from '../../../helpers/l10n';

export default class Search extends React.PureComponent {
  static propTypes = {
    loading: PropTypes.bool.isRequired,
    status: PropTypes.any.isRequired,
    taskType: PropTypes.any.isRequired,
    currents: PropTypes.any.isRequired,
    query: PropTypes.string.isRequired,
    onFilterUpdate: PropTypes.func.isRequired,
    onReload: PropTypes.func.isRequired
  };

  handleStatusChange(status /*: string */) {
    this.props.onFilterUpdate({ status });
  }

  handleTypeChange(taskType /*: string */) {
    this.props.onFilterUpdate({ taskType });
  }

  handleCurrentsChange(currents /*: string */) {
    this.props.onFilterUpdate({ currents });
  }

  handleDateChange(date /*: string */) {
    this.props.onFilterUpdate(date);
  }

  handleQueryChange = (query /*: string */) => {
    this.props.onFilterUpdate({ query });
  };

  handleReload(e /*: Object */) {
    e.target.blur();
    this.props.onReload();
  }

  handleReset(e /*: Object */) {
    e.preventDefault();
    e.target.blur();
    this.props.onFilterUpdate(DEFAULT_FILTERS);
  }

  renderSearchBox() {
    const { component, query } = this.props;

    if (component) {
      // do not render search form on the project-level page
      return null;
    }

    return (
      <li className="bt-search-form-large">
        <SearchBox
          onChange={this.handleQueryChange}
          placeholder={translate('background_tasks.search_by_task_or_component')}
          value={query}
        />
      </li>
    );
  }

  render() {
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
            <h6 className="bt-search-form-label">{translate('status')}</h6>
            <StatusFilter value={status} onChange={this.handleStatusChange.bind(this)} />
          </li>
          {types.length > 1 && (
            <li>
              <h6 className="bt-search-form-label">{translate('type')}</h6>
              <TypesFilter
                value={taskType}
                types={types}
                onChange={this.handleTypeChange.bind(this)}
              />
            </li>
          )}
          {!component && (
            <li>
              <h6 className="bt-search-form-label">
                {translate('background_tasks.currents_filter.ONLY_CURRENTS')}
              </h6>
              <CurrentsFilter value={currents} onChange={this.handleCurrentsChange.bind(this)} />
            </li>
          )}
          <li>
            <h6 className="bt-search-form-label">{translate('date')}</h6>
            <DateFilter
              minSubmittedAt={minSubmittedAt}
              maxExecutedAt={maxExecutedAt}
              onChange={this.handleDateChange.bind(this)}
            />
          </li>

          {this.renderSearchBox()}

          <li className="nowrap">
            <button className="js-reload" onClick={this.handleReload.bind(this)} disabled={loading}>
              {translate('reload')}
            </button>{' '}
            <button ref="resetButton" onClick={this.handleReset.bind(this)} disabled={loading}>
              {translate('reset_verb')}
            </button>
          </li>
        </ul>
      </section>
    );
  }
}
