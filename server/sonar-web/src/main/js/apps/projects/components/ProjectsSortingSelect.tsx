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
import {
  InputSelect,
  InteractiveIcon,
  LabelValueSelectOption,
  SortAscendIcon,
  SortDescendIcon,
  StyledPageTitle,
} from 'design-system';
import { omit, sortBy } from 'lodash';
import * as React from 'react';
import { OptionProps, components } from 'react-select';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { SORTING_LEAK_METRICS, SORTING_METRICS, parseSorting } from '../utils';

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
      }),
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
      <div className="sw-flex sw-items-center">
        <StyledPageTitle
          id="aria-projects-sort"
          as="label"
          className="sw-body-sm-highlight sw-mr-2"
        >
          {translate('projects.sort_by')}
        </StyledPageTitle>
        <InputSelect
          aria-labelledby="aria-projects-sort"
          className="sw-body-sm"
          onChange={(data: LabelValueSelectOption<string>) => this.handleSortChange(data)}
          options={this.getOptions()}
          components={{
            Option: this.projectsSortingSelectOption,
          }}
          placeholder={translate('project_activity.filter_events')}
          size="small"
          value={value}
        />
        <Tooltip
          mouseLeaveDelay={1}
          overlay={
            sortDesc ? translate('projects.sort_descending') : translate('projects.sort_ascending')
          }
        >
          <InteractiveIcon
            Icon={sortDesc ? SortDescendIcon : SortAscendIcon}
            aria-label={
              sortDesc
                ? translate('projects.sort_descending')
                : translate('projects.sort_ascending')
            }
            className="js-projects-invert-sort sw-ml-2"
            onClick={this.handleDescToggle}
            innerRef={(sortButtonRef) => {
              this.sortOrderButtonNode = sortButtonRef;
            }}
          />
        </Tooltip>
      </div>
    );
  }
}
