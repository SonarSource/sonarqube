/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ButtonSecondary, InputSearch } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { DEFAULT_FILTERS } from '../constants';
import { Query } from '../utils';
import CurrentsFilter from './CurrentsFilter';
import DateFilter from './DateFilter';
import StatusFilter from './StatusFilter';
import TypesFilter from './TypesFilter';

interface Props {
  component?: unknown;
  currents: string;
  loading: boolean;
  onFilterUpdate: (changes: Partial<Query>) => void;
  onReload: () => void;
  query: string;
  status: string;
  taskType: string;
  maxExecutedAt: Date | undefined;
  minSubmittedAt: Date | undefined;
  types: string[];
}

export default class Search extends React.PureComponent<Props> {
  handleStatusChange = (status: string) => {
    this.props.onFilterUpdate({ status });
  };

  handleTypeChange = (taskType: string) => {
    this.props.onFilterUpdate({ taskType });
  };

  handleCurrentsChange = (currents: string) => {
    this.props.onFilterUpdate({ currents });
  };

  handleDateChange = (date: { maxExecutedAt?: Date; minSubmittedAt?: Date }) => {
    this.props.onFilterUpdate(date);
  };

  handleQueryChange = (query: string) => {
    this.props.onFilterUpdate({ query });
  };

  handleReset = () => {
    this.props.onFilterUpdate(DEFAULT_FILTERS);
  };

  render() {
    const {
      loading,
      component,
      query,
      types,
      status,
      taskType,
      currents,
      minSubmittedAt,
      maxExecutedAt,
    } = this.props;

    return (
      <section className="sw-my-4">
        <ul className="sw-flex sw-items-center sw-flex-wrap sw-gap-4">
          <li>
            <label
              id="background-task-status-filter-label"
              className="sw-body-sm-highlight sw-mr-2"
              htmlFor="status-filter"
            >
              {translate('status')}
            </label>
            <StatusFilter id="status-filter" onChange={this.handleStatusChange} value={status} />
          </li>
          {types.length > 1 && (
            <li>
              <label
                id="background-task-type-filter-label"
                className="sw-body-sm-highlight sw-mr-2"
                htmlFor="types-filter"
              >
                {translate('type')}
              </label>
              <TypesFilter
                id="types-filter"
                onChange={this.handleTypeChange}
                types={types}
                value={taskType}
              />
            </li>
          )}
          {!component && (
            <li className="sw-flex sw-items-center">
              <label className="sw-body-sm-highlight sw-mr-2">
                {translate('background_tasks.currents_filter.ONLY_CURRENTS')}
              </label>
              <CurrentsFilter onChange={this.handleCurrentsChange} value={currents} />
            </li>
          )}
          <li className="sw-flex sw-items-center">
            <label className="sw-body-sm-highlight sw-mr-2">
              {translate('background_tasks.date_filter')}
            </label>
            <DateFilter
              maxExecutedAt={maxExecutedAt}
              minSubmittedAt={minSubmittedAt}
              onChange={this.handleDateChange}
            />
          </li>

          {!component && (
            <li>
              <InputSearch
                onChange={this.handleQueryChange}
                placeholder={translate('background_tasks.search_by_task_or_component')}
                value={query}
              />
            </li>
          )}

          <li>
            <ButtonSecondary
              className="js-reload sw-mr-2"
              disabled={loading}
              onClick={this.props.onReload}
            >
              {translate('reload')}
            </ButtonSecondary>
            <ButtonSecondary disabled={loading} onClick={this.handleReset}>
              {translate('reset_verb')}
            </ButtonSecondary>
          </li>
        </ul>
      </section>
    );
  }
}
