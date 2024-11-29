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
import { IconSearch } from '@sonarsource/echoes-react';
import { debounce } from 'lodash';
import React, { PropsWithChildren, useEffect, useMemo, useRef, useState } from 'react';
import { useIntl } from 'react-intl';
import tw, { theme } from 'twin.macro';
import { DEBOUNCE_DELAY } from '../../helpers/constants';
import { Key } from '../../helpers/keyboard';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { InputSizeKeys } from '../../types/theme';
import { InteractiveIcon } from '../InteractiveIcon';
import { Spinner } from '../Spinner';
import { CloseIcon } from '../icons/CloseIcon';

interface Props {
  autoFocus?: boolean;
  className?: string;
  id?: string;
  innerRef?: React.RefCallback<HTMLInputElement>;
  inputId?: string;
  loading?: boolean;
  maxLength?: number;
  minLength?: number; // Added minLength
  onBlur?: React.FocusEventHandler<HTMLInputElement>;
  onChange: (value: string) => void;
  onFocus?: React.FocusEventHandler<HTMLInputElement>;
  onKeyDown?: React.KeyboardEventHandler<HTMLInputElement>;
  onMouseDown?: React.MouseEventHandler<HTMLInputElement>;
  placeholder?: string;
  searchInputAriaLabel?: string;
  size?: InputSizeKeys;
  value?: string;
}

const DEFAULT_MAX_LENGTH = 100;

export function InputSearch(props: PropsWithChildren<Props>) {
  const {
    autoFocus,
    id,
    className,
    innerRef,
    inputId,
    onBlur,
    onChange,
    onFocus,
    onKeyDown,
    onMouseDown,
    placeholder,
    loading,
    maxLength = DEFAULT_MAX_LENGTH,
    minLength = 0,
    value: parentValue,
    searchInputAriaLabel,
  } = props;

  const intl = useIntl();
  const input = useRef<null | HTMLElement>(null);
  const [value, setValue] = useState(parentValue ?? '');

  const debouncedOnChange = useMemo(
    () =>
      debounce((val: string) => {
        onChange(val);
      }, DEBOUNCE_DELAY),
    [onChange],
  );

  useEffect(() => {
    if (parentValue !== undefined && parentValue !== value) {
      setValue(parentValue);
    }
  }, [parentValue]);

  useEffect(() => {
    if (autoFocus && input.current) {
      input.current.focus();
    }
  }, [autoFocus]);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const eventValue = event.currentTarget.value;
    setValue(eventValue);

    // Trigger onChange only if value length satisfies minLength or is empty
    if (eventValue.length === 0 || eventValue.length >= minLength) {
      debouncedOnChange(eventValue);
    }
  };

  const handleInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === Key.Escape) {
      event.preventDefault();
      handleClearClick();
    }
    onKeyDown?.(event);
  };

  const handleClearClick = () => {
    setValue('');
    onChange('');
    input.current?.focus();
  };

  return (
    <InputSearchWrapper className={className} id={id} onMouseDown={onMouseDown}>
      <StyledInputWrapper className="sw-flex sw-items-center">
        <input
          aria-label={searchInputAriaLabel ?? placeholder}
          autoComplete="off"
          className="js-input-search"
          id={inputId}
          maxLength={maxLength}
          onBlur={onBlur}
          onChange={handleInputChange}
          onFocus={onFocus}
          onKeyDown={handleInputKeyDown}
          placeholder={placeholder}
          ref={(node) => {
            input.current = node;
            innerRef?.(node);
          }}
          role="searchbox"
          type="search"
          value={value}
        />
        <StyledSearchIconWrapper>
          <Spinner className="sw-z-normal" loading={loading ?? false}>
            <StyledSearchIcon />
          </Spinner>
        </StyledSearchIconWrapper>
        {value && (
          <StyledInteractiveIcon
            Icon={CloseIcon}
            aria-label={intl.formatMessage({ id: 'clear' })}
            onClick={handleClearClick}
            size="small"
          />
        )}
      </StyledInputWrapper>
    </InputSearchWrapper>
  );
}

InputSearch.displayName = 'InputSearch'; // so that tests don't see the obfuscated production name

export const InputSearchWrapper = styled.div`
  width: var(--inputSize);

  ${tw`sw-relative sw-inline-block`}
  ${tw`sw-whitespace-nowrap`}
  ${tw`sw-align-middle`}
  ${tw`sw-h-control`}
`;

export const StyledInputWrapper = styled.div`
  input {
    background: ${themeColor('inputBackground')};
    color: ${themeContrast('inputBackground')};
    border: ${themeBorder('default', 'inputBorder')};

    ${tw`sw-rounded-2`}
    ${tw`sw-box-border`}
    ${tw`sw-pl-10`}
    ${tw`sw-typo-default`}
    ${tw`sw-w-full sw-h-control`}

    &::placeholder {
      color: ${themeColor('inputPlaceholder')};

      ${tw`sw-truncate`}
    }

    &:hover {
      border: ${themeBorder('default', 'inputFocus')};
    }

    &:focus,
    &:active {
      border: ${themeBorder('default', 'inputFocus')};
      outline: ${themeBorder('focus', 'inputFocus')};
    }

    &::-webkit-search-decoration,
    &::-webkit-search-cancel-button,
    &::-webkit-search-results-button,
    &::-webkit-search-results-decoration {
      ${tw`sw-hidden sw-appearance-none`}
    }
  }
`;

export const StyledSearchIcon = styled(IconSearch)`
  color: ${themeColor('inputBorder')};
`;

export const StyledSearchIconWrapper = styled.div`
  ${tw`sw-left-3`}
  ${tw`sw-absolute`}
  ${tw`sw-z-normal`}
`;

export const StyledInteractiveIcon = styled(InteractiveIcon)`
  ${tw`sw-absolute`}
  ${tw`sw-right-2`}
`;

export const StyledNote = styled.span`
  color: ${themeColor('inputPlaceholder')};
  top: calc(1px + ${theme('inset.2')});

  ${tw`sw-absolute`}
  ${tw`sw-left-12 sw-right-10`}
  ${tw`sw-typo-default`}
  ${tw`sw-text-right`}
  ${tw`sw-truncate`}
  ${tw`sw-pointer-events-none`}
`;
