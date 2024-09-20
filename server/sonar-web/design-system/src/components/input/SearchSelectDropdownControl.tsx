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
import classNames from 'classnames';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { INPUT_SIZES, themeBorder, themeColor, themeContrast } from '../../helpers';
import { Key } from '../../helpers/keyboard';
import { InputSizeKeys } from '../../types/theme';
import { InteractiveIcon } from '../InteractiveIcon';
import { ChevronDownIcon, CloseIcon } from '../icons';

interface SearchSelectDropdownControlProps {
  ariaLabel?: string;
  className?: string;
  disabled?: boolean;
  isClearable?: boolean;
  isDiscreet?: boolean;
  label?: React.ReactNode | string;
  onClear: VoidFunction;
  onClick: VoidFunction;
  placeholder?: string;
  size?: InputSizeKeys;
}

/**
 * @deprecated Use Select or SelectAsync from Echoes instead.
 *
 * See the [Migration Guide](https://xtranet-sonarsource.atlassian.net/wiki/x/K4AYxw)
 */
export function SearchSelectDropdownControl(props: SearchSelectDropdownControlProps) {
  const {
    className,
    disabled,
    placeholder,
    label,
    isClearable,
    isDiscreet,
    onClear,
    onClick,
    size = 'full',
    ariaLabel = '',
  } = props;

  const intl = useIntl();

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
          },
        )}
      >
        <span className="sw-flex-1 sw-truncate">{label ?? placeholder}</span>
        <div className="sw-flex sw-items-center">
          {isClearable && (
            <InteractiveIcon
              Icon={CloseIcon}
              aria-label={intl.formatMessage({ id: 'clear' })}
              currentColor
              onClick={() => {
                onClear();
              }}
              size="small"
            />
          )}
          <ChevronDownIcon />
        </div>
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
  ${tw`sw-px-3`};
  ${tw`sw-typo-default`};
  ${tw`sw-h-control`};
  ${tw`sw-leading-6`};
  ${tw`sw-cursor-pointer`};

  &.is-discreet {
    ${tw`sw-border-none`};
    ${tw`sw-px-1`};
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
  height: 100%;
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
