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
import { themeColor } from '../helpers/theme';
import { DonutChart } from './DonutChart';
import { NoDataIcon } from './icons';

const SIZE_TO_WIDTH_MAPPING = { xs: 16, sm: 24, md: 36 };
const SIZE_TO_THICKNESS_MAPPING = { xs: 2, sm: 3, md: 4 };
const FULL_PERCENT = 100;
const PAD_ANGLE = 0.1;

export interface CoverageIndicatorProps {
  size?: 'xs' | 'sm' | 'md';
  value?: number | string;
}

export function CoverageIndicator({ size = 'sm', value }: CoverageIndicatorProps) {
  const theme = useTheme();
  const width = SIZE_TO_WIDTH_MAPPING[size];
  const thickness = SIZE_TO_THICKNESS_MAPPING[size];

  if (value === undefined) {
    return <NoDataIcon height={width} width={width} />;
  }

  const themeRed = themeColor('coverageRed')({ theme });
  const themeGreen = themeColor('coverageGreen')({ theme });

  let padAngle = 0;
  const numberValue = Number(value || 0);
  const data = [
    { value: numberValue, fill: themeGreen },
    {
      value: FULL_PERCENT - numberValue,
      fill: themeRed,
    },
  ];
  if (numberValue !== 0 && numberValue < FULL_PERCENT) {
    padAngle = PAD_ANGLE; // Same for all sizes, because it scales automatically
  }

  return (
    <DonutChart
      data={data}
      height={width}
      padAngle={padAngle}
      thickness={thickness}
      width={width}
    />
  );
}
