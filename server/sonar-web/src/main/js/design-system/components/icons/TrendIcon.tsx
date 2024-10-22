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
import {
  IconArrowDownRight,
  IconArrowUpRight,
  IconEqual,
  IconProps,
} from '@sonarsource/echoes-react';
import { themeColor } from '../../helpers/theme';
import { CSSColor, ThemeColors } from '../../types';

export const enum TrendDirection {
  Down = 'down',
  Up = 'up',
  Equal = 'equal',
}

export const enum TrendType {
  Positive = 'positive',
  Negative = 'negative',
  Neutral = 'neutral',
  Disabled = 'disabled',
}

interface Props extends IconProps {
  direction: TrendDirection;
  type: TrendType;
}

export function TrendIcon(props: Readonly<Props>) {
  const { direction, type, ...iconProps } = props;

  if (direction === TrendDirection.Up) {
    return (
      <TrendIconWrapper trendType={type}>
        <IconArrowUpRight aria-label="trend-up" {...iconProps} />
      </TrendIconWrapper>
    );
  }

  if (direction === TrendDirection.Down) {
    return (
      <TrendIconWrapper trendType={type}>
        <IconArrowDownRight aria-label="trend-down" {...iconProps} />
      </TrendIconWrapper>
    );
  }

  return (
    <TrendIconWrapper trendType={type}>
      <IconEqual aria-label="trend-equal" {...iconProps} />
    </TrendIconWrapper>
  );
}

const ICON_COLORS: Record<TrendType, ThemeColors | CSSColor> = {
  [TrendType.Positive]: 'iconTrendPositive',
  [TrendType.Negative]: 'iconTrendNegative',
  [TrendType.Neutral]: 'iconTrendNeutral',
  [TrendType.Disabled]: 'var(--echoes-color-icon-disabled)',
};

const TrendIconWrapper = styled.span<{
  trendType: TrendType;
}>`
  color: ${({ trendType }) => themeColor(ICON_COLORS[trendType])};
`;
