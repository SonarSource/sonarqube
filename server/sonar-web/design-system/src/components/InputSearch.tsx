/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import { debounce } from 'lodash';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import tw, { theme } from 'twin.macro';
import { DEBOUNCE_DELAY, INPUT_SIZES } from '../helpers/constants';
import { Key } from '../helpers/keyboard';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import { InputSizeKeys } from '../types/theme';
import DeferredSpinner from './DeferredSpinner';
import CloseIcon from './icons/CloseIcon';
import SearchIcon from './icons/SearchIcon';
import { InteractiveIcon } from './InteractiveIcon';

interface Props {
  autoFocus?: boolean;
  className?: string;
  clearIconAriaLabel: string;
  id?: string;
  innerRef?: React.RefCallback<HTMLInputElement>;
  loading?: boolean;
  maxLength?: number;
  minLength?: number;
  onBlur?: React.FocusEventHandler<HTMLInputElement>;
  onChange: (value: string) => void;
  onFocus?: React.FocusEventHandler<HTMLInputElement>;
  onKeyDown?: React.KeyboardEventHandler<HTMLInputElement>;
  onMouseDown?: React.MouseEventHandler<HTMLInputElement>;
  placeholder: string;
  searchInputAriaLabel: string;
  size?: InputSizeKeys;
  tooShortText?: string;
  value?: string;
}

const DEFAULT_MAX_LENGTH = 100;

export default function InputSearch({
  autoFocus,
  id,
  className,
  innerRef,
  onBlur,
  onChange,
  onFocus,
  onKeyDown,
  onMouseDown,
  placeholder,
  loading,
  minLength,
  maxLength = DEFAULT_MAX_LENGTH,
  size = 'medium',
  value: parentValue,
  tooShortText,
  searchInputAriaLabel,
  clearIconAriaLabel,
}: Props) {
  const input = useRef<null | HTMLElement>(null);
  const [value, setValue] = useState(parentValue ?? '');
  const debouncedOnChange = useMemo(() => debounce(onChange, DEBOUNCE_DELAY), [onChange]);

  const tooShort = isDefined(minLength) && value.length > 0 && value.length < minLength;
  const inputClassName = classNames('js-input-search', {
    touched: value.length > 0 && (!minLength || minLength > value.length),
    'sw-pr-10': value.length > 0,
  });

  useEffect(() => {
    if (parentValue !== undefined) {
      setValue(parentValue);
    }
  }, [parentValue]);

  const changeValue = (newValue: string) => {
    if (newValue.length === 0 || !minLength || minLength <= newValue.length) {
      debouncedOnChange(newValue);
    }
  };

  const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const eventValue = event.currentTarget.value;
    setValue(eventValue);
    changeValue(eventValue);
  };

  const handleInputKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === Key.Escape) {
      event.preventDefault();
      handleClearClick();
    }
    onKeyDown?.(event);
  };

  const handleClearClick = () => {
    onChange('');
    if (parentValue === undefined || parentValue === '') {
      setValue('');
    }
    input.current?.focus();
  };
  const ref = (node: HTMLInputElement | null) => {
    input.current = node;
    innerRef?.(node);
  };

  return (
    <InputSearchWrapper
      className={className}
      id={id}
      onMouseDown={onMouseDown}
      style={{ '--inputSize': INPUT_SIZES[size] }}
      title={tooShort && tooShortText && isDefined(minLength) ? tooShortText : ''}
    >
      <StyledInputWrapper className="sw-flex sw-items-center">
        <input
          aria-label={searchInputAriaLabel}
          autoComplete="off"
          autoFocus={autoFocus}
          className={inputClassName}
          maxLength={maxLength}
          onBlur={onBlur}
          onChange={handleInputChange}
          onFocus={onFocus}
          onKeyDown={handleInputKeyDown}
          placeholder={placeholder}
          ref={ref}
          role="searchbox"
          type="search"
          value={value}
        />
        <DeferredSpinner loading={loading !== undefined ? loading : false}>
          <StyledSearchIcon />
        </DeferredSpinner>
        {value && (
          <StyledInteractiveIcon
            Icon={CloseIcon}
            aria-label={clearIconAriaLabel}
            className="js-input-search-clear"
            onClick={handleClearClick}
            size="small"
          />
        )}

        {tooShort && tooShortText && isDefined(minLength) && (
          <StyledNote className="sw-ml-1" role="note">
            {tooShortText}
          </StyledNote>
        )}
      </StyledInputWrapper>
    </InputSearchWrapper>
  );
}

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
    ${tw`sw-body-sm`}
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

const StyledSearchIcon = styled(SearchIcon)`
  color: ${themeColor('inputBorder')};
  top: calc((${theme('height.control')} - ${theme('spacing.4')}) / 2);

  ${tw`sw-left-3`}
  ${tw`sw-absolute`}
`;

export const StyledInteractiveIcon = styled(InteractiveIcon)`
  ${tw`sw-absolute`}
  ${tw`sw-right-2`}
`;

const StyledNote = styled.span`
  color: ${themeColor('inputPlaceholder')};
  top: calc(1px + ${theme('inset.2')});

  ${tw`sw-absolute`}
  ${tw`sw-left-12 sw-right-10`}
  ${tw`sw-body-sm`}
  ${tw`sw-text-right`}
  ${tw`sw-truncate`}
  ${tw`sw-pointer-events-none`}
`;
