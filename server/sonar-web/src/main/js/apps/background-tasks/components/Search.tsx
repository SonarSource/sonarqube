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

import { Button, Label } from '@sonarsource/echoes-react';
import * as React from 'react';
import { InputSearch } from '~design-system';
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
  maxExecutedAt: Date | undefined;
  minSubmittedAt: Date | undefined;
  onFilterUpdate: (changes: Partial<Query>) => void;
  onReload: () => void;
  query: string;
  status: string;
  taskType: string;
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

  handleReload = () => {
    this.props.onReload();
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
            <Label
              id="background-task-status-filter-label"
              className="sw-mr-2"
              htmlFor="status-filter"
            >
              {translate('status')}
            </Label>

            <StatusFilter id="status-filter" onChange={this.handleStatusChange} value={status} />
          </li>

          {types.length > 1 && (
            <li>
              <Label
                id="background-task-type-filter-label"
                className="sw-mr-2"
                htmlFor="types-filter"
              >
                {translate('type')}
              </Label>

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
              <Label className="sw-mr-2">
                {translate('background_tasks.currents_filter.ONLY_CURRENTS')}
              </Label>

              <CurrentsFilter onChange={this.handleCurrentsChange} value={currents} />
            </li>
          )}

          <li className="sw-flex sw-items-center">
            <Label className="sw-mr-2">{translate('background_tasks.date_filter')}</Label>

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
            <Button className="js-reload sw-mr-2" isDisabled={loading} onClick={this.handleReload}>
              {translate('reload')}
            </Button>

            <Button isDisabled={loading} onClick={this.handleReset}>
              {translate('reset_verb')}
            </Button>
          </li>
        </ul>
      </section>
    );
  }
}
