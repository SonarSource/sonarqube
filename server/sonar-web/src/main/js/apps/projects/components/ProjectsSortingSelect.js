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
// @flow
import React from 'react';
import { sortBy } from 'lodash';
import Select from 'react-select';
import ProjectsSortingSelectOption from './ProjectsSortingSelectOption';
import SortAscIcon from '../../../components/icons-components/SortAscIcon';
import SortDescIcon from '../../../components/icons-components/SortDescIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { SORTING_METRICS, SORTING_LEAK_METRICS, parseSorting } from '../utils';

export type Option = { label: string, value: string, class?: string, short?: string };

type Props = {
  className?: string,
  onChange: (sort: string, desc: boolean) => void,
  selectedSort: string,
  view: string,
  defaultOption: string
};

type State = {
  sortValue: string,
  sortDesc: boolean
};

export default class ProjectsSortingSelect extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = parseSorting(props.selectedSort);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.selectedSort !== this.props.selectedSort) {
      this.setState(parseSorting(this.props.selectedSort));
    }
  }

  getOptions = () => {
    const sortMetrics = this.props.view === 'leak' ? SORTING_LEAK_METRICS : SORTING_METRICS;
    return sortBy(
      sortMetrics,
      opt => (opt.value === this.props.defaultOption ? 0 : 1)
    ).map((opt: { value: string, class?: string }) => ({
      value: opt.value,
      label: translate('projects.sorting', opt.value),
      class: opt.class
    }));
  };

  handleDescToggle = (evt: Event & { currentTarget: HTMLElement }) => {
    evt.preventDefault();
    evt.currentTarget.blur();
    this.props.onChange(this.state.sortValue, !this.state.sortDesc);
  };

  handleSortChange = (option: Option) => this.props.onChange(option.value, this.state.sortDesc);

  render() {
    const { sortDesc } = this.state;

    return (
      <div className={this.props.className}>
        <label>{translate('projects.sort_by')}:</label>
        <Select
          className="little-spacer-left input-medium"
          clearable={false}
          onChange={this.handleSortChange}
          optionComponent={ProjectsSortingSelectOption}
          options={this.getOptions()}
          searchable={false}
          value={this.state.sortValue}
        />
        <Tooltip
          overlay={
            sortDesc ? translate('projects.sort_descending') : translate('projects.sort_ascending')
          }>
          <a className="spacer-left button-icon" href="#" onClick={this.handleDescToggle}>
            {sortDesc
              ? <SortDescIcon className="little-spacer-top" />
              : <SortAscIcon className="little-spacer-top" />}
          </a>
        </Tooltip>
      </div>
    );
  }
}
