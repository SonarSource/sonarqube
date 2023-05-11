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
import * as React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers';
import { ButtonProps, ButtonSecondary } from './buttons';

export type FacetItemProps = Omit<ButtonProps, 'name' | 'onClick'> & {
  active?: boolean;
  name: string;
  onClick: (x: string, multiple?: boolean) => void;
  stat?: React.ReactNode;
  /** Textual version of `name` */
  tooltip?: string;
  value: string;
};

export function FacetItem({
  active,
  className,
  disabled: disabledProp = false,
  icon,
  name,
  onClick,
  stat,
  tooltip,
  value,
}: FacetItemProps) {
  const disabled = disabledProp || (stat as number) === 0;

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();

    onClick(value, event.ctrlKey || event.metaKey);
  };

  return (
    <StyledButton
      active={active}
      className={className}
      data-facet={value}
      disabled={disabled}
      icon={icon}
      onClick={handleClick}
      role="listitem"
      title={tooltip}
    >
      <span className="container">
        <span className="name">{name}</span>
        <span className="stat">{stat}</span>
      </span>
    </StyledButton>
  );
}

const StyledButton = styled(ButtonSecondary)<{ active?: boolean }>`
  ${tw`sw-body-sm`};
  ${tw`sw-p-1`};
  ${tw`sw-rounded-1`};

  --background: ${({ active }) => (active ? themeColor('facetItemSelected') : 'transparent')};
  --backgroundHover: ${({ active }) => (active ? themeColor('facetItemSelected') : 'transparent')};

  --border: ${({ active }) =>
    active
      ? themeBorder('default', 'facetItemSelectedBorder')
      : themeBorder('default', 'transparent')};

  &:hover {
    --border: ${themeBorder('default', 'facetItemSelectedBorder')};
  }

  & span.container {
    ${tw`sw-container`};
    ${tw`sw-flex`};
    ${tw`sw-items-center`};
    ${tw`sw-justify-between`};

    & span.stat {
      color: ${themeColor('facetItemLight')};
    }
  }

  &:disabled {
    background-color: transparent;
    border-color: transparent;

    & span.container span.stat {
      color: ${themeContrast('buttonDisabled')};
    }

    &:hover {
      background-color: transparent;
      border-color: transparent;
    }
  }
`;
