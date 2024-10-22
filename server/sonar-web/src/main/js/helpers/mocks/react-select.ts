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
  ClearIndicatorProps,
  ControlProps,
  DropdownIndicatorProps,
  GroupBase,
  InputProps,
  OptionProps,
} from 'react-select';

export function mockReactSelectOptionProps<
  OptionType = unknown,
  IsMulti extends boolean = boolean,
  GroupType extends GroupBase<OptionType> = GroupBase<OptionType>,
>(
  data: OptionType,
  overrides?: OptionProps<OptionType, IsMulti, GroupType>,
): OptionProps<OptionType, IsMulti, GroupType> {
  return {
    ...overrides,
    data,
  } as OptionProps<OptionType, IsMulti, GroupType>;
}

export function mockReactSelectInputProps(): InputProps {
  return {} as InputProps;
}

export function mockReactSelectControlProps<
  OptionType = unknown,
  IsMulti extends boolean = boolean,
  GroupType extends GroupBase<OptionType> = GroupBase<OptionType>,
>(): ControlProps<OptionType, IsMulti, GroupType> {
  return {} as ControlProps<OptionType, IsMulti, GroupType>;
}

export function mockReactSelectClearIndicatorProps<
  OptionType = unknown,
  IsMulti extends boolean = boolean,
  GroupType extends GroupBase<OptionType> = GroupBase<OptionType>,
>(_option: OptionType): ClearIndicatorProps<OptionType, IsMulti, GroupType> {
  return { getStyles: () => {} } as unknown as ClearIndicatorProps<OptionType, IsMulti, GroupType>;
}

export function mockReactSelectDropdownIndicatorProps<
  OptionType = unknown,
  IsMulti extends boolean = boolean,
  GroupType extends GroupBase<OptionType> = GroupBase<OptionType>,
>(_option: OptionType): DropdownIndicatorProps<OptionType, IsMulti, GroupType> {
  return {} as DropdownIndicatorProps<OptionType, IsMulti, GroupType>;
}
