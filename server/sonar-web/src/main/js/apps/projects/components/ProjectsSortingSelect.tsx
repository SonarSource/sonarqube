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
import { omit, sortBy } from 'lodash';
import * as React from 'react';
import { components, OptionProps } from 'react-select';
import { colors } from '../../../app/theme';
import { ButtonIcon } from '../../../components/controls/buttons';
import Select from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import SortAscIcon from '../../../components/icons/SortAscIcon';
import SortDescIcon from '../../../components/icons/SortDescIcon';
import { translate } from '../../../helpers/l10n';
import { parseSorting, SORTING_LEAK_METRICS, SORTING_METRICS } from '../utils';

interface Props {
  className?: string;
  defaultOption: string;
  onChange: (sort: string, desc: boolean) => void;
  selectedSort: string;
  view: string;
}

export interface Option {
  label: string;
  value: string;
  className?: string;
  short?: string;
}

export default class ProjectsSortingSelect extends React.PureComponent<Props> {
  sortOrderButtonNode: HTMLElement | null = null;

  getSorting = () => {
    const options = this.getOptions();
    const { sortDesc, sortValue } = parseSorting(this.props.selectedSort);
    return { sortDesc, value: options.find((o) => o.value === sortValue) };
  };

  getOptions = () => {
    const sortMetrics = this.props.view === 'leak' ? SORTING_LEAK_METRICS : SORTING_METRICS;
    return sortBy(sortMetrics, (option) => (option.value === this.props.defaultOption ? 0 : 1)).map(
      (option) => ({
        value: option.value,
        label: translate('projects.sorting', option.value),
        className: option.class,
      })
    );
  };

  handleDescToggle = () => {
    const { sortDesc, sortValue } = parseSorting(this.props.selectedSort);
    this.props.onChange(sortValue, !sortDesc);
    if (this.sortOrderButtonNode) {
      this.sortOrderButtonNode.focus();
    }
  };

  handleSortChange = (option: Option) => {
    this.props.onChange(option.value, this.getSorting().sortDesc);
  };

  projectsSortingSelectOption = (props: OptionProps<Option, false>) => {
    const { data, children } = props;
    return (
      <components.Option
        {...omit(props, ['children'])}
        className={`it__project-sort-option-${data.value} ${data.className}`}
      >
        {data.short ? data.short : children}
      </components.Option>
    );
  };

  render() {
    const { sortDesc, value } = this.getSorting();

    return (
      <div className={this.props.className}>
        <label id="aria-projects-sort">{translate('projects.sort_by')}:</label>
        <Select
          aria-labelledby="aria-projects-sort"
          className="little-spacer-left input-medium it__projects-sort-select"
          isClearable={false}
          onChange={this.handleSortChange}
          components={{
            Option: this.projectsSortingSelectOption,
          }}
          options={this.getOptions()}
          isSearchable={false}
          value={value}
        />
        <Tooltip
          mouseLeaveDelay={1}
          overlay={
            sortDesc ? translate('projects.sort_descending') : translate('projects.sort_ascending')
          }
        >
          <ButtonIcon
            aria-label={
              sortDesc
                ? translate('projects.sort_descending')
                : translate('projects.sort_ascending')
            }
            className="js-projects-sorting-invert spacer-left"
            color={colors.gray52}
            onClick={this.handleDescToggle}
            innerRef={(sortButtonRef) => {
              this.sortOrderButtonNode = sortButtonRef;
            }}
          >
            {sortDesc ? <SortDescIcon className="" /> : <SortAscIcon className="" />}
          </ButtonIcon>
        </Tooltip>
      </div>
    );
  }
}
