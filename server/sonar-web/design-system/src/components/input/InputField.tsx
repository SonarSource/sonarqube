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
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import { forwardRef } from 'react';
import tw from 'twin.macro';
import { INPUT_SIZES } from '../../helpers/constants';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { InputSizeKeys, ThemedProps } from '../../types/theme';

interface InputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  as?: React.ElementType;
  className?: string;
  isInvalid?: boolean;
  size?: InputSizeKeys;
}

interface InputTextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  className?: string;
  isInvalid?: boolean;
  size?: InputSizeKeys;
}

export const InputField = forwardRef<HTMLInputElement, InputProps>(
  ({ size = 'medium', style, ...props }, ref) => {
    return (
      <StyledInput ref={ref} style={{ ...style, '--inputSize': INPUT_SIZES[size] }} {...props} />
    );
  },
);
InputField.displayName = 'InputField';

export const InputTextArea = forwardRef<HTMLTextAreaElement, InputTextAreaProps>(
  ({ size = 'medium', style, ...props }, ref) => {
    return (
      <StyledTextArea ref={ref} style={{ ...style, '--inputSize': INPUT_SIZES[size] }} {...props} />
    );
  },
);
InputTextArea.displayName = 'InputTextArea';

const defaultStyle = (props: ThemedProps) => css`
  --border: ${themeBorder('default', 'inputBorder')(props)};
  --focusBorder: ${themeBorder('default', 'inputFocus')(props)};
  --focusOutline: var(--echoes-focus-border-width-default) solid var(--echoes-color-focus-default);
`;

const dangerStyle = (props: ThemedProps) => css`
  --border: ${themeBorder('default', 'inputDanger')(props)};
  --focusBorder: ${themeBorder('default', 'inputDangerFocus')(props)};
  --focusOutline: var(--echoes-focus-border-width-default) solid var(--echoes-color-focus-default);
`;

const getInputVariant = (props: ThemedProps & { isInvalid?: boolean; isValid?: boolean }) => {
  const { isInvalid } = props;
  if (isInvalid) {
    return dangerStyle;
  }
  return defaultStyle;
};

const baseStyle = (props: ThemedProps) => css`
  color: ${themeContrast('inputBackground')(props)};
  background: ${themeColor('inputBackground')(props)};
  border: var(--border);
  width: var(--inputSize);
  transition: border-color 0.2s ease;

  ${tw`sw-typo-default`}
  ${tw`sw-box-border`}
  ${tw`sw-rounded-2`}
  ${tw`sw-px-3 sw-py-2`}

  &::placeholder {
    color: var(--echoes-color-text-placeholder);
  }

  &:hover {
    border: var(--focusBorder);
  }

  &:active,
  &:focus,
  &:focus-within,
  &:focus-visible {
    border: var(--focusBorder);
    outline: var(--focusOutline);
    outline-offset: var(--echoes-focus-border-offset-default);
  }

  &:disabled,
  &:disabled:hover {
    color: ${themeContrast('inputDisabled')(props)};
    background-color: ${themeColor('inputDisabled')(props)};
    border: ${themeBorder('default', 'inputDisabledBorder')(props)};
    outline: none;

    ${tw`sw-cursor-not-allowed`};
    &::placeholder {
      color: var(--echoes-color-text-disabled);
    }
  }
`;

const StyledInput = styled.input`
  ${getInputVariant}
  ${baseStyle}
  ${tw`sw-h-control`}
`;

const StyledTextArea = styled.textarea`
  ${getInputVariant};
  ${baseStyle};
`;
