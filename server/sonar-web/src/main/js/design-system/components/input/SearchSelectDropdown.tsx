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

import styled from '@emotion/styled';
import React from 'react';
import {
  ActionMeta,
  GroupBase,
  InputActionMeta,
  OnChangeValue,
  OptionsOrGroups,
} from 'react-select';
import { AsyncProps } from 'react-select/async';
import Select from 'react-select/dist/declarations/src/Select';
import tw from 'twin.macro';
import { PopupPlacement, PopupZLevel, themeBorder } from '../../helpers';
import {
  IconOption,
  LabelValueSelectOption,
  SelectProps,
} from '../../sonar-aligned/components/input';
import { InputSizeKeys } from '../../types/theme';
import { DropdownToggler } from '../DropdownToggler';
import { SearchHighlighterContext } from '../SearchHighlighter';
import { SearchSelect } from './SearchSelect';
import { SearchSelectDropdownControl } from './SearchSelectDropdownControl';

declare module 'react-select/dist/declarations/src/Select' {
  export interface Props<Option, IsMulti extends boolean, Group extends GroupBase<Option>> {
    minLength?: number;
  }
}

interface SearchSelectDropdownProps<
  V,
  Option extends LabelValueSelectOption<V>,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
> extends SelectProps<Option, IsMulti, Group>,
    AsyncProps<Option, IsMulti, Group> {
  className?: string;
  controlAriaLabel?: string;
  controlLabel?: React.ReactNode | string;
  controlPlaceholder?: string;
  controlSize?: InputSizeKeys;
  isDiscreet?: boolean;
  zLevel?: PopupZLevel;
}

/**
 * @deprecated Use Select or SelectAsync from Echoes instead.
 *
 * See the [Migration Guide](https://xtranet-sonarsource.atlassian.net/wiki/x/K4AYxw)
 */
export function SearchSelectDropdown<
  V,
  Option extends LabelValueSelectOption<V>,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: SearchSelectDropdownProps<V, Option, IsMulti, Group>) {
  const {
    className,
    isDiscreet,
    value,
    loadOptions,
    controlLabel,
    controlPlaceholder,
    controlSize,
    isDisabled,
    minLength,
    controlAriaLabel,
    menuIsOpen,
    onChange,
    onInputChange,
    isClearable,
    zLevel = PopupZLevel.Global,
    placeholder = '',
    ...rest
  } = props;
  const [open, setOpen] = React.useState(false);
  const [inputValue, setInputValue] = React.useState('');

  React.useEffect(() => {
    if (menuIsOpen) {
      setOpen(true);
    }
  }, [menuIsOpen]);

  const ref = React.useRef<Select<Option, IsMulti, Group>>(null);

  const computedControlLabel = controlLabel ?? (value as Option | undefined)?.label ?? null;

  const toggleDropdown = React.useCallback(
    (value?: boolean) => {
      setOpen(value ?? !open);
    },
    [open],
  );

  const handleChange = React.useCallback(
    (newValue: OnChangeValue<Option, IsMulti>, actionMeta: ActionMeta<Option>) => {
      toggleDropdown(false);
      onChange?.(newValue, actionMeta);
    },
    [toggleDropdown, onChange],
  );

  const handleLoadOptions = React.useCallback(
    (query: string, callback: (options: OptionsOrGroups<Option, Group>) => void) => {
      return query.length >= (minLength ?? 0) ? loadOptions?.(query, callback) : undefined;
    },
    [minLength, loadOptions],
  );

  const handleInputChange = React.useCallback(
    (newValue: string, actionMeta: InputActionMeta) => {
      if (actionMeta.action === 'menu-close') {
        setInputValue(actionMeta.prevInputValue);
        return actionMeta.prevInputValue;
      }

      setInputValue(newValue);
      onInputChange?.(newValue, actionMeta);
      return newValue;
    },
    [onInputChange],
  );

  const handleClear = () => {
    onChange?.(null as OnChangeValue<Option, IsMulti>, {
      action: 'clear',
      removedValues: [],
    });
  };

  React.useEffect(() => {
    if (open) {
      ref.current?.inputRef?.select();
    } else {
      setInputValue('');
    }
  }, [open]);

  return (
    <DropdownToggler
      allowResizing
      className="sw-overflow-visible sw-border-none"
      onRequestClose={() => {
        toggleDropdown(false);
      }}
      open={open}
      overlay={
        <SearchHighlighterContext.Provider value={inputValue}>
          <StyledSearchSelectWrapper>
            <SearchSelect
              cacheOptions
              {...rest}
              components={{
                SingleValue: () => null,
                Option: IconOption,
                ...rest.components,
              }}
              loadOptions={handleLoadOptions}
              menuIsOpen
              minLength={minLength}
              onChange={handleChange}
              onInputChange={handleInputChange}
              placeholder={placeholder}
              selectRef={ref}
              size="large"
            />
          </StyledSearchSelectWrapper>
        </SearchHighlighterContext.Provider>
      }
      placement={PopupPlacement.BottomLeft}
      zLevel={zLevel}
    >
      <SearchSelectDropdownControl
        ariaLabel={controlAriaLabel}
        className={className}
        disabled={isDisabled}
        isClearable={isClearable && Boolean(value)}
        isDiscreet={isDiscreet}
        label={computedControlLabel}
        onClear={handleClear}
        onClick={() => {
          toggleDropdown(true);
        }}
        placeholder={controlPlaceholder}
        size={controlSize}
      />
    </DropdownToggler>
  );
}

const StyledSearchSelectWrapper = styled.div`
  ${tw`sw-w-full`};
  ${tw`sw-rounded-2`};

  .react-select {
    border: ${themeBorder('default', 'inputDisabledBorder')};
    ${tw`sw-rounded-2`};
  }

  .react-select__menu {
    ${tw`sw-m-0`};
    ${tw`sw-relative`};
    ${tw`sw-shadow-none`};
    ${tw`sw-rounded-2`};
  }

  .react-select__menu-notice--loading {
    ${tw`sw-hidden`}
  }

  .react-select__input-container {
    &::after {
      content: '' !important;
    }
  }
`;
