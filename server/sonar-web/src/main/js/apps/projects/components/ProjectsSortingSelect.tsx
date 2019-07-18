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
import { sortBy } from 'lodash';
import * as React from 'react';
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import Select from 'sonar-ui-common/components/controls/Select';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import SortAscIcon from 'sonar-ui-common/components/icons/SortAscIcon';
import SortDescIcon from 'sonar-ui-common/components/icons/SortDescIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../app/theme';
import { parseSorting, SORTING_LEAK_METRICS, SORTING_METRICS } from '../utils';
import ProjectsSortingSelectOption, { Option } from './ProjectsSortingSelectOption';

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
            color={colors.gray60}
            onClick={this.handleDescToggle}>
            {sortDesc ? <SortDescIcon className="" /> : <SortAscIcon className="" />}
          </ButtonIcon>
        </Tooltip>
      </div>
    );
  }
}
