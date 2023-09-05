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
import tw from 'twin.macro';
import { INPUT_SIZES, themeBorder, themeColor, themeContrast } from '../../helpers';
import { Key } from '../../helpers/keyboard';
import { InputSizeKeys } from '../../types/theme';
import { ChevronDownIcon } from '../icons';

interface SearchSelectDropdownControlProps {
  ariaLabel?: string;
  className?: string;
  disabled?: boolean;
  isDiscreet?: boolean;
  label?: React.ReactNode | string;
  onClick: VoidFunction;
  size?: InputSizeKeys;
}

export function SearchSelectDropdownControl(props: SearchSelectDropdownControlProps) {
  const { className, disabled, label, isDiscreet, onClick, size = 'full', ariaLabel = '' } = props;
  return (
    <StyledControl
      aria-label={ariaLabel}
      className={classNames(className, { 'is-discreet': isDiscreet })}
      onClick={() => {
        if (!disabled) {
          onClick();
        }
      }}
      onKeyDown={(event) => {
        if (event.key === Key.Enter || event.key === Key.ArrowDown) {
          onClick();
        }
      }}
      role="combobox"
      style={{ '--inputSize': isDiscreet ? 'auto' : INPUT_SIZES[size] }}
      tabIndex={disabled ? -1 : 0}
    >
      <InputValue
        className={classNames(
          'it__js-search-input-value sw-flex sw-justify-between sw-items-center',
          {
            'is-disabled': disabled,
            'is-placeholder': !label,
          }
        )}
      >
        <span className="sw-truncate">{label}</span>
        <ChevronDownIcon className="sw-ml-1" />
      </InputValue>
    </StyledControl>
  );
}

const StyledControl = styled.div`
  color: ${themeContrast('inputBackground')};
  background: ${themeColor('inputBackground')};
  border: ${themeBorder('default', 'inputBorder')};
  width: var(--inputSize);

  ${tw`sw-flex sw-justify-between sw-items-center`};
  ${tw`sw-rounded-2`};
  ${tw`sw-box-border`};
  ${tw`sw-px-3 sw-py-2`};
  ${tw`sw-body-sm`};
  ${tw`sw-h-control`};
  ${tw`sw-leading-4`};
  ${tw`sw-cursor-pointer`};

  &.is-discreet {
    ${tw`sw-border-none`};
    ${tw`sw-p-0`};
    ${tw`sw-w-auto sw-h-auto`};

    background: inherit;
  }

  &:hover {
    border: ${themeBorder('default', 'inputFocus')};

    &.is-discreet {
      ${tw`sw-border-none`};
      color: ${themeColor('discreetButtonHover')};
    }
  }

  &:focus,
  &:focus-visible,
  &:focus-within {
    border: ${themeBorder('default', 'inputFocus')};
    outline: ${themeBorder('focus', 'inputFocus')};

    &.is-discreet {
      ${tw`sw-rounded-1 sw-border-none`};
      outline: ${themeBorder('focus', 'discreetFocusBorder')};
    }
  }
`;

const InputValue = styled.span`
  width: 100%;
  color: ${themeContrast('inputBackground')};

  ${tw`sw-truncate`};

  &.is-placeholder {
    color: ${themeColor('inputPlaceholder')};
  }

  &.is-disabled {
    color: ${themeContrast('inputDisabled')};
  }
`;
