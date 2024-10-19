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
import classNames from 'classnames';
import { omit } from 'lodash';
import { useMemo } from 'react';
import ReactSelect, { GroupBase } from 'react-select';
import {
  ClearIndicator,
  DropdownIndicator,
  IconOption,
  SelectProps,
  SingleValue,
  selectStyle,
} from './SelectCommon';

/**
 * @deprecated Use Select or SelectAsync from Echoes instead.
 *
 * See the [Migration Guide](https://xtranet-sonarsource.atlassian.net/wiki/x/K4AYxw)
 */
export function InputSelect<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>({
  size = 'medium',
  className,
  options,
  getOptionLabel,
  selectRef,
  shouldSortOption = false,
  ...props
}: SelectProps<Option, IsMulti, Group>) {
  const orderedOptions = useMemo(() => {
    if (!options || options.length === 0) {
      return options;
    }

    if (shouldSortOption) {
      return (options as Option[]).sort((a, b) => {
        const nameA = getOptionLabel?.(a).toUpperCase() ?? '';
        const nameB = getOptionLabel?.(b).toUpperCase() ?? '';
        if (nameA < nameB) {
          return -1;
        }
        if (nameA > nameB) {
          return 1;
        }

        return 0;
      });
    }

    return options;
  }, [shouldSortOption, getOptionLabel, options]);

  return (
    <ReactSelect<Option, IsMulti, Group>
      {...omit(props, 'className', 'large')}
      className={classNames('react-select', className)}
      classNamePrefix="react-select"
      classNames={{
        container: () => 'sw-relative sw-inline-block sw-align-middle',
        placeholder: () => 'sw-truncate sw-leading-4',
        menu: () => 'sw-z-dropdown-menu sw-ml-1/2 sw-mt-2',
        menuList: () => 'sw-overflow-y-auto sw-py-2 sw-max-h-[12.25rem]',
        clearIndicator: () => 'sw-p-0',
        dropdownIndicator: () => classNames(props.isClearable && 'sw-p-0'),
        control: ({ isDisabled }) =>
          classNames(
            'sw-box-border sw-rounded-2 sw-overflow-hidden',
            isDisabled && 'sw-pointer-events-none sw-cursor-not-allowed',
          ),
        option: ({ isDisabled }) =>
          classNames(
            'it__select-option sw-py-2 sw-px-3 sw-cursor-pointer',
            isDisabled && 'sw-pointer-events-none sw-cursor-not-allowed',
          ),
        ...props.classNames,
      }}
      components={{
        ClearIndicator,
        Option: IconOption,
        SingleValue,
        DropdownIndicator,
        IndicatorSeparator: null,
        ...props.components,
      }}
      getOptionLabel={getOptionLabel}
      isClearable={props.isClearable ?? false}
      isSearchable={props.isSearchable ?? false}
      onMenuOpen={props.onMenuOpen}
      options={orderedOptions}
      ref={selectRef}
      styles={selectStyle({ size })}
    />
  );
}
