/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import { translate } from 'sonar-ui-common/helpers/l10n';
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
            <StatusFilter onChange={this.handleStatusChange} value={status} />
          </li>
          {types.length > 1 && (
            <li>
              <h6 className="bt-search-form-label">{translate('type')}</h6>
              <TypesFilter onChange={this.handleTypeChange} types={types} value={taskType} />
            </li>
          )}
          {!component && (
            <li>
              <h6 className="bt-search-form-label">
                {translate('background_tasks.currents_filter.ONLY_CURRENTS')}
              </h6>
              <CurrentsFilter onChange={this.handleCurrentsChange} value={currents} />
            </li>
          )}
          <li>
            <h6 className="bt-search-form-label">{translate('date')}</h6>
            <DateFilter
              maxExecutedAt={maxExecutedAt}
              minSubmittedAt={minSubmittedAt}
              onChange={this.handleDateChange}
            />
          </li>

          {this.renderSearchBox()}

          <li className="nowrap">
            <Button className="js-reload" disabled={loading} onClick={this.props.onReload}>
              {translate('reload')}
            </Button>{' '}
            <Button disabled={loading} onClick={this.handleReset}>
              {translate('reset_verb')}
            </Button>
          </li>
        </ul>
      </section>
    );
  }
}
