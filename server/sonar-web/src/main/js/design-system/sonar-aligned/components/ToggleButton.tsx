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
import tw from 'twin.macro';
import { Badge } from '../../components/Badge';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { getTabId, getTabPanelId } from '../helpers/tabs';
import { ButtonSecondary } from './buttons';

type ToggleButtonValueType = string | number | boolean;

export interface ToggleButtonsOption<T extends ToggleButtonValueType> {
  counter?: number;
  disabled?: boolean;
  label: string | React.ReactNode;
  value: T;
}

export interface ButtonToggleProps<T extends ToggleButtonValueType> {
  disabled?: boolean;
  label?: string;
  onChange: (value: T) => void;
  options: ReadonlyArray<ToggleButtonsOption<T>>;
  role?: 'radiogroup' | 'tablist';
  value?: T;
}

export function ToggleButton<T extends ToggleButtonValueType>(props: ButtonToggleProps<T>) {
  const { disabled = false, label, options, value, role = 'radiogroup' } = props;
  const isRadioGroup = role === 'radiogroup';

  return (
    <Wrapper aria-label={label} role={role}>
      {options.map((option) => (
        <OptionButton
          aria-checked={isRadioGroup ? option.value === value : undefined}
          aria-controls={isRadioGroup ? undefined : getTabPanelId(String(option.value))}
          aria-current={option.value === value}
          aria-selected={!isRadioGroup ? option.value === value : undefined}
          data-value={option.value}
          disabled={disabled || option.disabled}
          id={getTabId(String(option.value))}
          key={option.value.toString()}
          onClick={() => {
            if (option.value !== value) {
              props.onChange(option.value);
            }
          }}
          role={isRadioGroup ? 'radio' : 'tab'}
          selected={option.value === value}
        >
          {option.label}
          {option.counter ? (
            <Badge className="sw-ml-1" variant="counter">
              {option.counter}
            </Badge>
          ) : null}
        </OptionButton>
      ))}
    </Wrapper>
  );
}

const Wrapper = styled.div`
  border: ${themeBorder('default', 'toggleBorder')};

  ${tw`sw-inline-flex`}
  ${tw`sw-h-control`}
  ${tw`sw-box-border`}
  ${tw`sw-font-semibold`}
  ${tw`sw-rounded-2`}
`;

const OptionButton = styled(ButtonSecondary)<{ selected: boolean }>`
  background: ${(props) => (props.selected ? themeColor('toggleHover') : themeColor('toggle'))};
  border: none;
  color: ${(props) => (props.selected ? themeContrast('toggleHover') : themeContrast('toggle'))};
  font-weight: ${(props) =>
    props.selected ? 'var(--echoes-font-weight-semi-bold)' : 'var(--echoes-font-weight-regular)'};
  height: auto;

  ${tw`sw-rounded-0`};
  ${tw`sw-truncate`};

  &:first-of-type {
    ${tw`sw-rounded-l-2`};
  }

  &:last-of-type {
    ${tw`sw-rounded-r-2`};
  }

  &:not(:last-of-type) {
    border-right: ${themeBorder('default', 'toggleBorder')};
  }

  &:hover {
    background: ${themeColor('toggleHover')};
    color: ${themeContrast('toggleHover')};
  }

  &:focus-visible {
    outline: var(--echoes-focus-border-width-default) solid var(--echoes-color-focus-default);
    outline-offset: var(--echoes-focus-border-offset-default);
    z-index: 1;
  }
`;
