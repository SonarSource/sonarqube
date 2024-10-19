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
import React from 'react';
import { GroupBase, InputProps } from 'react-select';
import AsyncSelect, { AsyncProps } from 'react-select/async';
import { INPUT_SIZES } from '../../helpers';
import { Key } from '../../helpers/keyboard';
import { SelectProps, selectStyle } from '../../sonar-aligned/components/input';
import { InputSearch } from './InputSearch';

type SearchSelectProps<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
> = SelectProps<Option, IsMulti, Group> & AsyncProps<Option, IsMulti, Group>;

/**
 * @deprecated Use Select or SelectAsync from Echoes instead.
 * See the [Migration Guide](https://xtranet-sonarsource.atlassian.net/wiki/x/K4AYxw)
 */
export function SearchSelect<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>({ size = 'full', selectRef, ...props }: SearchSelectProps<Option, IsMulti, Group>) {
  const styles = selectStyle<Option, IsMulti, Group>({ size });
  return (
    <AsyncSelect<Option, IsMulti, Group>
      {...omit(props, 'className', 'large')}
      className={classNames('react-select', props.className)}
      classNamePrefix="react-select"
      classNames={{
        control: ({ isDisabled }) =>
          classNames(
            'sw-border-0 sw-rounded-2 sw-outline-none sw-shadow-none',
            isDisabled && 'sw-pointer-events-none sw-cursor-not-allowed',
          ),
        indicatorsContainer: () => 'sw-hidden',
        input: () => `sw-flex sw-w-full sw-p-0 sw-m-0`,
        valueContainer: () => `sw-px-3 sw-pb-1 sw-mb-1 sw-pt-4`,
        placeholder: () => 'sw-hidden',
        ...props.classNames,
      }}
      components={{
        Input: SearchSelectInput,
        ...props.components,
      }}
      ref={selectRef}
      styles={{
        ...styles,
        menu: (base, props) => ({
          ...styles.menu?.(base, props),
          width: `calc(${INPUT_SIZES[size]} - 2px)`,
        }),
      }}
    />
  );
}

/**
 * @deprecated Use Select or SelectAsync from Echoes instead.
 */
export function SearchSelectInput<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: InputProps<Option, IsMulti, Group>) {
  const {
    selectProps: { placeholder, isLoading, inputValue, minLength },
  } = props;

  const onChange = (v: string, prevValue = '') => {
    props.selectProps.onInputChange(v, { action: 'input-change', prevInputValue: prevValue });
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    const target = event.target as HTMLInputElement;

    if (event.key === Key.Escape.toString() && target.value !== '') {
      event.stopPropagation();
      onChange('');
    }
  };

  return (
    <InputSearch
      {...omit(props, 'value', 'aria-label', 'id')}
      autoFocus
      inputId={props.id}
      loading={isLoading && inputValue.length >= (minLength ?? 0)}
      minLength={minLength}
      onChange={onChange}
      onKeyDown={handleKeyDown}
      placeholder={placeholder as string}
      searchInputAriaLabel={props['aria-label']}
      size="full"
    />
  );
}
