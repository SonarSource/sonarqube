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
import React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../../helpers/theme';

type AllowedRadioButtonAttributes = Pick<
  React.InputHTMLAttributes<HTMLInputElement>,
  'aria-label' | 'autoFocus' | 'id' | 'name' | 'style' | 'title' | 'type'
>;

interface PropsBase extends AllowedRadioButtonAttributes {
  checked: boolean;
  children?: React.ReactNode;
  className?: string;
  disabled?: boolean;
}

type Props =
  | ({ onCheck: (value: string) => void; value: string } & PropsBase)
  | ({ onCheck: () => void; value: never } & PropsBase);

export function RadioButton({
  checked,
  children,
  className,
  disabled,
  onCheck,
  value,
  ...htmlProps
}: Props) {
  const handleChange = () => {
    if (!disabled) {
      onCheck(value);
    }
  };

  return (
    <LabelStyled
      className={classNames(
        {
          disabled,
        },
        className,
      )}
    >
      <RadioButtonStyled
        aria-checked={checked}
        aria-disabled={disabled}
        checked={checked}
        disabled={disabled}
        onChange={handleChange}
        type="radio"
        value={value}
        {...htmlProps}
      />
      {children}
    </LabelStyled>
  );
}

const LabelStyled = styled.label<{ disabled?: boolean }>`
  ${tw`sw-flex sw-items-center`}
  ${tw`sw-cursor-pointer`}

  &.disabled {
    color: ${themeColor('radioDisabledLabel')};
    ${tw`sw-cursor-not-allowed`}
  }
`;

export const RadioButtonStyled = styled.input`
  appearance: none; //disables native style
  border: ${themeBorder('default', 'radioBorder')};

  ${tw`sw-cursor-pointer`}

  ${tw`sw-w-4 sw-min-w-4 sw-h-4 sw-min-h-4`}
  ${tw`sw-p-1 sw-mr-2`}
  ${tw`sw-inline-block`}
  ${tw`sw-box-border`}
  ${tw`sw-rounded-pill`}

  &:hover {
    background: ${themeColor('radioHover')};
  }

  &:focus,
  &:focus-visible {
    background: ${themeColor('radioHover')};
    border: ${themeBorder('default', 'radioFocusBorder')};
    outline: ${themeBorder('focus', 'radioFocusOutline')};
  }

  &.is-checked,
  &:focus:checked,
  &:focus-visible:checked,
  &:hover:checked,
  &:checked {
    // Color cannot be used with multiple backgrounds, only image is allowed
    background-image: linear-gradient(to right, ${themeColor('radio')}, ${themeColor('radio')}),
      linear-gradient(to right, ${themeColor('radioChecked')}, ${themeColor('radioChecked')});
    background-clip: content-box, padding-box;
    border: ${themeBorder('default', 'radioBorder')};
  }

  &.is-disabled,
  &:disabled {
    background: ${themeColor('radioDisabledBackground')};
    border: ${themeBorder('default', 'radioDisabledBorder')};
    background-clip: unset;

    &.is-checked,
    &:checked {
      background-image: linear-gradient(
          to right,
          ${themeColor('radioDisabled')},
          ${themeColor('radioDisabled')}
        ),
        linear-gradient(
          to right,
          ${themeColor('radioDisabledBackground')},
          ${themeColor('radioDisabledBackground')}
        ) !important;
      background-clip: content-box, padding-box !important;
      border: ${themeBorder('default', 'radioDisabledBorder')} !important;
    }
  }
`;
