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
import { useTheme as themeInfo } from '@emotion/react';
import { RefObject } from 'react';
import { useIntl } from 'react-intl';
import {
  ClearIndicatorProps,
  GroupBase,
  Props as NamedProps,
  OptionProps,
  StylesConfig,
  components,
} from 'react-select';
import Select from 'react-select/dist/declarations/src/Select';
import { InteractiveIcon } from '../../../components/InteractiveIcon';
import { SearchHighlighter } from '../../../components/SearchHighlighter';
import { ChevronDownIcon, CloseIcon } from '../../../components/icons';
import { INPUT_SIZES } from '../../../helpers';
import { themeBorder, themeColor, themeContrast } from '../../../helpers/theme';
import { InputSizeKeys } from '../../../types/theme';

export interface ExtensionProps<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
> {
  clearLabel?: string;
  selectRef?: RefObject<Select<Option, IsMulti, Group>>;
  shouldSortOption?: boolean;
  size?: InputSizeKeys;
}

export type SelectProps<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
> = NamedProps<Option, IsMulti, Group> & ExtensionProps<Option, IsMulti, Group>;

export function IconOption<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: OptionProps<Option, IsMulti, Group>) {
  const { label, isSelected } = props;
  const { Icon } = props.data as { Icon: JSX.Element };

  // For tests and a11y
  props.innerProps.role = 'option';
  props.innerProps['aria-selected'] = isSelected;

  return (
    <components.Option {...props}>
      <div className="sw-flex sw-items-center sw-gap-1">
        {Icon}
        <SearchHighlighter>{label}</SearchHighlighter>
      </div>
    </components.Option>
  );
}

export function SingleValue<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: OptionProps<Option, IsMulti, Group>) {
  const label = props.selectProps.getOptionLabel(props.data);
  const { Icon } = props.data as { Icon: JSX.Element };

  return (
    <components.SingleValue {...props}>
      <div className="sw-flex sw-items-center sw-gap-1">
        {Icon}
        {label}
      </div>
    </components.SingleValue>
  );
}

export function ClearIndicator<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(
  props: ClearIndicatorProps<Option, IsMulti, Group> & {
    selectProps: SelectProps<Option, IsMulti, Group>;
  },
) {
  const intl = useIntl();
  const {
    selectProps: { clearLabel },
  } = props;

  return (
    <components.ClearIndicator {...props}>
      <InteractiveIcon
        Icon={CloseIcon}
        aria-label={clearLabel ?? intl.formatMessage({ id: 'clear' })}
        onClick={props.clearValue}
        size="small"
      />
    </components.ClearIndicator>
  );
}

export function DropdownIndicator<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: OptionProps<Option, IsMulti, Group>) {
  return (
    <components.DropdownIndicator {...props}>
      <div className="sw-pr-2 sw-flex">
        <ChevronDownIcon />
      </div>
    </components.DropdownIndicator>
  );
}

export function selectStyle<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>({ size }: { size: InputSizeKeys }): StylesConfig<Option, IsMulti, Group> {
  const theme = themeInfo();

  return {
    control: (base, { isFocused, menuIsOpen, isDisabled }) => ({
      ...base,
      color: themeContrast('inputBackground')({ theme }),
      cursor: 'pointer',
      background: themeColor('inputBackground')({ theme }),
      transition: 'border 0.2s ease, outline 0.2s ease',
      outline: isFocused && !menuIsOpen ? themeBorder('focus', 'inputFocus')({ theme }) : 'none',
      ...(isDisabled && {
        color: themeContrast('inputDisabled')({ theme }),
        background: themeColor('inputDisabled')({ theme }),
        border: themeBorder('default', 'inputDisabledBorder')({ theme }),
        outline: 'none',
      }),
      ...(isFocused && {
        border: themeBorder('default', 'inputBorder')({ theme }),
      }),
    }),
    menu: (base) => ({
      ...base,
      width: INPUT_SIZES[size],
    }),
    option: (base, { isFocused, isSelected }) => ({
      ...base,
      ...((isSelected || isFocused) && {
        background: themeColor('selectOptionSelected')({ theme }),
        color: themeContrast('primaryLight')({ theme }),
      }),
    }),
    singleValue: (base) => ({
      ...base,
      color: themeContrast('primaryLight')({ theme }),
    }),
    placeholder: (base) => ({
      ...base,
      color: themeContrast('inputPlaceholder')({ theme }),
    }),
  };
}

export interface LabelValueSelectOption<V = string> {
  Icon?: React.ReactNode;
  label: string;
  value: V;
}
