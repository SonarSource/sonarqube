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
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { InputSizeKeys } from '../../types/theme';
import { InputSelect, LabelValueSelectOption } from './InputSelect';

interface Props<V> {
  className?: string;
  components?: Parameters<typeof InputSelect>[0]['components'];
  customValue?: JSX.Element;
  isDisabled?: boolean;
  menuIsOpen?: boolean;
  onMenuClose?: () => void;
  onMenuOpen?: () => void;
  options: Array<LabelValueSelectOption<V>>;
  setValue: ({ value }: LabelValueSelectOption<V>) => void;
  size?: InputSizeKeys;
  value: V;
}

export function DiscreetSelect<V>({
  className,
  customValue,
  onMenuOpen,
  options,
  size = 'small',
  setValue,
  value,
  ...props
}: Props<V>) {
  return (
    <StyledSelect
      className={className}
      onChange={setValue}
      onMenuOpen={onMenuOpen}
      options={options}
      placeholder={customValue}
      size={size}
      value={options.find((item) => item.value === value)}
      {...props}
    />
  );
}

const StyledSelect = styled(InputSelect)`
  & {
    width: inherit !important;
  }

  & .react-select__dropdown-indicator {
    ${tw`sw-p-0 sw-py-1`};
  }

  & .react-select__value-container {
    ${tw`sw-p-0`};
  }

  & .react-select__menu {
    margin: 0;
  }

  & .react-select__control {
    height: auto;
    min-height: inherit;
    color: ${themeContrast('discreetBackground')};
    background: none;
    outline: inherit;
    box-shadow: none;

    ${tw`sw-border-none`};
    ${tw`sw-p-0`};
    ${tw`sw-cursor-pointer`};
    ${tw`sw-flex sw-items-center`};
    ${tw`sw-body-sm`};
    ${tw`sw-select-none`};

    &:hover {
      ${tw`sw-border-none`};
      outline: none;
      color: ${themeColor('discreetButtonHover')};
      border-color: inherit;
      box-shadow: none;

      & .react-select__single-value,
      & .react-select__dropdown-indicator,
      & .react-select__placeholder {
        color: ${themeColor('discreetButtonHover')};
      }
    }

    &:focus {
      ${tw`sw-rounded-1`};
      color: ${themeColor('discreetButtonHover')};
      background: ${themeColor('discreetBackground')};
      outline: ${themeBorder('focus', 'discreetFocusBorder')};
      border-color: inherit;
      box-shadow: none;
    }
  }

  & .react-select__control--is-focused,
  & .react-select__control--menu-is-open {
    ${tw`sw-border-none`};
  }
`;
