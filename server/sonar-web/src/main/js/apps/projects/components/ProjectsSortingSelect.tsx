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
import { sortBy } from 'lodash';
import ProjectsSortingSelectOption, { Option } from './ProjectsSortingSelectOption';
import * as theme from '../../../app/theme';
import SortAscIcon from '../../../components/icons-components/SortAscIcon';
import SortDescIcon from '../../../components/icons-components/SortDescIcon';
import Select from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import { ButtonIcon } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { SORTING_METRICS, SORTING_LEAK_METRICS, parseSorting } from '../utils';

interface Props {
  className?: string;
  defaultOption: string;
  onChange: (sort: string, desc: boolean) => void;
  selectedSort: string;
  view: string;
}

export default class ProjectsSortingSelect extends React.PureComponent<Props> {
  getSorting = () => parseSorting(this.props.selectedSort);

  getOptions = () => {
    const sortMetrics = this.props.view === 'leak' ? SORTING_LEAK_METRICS : SORTING_METRICS;
    return sortBy(sortMetrics, option => (option.value === this.props.defaultOption ? 0 : 1)).map(
      option => ({
        value: option.value,
        label: translate('projects.sorting', option.value),
        class: option.class
      })
    );
  };

  handleDescToggle = () => {
    const sorting = this.getSorting();
    this.props.onChange(sorting.sortValue, !sorting.sortDesc);
  };

  handleSortChange = (option: Option) =>
    this.props.onChange(option.value, this.getSorting().sortDesc);

  render() {
    const { sortDesc, sortValue } = this.getSorting();

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
          value={sortValue}
        />
        <Tooltip
          overlay={
            sortDesc ? translate('projects.sort_descending') : translate('projects.sort_ascending')
          }>
          <ButtonIcon
            className="js-projects-sorting-invert spacer-left"
            color={theme.gray60}
            onClick={this.handleDescToggle}>
            {sortDesc ? <SortDescIcon className="" /> : <SortAscIcon className="" />}
          </ButtonIcon>
        </Tooltip>
      </div>
    );
  }
}
