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
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { ThemeColors } from '../types/theme';

type PillVariant = 'danger' | 'warning' | 'info' | 'neutral';

const variantThemeColors: Record<PillVariant, ThemeColors> = {
  danger: 'pillDanger',
  warning: 'pillWarning',
  info: 'pillInfo',
  neutral: 'pillNeutral',
};

interface PillProps {
  ['aria-label']?: string;
  children: ReactNode;
  className?: string;
  variant: PillVariant;
}

export function Pill({ children, variant, ...rest }: PillProps) {
  return (
    <StyledPill color={variantThemeColors[variant]} {...rest}>
      {children}
    </StyledPill>
  );
}

const StyledPill = styled.span<{
  color: ThemeColors;
}>`
  ${tw`sw-cursor-pointer`};
  ${tw`sw-body-sm`};
  ${tw`sw-w-fit`};
  ${tw`sw-inline-block`};
  ${tw`sw-whitespace-nowrap`};
  ${tw`sw-px-[8px] sw-py-[2px]`};
  ${tw`sw-rounded-pill`};

  color: ${({ color }) => themeContrast(color)};
  background-color: ${({ color }) => themeColor(color)};

  &:empty {
    ${tw`sw-hidden`}
  }
`;
