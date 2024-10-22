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

import { InputSize, Select, SelectOption, Tooltip } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { sortBy } from 'lodash';
import * as React from 'react';
import { InteractiveIcon, SortAscendIcon, SortDescendIcon, StyledPageTitle } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { SORTING_LEAK_METRICS, SORTING_METRICS, parseSorting } from '../utils';

interface Props {
  defaultOption: string;
  onChange: (sort: string, desc: boolean) => void;
  selectedSort: string;
  view: string;
}

export interface Option extends SelectOption {
  optionClass?: string;
  short?: string;
}

export default class ProjectsSortingSelect extends React.PureComponent<Props> {
  sortOrderButtonNode: HTMLElement | null = null;

  getSorting = () => {
    return parseSorting(this.props.selectedSort);
  };

  getOptions = () => {
    const sortMetrics = this.props.view === 'leak' ? SORTING_LEAK_METRICS : SORTING_METRICS;
    return sortBy(sortMetrics, (option) => (option.value === this.props.defaultOption ? 0 : 1)).map(
      (option) => ({
        value: option.value,
        label: translate('projects.sorting', option.value),
        optionClass: option.class,
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

  handleSortChange = (value: string) => {
    this.props.onChange(value, this.getSorting().sortDesc);
  };

  render() {
    const { sortDesc, sortValue } = this.getSorting();

    return (
      <div className="sw-flex sw-items-center">
        <StyledPageTitle id="aria-projects-sort" as="label" className="sw-typo-semibold sw-mr-2">
          {translate('projects.sort_by')}
        </StyledPageTitle>
        <Select
          ariaLabelledBy="aria-projects-sort"
          className="sw-typo-default"
          onChange={this.handleSortChange}
          data={this.getOptions()}
          optionComponent={ProjectsSortingSelectItem}
          placeholder={translate('project_activity.filter_events')}
          isNotClearable
          value={sortValue}
          size={InputSize.Medium}
        />
        <Tooltip
          content={
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

function ProjectsSortingSelectItem({ label, optionClass, short, value }: Readonly<Option>) {
  return (
    <div className={classNames(`it__project-sort-option-${value}`, optionClass)}>
      {short ?? label}
    </div>
  );
}
