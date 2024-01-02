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
import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import React from 'react';
import tw from 'twin.macro';
import { themeAvatarColor } from '../../helpers/theme';
import { IconProps } from '../icons/Icon';
import { Size, iconSizeMap, sizeMap } from './utils';

export interface GenericAvatarProps {
  Icon?: React.ComponentType<React.PropsWithChildren<IconProps>>;
  className?: string;
  name: string;
  size?: Size;
}

export function GenericAvatar({
  className,
  Icon,
  name,
  size = 'sm',
}: Readonly<GenericAvatarProps>) {
  const theme = useTheme();
  const text = name.length > 0 ? name[0].toUpperCase() : '';

  const iconSize = iconSizeMap[size];

  return (
    <StyledGenericAvatar
      aria-label={name}
      className={className}
      name={name}
      role="img"
      size={sizeMap[size]}
    >
      {Icon ? (
        <Icon fill={themeAvatarColor(name, true)({ theme })} height={iconSize} width={iconSize} />
      ) : (
        text
      )}
    </StyledGenericAvatar>
  );
}

export const StyledGenericAvatar = styled.div<{ name: string; size: number }>`
  ${tw`sw-text-center`};
  ${tw`sw-align-top`};
  ${tw`sw-select-none`};
  ${tw`sw-font-regular`};
  ${tw`sw-rounded-1`};
  ${tw`sw-inline-flex`};
  ${tw`sw-items-center`};
  ${tw`sw-justify-center`};
  height: ${({ size }) => size}px;
  width: ${({ size }) => size}px;
  background-color: ${({ name, theme }) => themeAvatarColor(name)({ theme })};
  color: ${({ name, theme }) => themeAvatarColor(name, true)({ theme })};
  font-size: ${({ size }) => Math.max(Math.floor(size / 2), 8)}px;
  line-height: ${({ size }) => size}px;
`;
