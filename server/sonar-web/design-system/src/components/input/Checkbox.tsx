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
import { Spinner } from 'design-system/src/components/Spinner';
import React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { CheckIcon } from '../icons/CheckIcon';
import { CustomIcon } from '../icons/Icon';

interface Props {
  checked: boolean;
  children?: React.ReactNode;
  className?: string;
  disabled?: boolean;
  id?: string;
  label?: string;
  loading?: boolean;
  onCheck: (checked: boolean, id?: string) => void;
  onClick?: (event: React.MouseEvent<HTMLLabelElement>) => void;
  onFocus?: VoidFunction;
  right?: boolean;
  thirdState?: boolean;
  title?: string;
}

/** @deprecated Use Checkbox from Echoes instead.
 *
 * Some of the props have changed or been renamed:
 * - ~`aria-disabled`~ is now inferred from `isDisabled`
 * - `ariaLabel` is now mandatory in the absence of `label`
 * - ~`children`~ has been removed
 * - `disabled` is now `isDisabled`
 * - `label` is no longer used in the aria-label but displayed next to the checkbox
 * - `loading` is now `isLoading`
 * - ~`onClick`~ has been removed
 * - ~`right`~ has been removed
 * - `thirdState` is now represented by the `indeterminate` value of the `checked` prop
 */
export function Checkbox({
  checked,
  disabled,
  children,
  className,
  id,
  label,
  loading = false,
  onCheck,
  onFocus,
  onClick,
  right,
  thirdState = false,
  title,
}: Props) {
  const handleChange = () => {
    if (!disabled) {
      onCheck(!checked, id);
    }
  };

  return (
    <CheckboxContainer className={className} disabled={disabled} onClick={onClick}>
      {right && children}
      <AccessibleCheckbox
        aria-label={label ?? title}
        checked={checked}
        disabled={disabled ?? loading}
        id={id}
        onChange={handleChange}
        onFocus={onFocus}
        type="checkbox"
      />
      <Spinner loading={loading}>
        <StyledCheckbox aria-hidden data-clickable="true" title={title}>
          <CheckboxIcon checked={checked} thirdState={thirdState} />
        </StyledCheckbox>
      </Spinner>
      {!right && children}
    </CheckboxContainer>
  );
}

interface CheckIconProps {
  checked?: boolean;
  thirdState?: boolean;
}

function CheckboxIcon({ checked, thirdState }: CheckIconProps) {
  if (checked && thirdState) {
    return (
      <CustomIcon>
        <rect fill="currentColor" height="2" rx="1" width="50%" x="4" y="7" />
      </CustomIcon>
    );
  } else if (checked) {
    return <CheckIcon fill="buttonSecondary" />;
  }
  return null;
}

const CheckboxContainer = styled.label<{ disabled?: boolean }>`
  color: ${themeContrast('backgroundSecondary')};
  user-select: none;

  ${tw`sw-inline-flex sw-items-center`};

  &:hover {
    ${tw`sw-cursor-pointer`}
  }

  &:disabled {
    color: ${themeContrast('checkboxDisabled')};
    ${tw`sw-cursor-not-allowed`}
  }
`;

export const StyledCheckbox = styled.span`
  border: ${themeBorder('default', 'primary')};
  color: ${themeContrast('primary')};

  ${tw`sw-w-4 sw-h-4`};
  ${tw`sw-rounded-1/2`};
  ${tw`sw-box-border`}
  ${tw`sw-inline-flex sw-items-center sw-justify-center`};
`;

export const AccessibleCheckbox = styled.input`
  // Following css makes the checkbox accessible and invisible
  border: 0;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  height: 1px;
  overflow: hidden;
  padding: 0;
  white-space: nowrap;
  width: 1px;
  appearance: none;

  &:focus,
  &:active {
    &:not(:disabled) ~ ${StyledCheckbox} {
      outline: ${themeBorder('focus', 'primary')};
    }
  }

  &:checked {
    & ~ ${StyledCheckbox} {
      background: ${themeColor('primary')};
    }
    &:disabled ~ ${StyledCheckbox} {
      background: ${themeColor('checkboxDisabledChecked')};
    }
  }

  &:hover {
    &:not(:disabled) ~ ${StyledCheckbox} {
      background: ${themeColor('checkboxHover')};
      border: ${themeBorder('default', 'primary')};
    }

    &:checked:not(:disabled) ~ ${StyledCheckbox} {
      background: ${themeColor('checkboxCheckedHover')};
      border: ${themeBorder('default', 'checkboxCheckedHover')};
    }
  }

  &:disabled ~ ${StyledCheckbox} {
    background: ${themeColor('checkboxDisabled')};
    color: ${themeColor('checkboxDisabled')};
    border: ${themeBorder('default', 'checkboxDisabledChecked')};
  }
`;
