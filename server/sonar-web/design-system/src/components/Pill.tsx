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
import { forwardRef, ReactNode } from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { ThemeColors } from '../types/theme';

export enum PillVariant {
  Critical = 'critical',
  Danger = 'danger',
  Warning = 'warning',
  Caution = 'caution',
  Info = 'info',
  Accent = 'accent',
}

const variantThemeColors: Record<PillVariant, ThemeColors> = {
  [PillVariant.Critical]: 'pillCritical',
  [PillVariant.Danger]: 'pillDanger',
  [PillVariant.Warning]: 'pillWarning',
  [PillVariant.Caution]: 'pillCaution',
  [PillVariant.Info]: 'pillInfo',
  [PillVariant.Accent]: 'pillAccent',
};

const variantThemeBorderColors: Record<PillVariant, ThemeColors> = {
  [PillVariant.Critical]: 'pillCriticalBorder',
  [PillVariant.Danger]: 'pillDangerBorder',
  [PillVariant.Warning]: 'pillWarningBorder',
  [PillVariant.Caution]: 'pillCautionBorder',
  [PillVariant.Info]: 'pillInfoBorder',
  [PillVariant.Accent]: 'pillAccentBorder',
};

interface PillProps {
  ['aria-label']?: string;
  children: ReactNode;
  className?: string;
  onClick?: () => void;
  variant: PillVariant;
}

// eslint-disable-next-line react/display-name
export const Pill = forwardRef<HTMLButtonElement, Readonly<PillProps>>(
  ({ children, variant, onClick, ...rest }, ref) => {
    return onClick ? (
      <StyledPillButton onClick={onClick} ref={ref} variant={variant} {...rest}>
        {children}
      </StyledPillButton>
    ) : (
      <StyledPill ref={ref} variant={variant} {...rest}>
        {children}
      </StyledPill>
    );
  },
);

const reusedStyles = css`
  ${tw`sw-body-xs`};
  ${tw`sw-w-fit`};
  ${tw`sw-inline-block`};
  ${tw`sw-whitespace-nowrap`};
  ${tw`sw-px-[8px] sw-py-[2px]`};
  ${tw`sw-rounded-pill`};
  border-width: 1px;

  &:empty {
    ${tw`sw-hidden`}
  }
`;

const StyledPill = styled.span<{
  variant: PillVariant;
}>`
  ${reusedStyles};

  background-color: ${({ variant }) => themeColor(variantThemeColors[variant])};
  color: ${({ variant }) => themeContrast(variantThemeColors[variant])};
  border-style: ${({ variant }) => (variant === PillVariant.Accent ? 'hidden' : 'solid')};
  border-color: ${({ variant }) => themeColor(variantThemeBorderColors[variant])};
`;

const StyledPillButton = styled.button<{
  variant: PillVariant;
}>`
  ${reusedStyles};

  background-color: ${({ variant }) => themeColor(variantThemeColors[variant])};
  color: ${({ variant }) => themeContrast(variantThemeColors[variant])};
  border-style: ${({ variant }) => (variant === PillVariant.Accent ? 'hidden' : 'solid')};
  border-color: ${({ variant }) => themeColor(variantThemeBorderColors[variant])};

  cursor: pointer;
`;
