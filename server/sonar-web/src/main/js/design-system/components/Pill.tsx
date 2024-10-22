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
  Success = 'success',
  Neutral = 'neutral',
}

export enum PillHighlight {
  Medium = 'medium',
  Low = 'low',
}

const variantThemeColors: Record<PillVariant, ThemeColors> = {
  [PillVariant.Critical]: 'pillCritical',
  [PillVariant.Danger]: 'pillDanger',
  [PillVariant.Warning]: 'pillWarning',
  [PillVariant.Caution]: 'pillCaution',
  [PillVariant.Info]: 'pillInfo',
  [PillVariant.Accent]: 'pillAccent',
  [PillVariant.Success]: 'pillSuccess',
  [PillVariant.Neutral]: 'pillNeutral',
};

const variantThemeBorderColors: Record<PillVariant, ThemeColors> = {
  [PillVariant.Critical]: 'pillCriticalBorder',
  [PillVariant.Danger]: 'pillDangerBorder',
  [PillVariant.Warning]: 'pillWarningBorder',
  [PillVariant.Caution]: 'pillCautionBorder',
  [PillVariant.Info]: 'pillInfoBorder',
  [PillVariant.Accent]: 'pillAccentBorder',
  [PillVariant.Success]: 'pillSuccessBorder',
  [PillVariant.Neutral]: 'pillNeutralBorder',
};

const variantThemeHoverColors: Record<PillVariant, ThemeColors> = {
  [PillVariant.Critical]: 'pillCriticalHover',
  [PillVariant.Danger]: 'pillDangerHover',
  [PillVariant.Warning]: 'pillWarningHover',
  [PillVariant.Caution]: 'pillCautionHover',
  [PillVariant.Info]: 'pillInfoHover',
  [PillVariant.Accent]: 'pillAccentHover',
  [PillVariant.Success]: 'pillSuccessHover',
  [PillVariant.Neutral]: 'pillNeutralHover',
};

interface PillProps {
  ['aria-label']?: string;
  children: ReactNode;
  className?: string;
  highlight?: PillHighlight;
  // If pill is wrapped with Tooltip, it will have onClick prop overriden.
  // So to avoid hover effect, we add additional prop to disable hover effect even with onClick.
  notClickable?: boolean;
  onClick?: () => void;
  variant: PillVariant;
}

// eslint-disable-next-line react/display-name
export const Pill = forwardRef<HTMLButtonElement, Readonly<PillProps>>(
  ({ children, variant, highlight = PillHighlight.Low, onClick, notClickable, ...rest }, ref) => {
    return onClick && !notClickable ? (
      <StyledPillButton onClick={onClick} ref={ref} variant={variant} {...rest}>
        {children}
      </StyledPillButton>
    ) : (
      <StyledPill highlight={highlight} ref={ref} variant={variant} {...rest}>
        {children}
      </StyledPill>
    );
  },
);

const reusedStyles = css`
  ${tw`sw-typo-sm`};
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
  highlight: PillHighlight;
  variant: PillVariant;
}>`
  ${reusedStyles};

  background-color: ${({ variant, highlight }) =>
    highlight === PillHighlight.Medium && themeColor(variantThemeColors[variant])};
  color: ${({ variant }) => themeContrast(variantThemeColors[variant])};
  border-style: ${({ highlight }) => (highlight === PillHighlight.Medium ? 'hidden' : 'solid')};
  border-color: ${({ variant, highlight }) =>
    highlight === PillHighlight.Low && themeColor(variantThemeBorderColors[variant])};
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

  &:hover {
    background-color: ${({ variant }) => themeColor(variantThemeHoverColors[variant])};
  }

  &:focus {
    outline: var(--echoes-color-focus-default) solid var(--echoes-focus-border-width-default);
    outline-offset: var(--echoes-focus-border-offset-default);
  }
`;
