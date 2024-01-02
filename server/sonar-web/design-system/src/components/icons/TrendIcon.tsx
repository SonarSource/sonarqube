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
import { themeColor } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

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
  const theme = useTheme();
  const { direction, type, ...iconProps } = props;

  const fill = themeColor(
    (
      {
        [TrendType.Positive]: 'iconTrendPositive',
        [TrendType.Negative]: 'iconTrendNegative',
        [TrendType.Neutral]: 'iconTrendNeutral',
        [TrendType.Disabled]: 'iconTrendDisabled',
      } as const
    )[type],
  )({ theme });

  return (
    <CustomIcon {...iconProps}>
      {direction === TrendDirection.Up && (
        <path
          aria-label="trend-up"
          clipRule="evenodd"
          d="M4.75802 4.3611a.74997.74997 0 0 1 .74953-.74953H11.518a.74985.74985 0 0 1 .5298.21967.74967.74967 0 0 1 .2197.52986v6.0104a.75043.75043 0 0 1-.2286.5132.75053.75053 0 0 1-.5209.2104.75017.75017 0 0 1-.5209-.2104.75054.75054 0 0 1-.2287-.5132V6.1713l-5.26085 5.2609a.75014.75014 0 0 1-1.06066 0 .75004.75004 0 0 1 0-1.0607l5.26088-5.26086H5.50755a.75001.75001 0 0 1-.74953-.74954Z"
          fill={fill}
          fillRule="evenodd"
        />
      )}
      {direction === TrendDirection.Down && (
        <path
          aria-label="trend-down"
          clipRule="evenodd"
          d="M11.5052 4.14237a.75026.75026 0 0 1 .5299.21967.74997.74997 0 0 1 .2196.52986v6.0104a.7501.7501 0 0 1-.2196.5299.75027.75027 0 0 1-.5299.2196H5.49479a.74976.74976 0 0 1-.51314-.2286.75004.75004 0 0 1 0-1.0418.74976.74976 0 0 1 .51314-.2286h4.20022L4.43413 4.8919a.75001.75001 0 0 1 1.06066-1.06066l5.26091 5.26087V4.8919a.74997.74997 0 0 1 .2196-.52986.75008.75008 0 0 1 .5299-.21967Z"
          fill={fill}
          fillRule="evenodd"
        />
      )}
      {direction === TrendDirection.Equal && (
        <g aria-label="trend-equal">
          <rect fill={fill} height="1.5" rx=".75" width="8" x="4" y="5" />
          <rect fill={fill} height="1.5" rx=".75" width="8" x="4" y="9" />
        </g>
      )}
    </CustomIcon>
  );
}
