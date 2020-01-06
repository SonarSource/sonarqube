/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as React from 'react';
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';
import { colors } from '../../app/theme';

const DonutChart = lazyLoadComponent(
  () => import('sonar-ui-common/components/charts/DonutChart'),
  'DonutChart'
);

const SIZE_TO_WIDTH_MAPPING = { small: 16, normal: 24, big: 40, huge: 60 };

const SIZE_TO_THICKNESS_MAPPING = { small: 2, normal: 3, big: 3, huge: 4 };

export interface CoverageRatingProps {
  muted?: boolean;
  size?: 'small' | 'normal' | 'big' | 'huge';
  value: number | string | null | undefined;
}

export default function CoverageRating({
  muted = false,
  size = 'normal',
  value
}: CoverageRatingProps) {
  let data = [{ value: 100, fill: '#ccc ' }];
  let padAngle = 0;

  if (value != null) {
    const numberValue = Number(value);
    data = [
      { value: numberValue, fill: muted ? colors.gray71 : colors.green },
      { value: 100 - numberValue, fill: muted ? colors.barBackgroundColor : colors.lineCoverageRed }
    ];
    if (numberValue !== 0 && numberValue < 100) {
      padAngle = 0.1; // Same for all sizes, because it scales automatically
    }
  }

  const width = SIZE_TO_WIDTH_MAPPING[size];
  const thickness = SIZE_TO_THICKNESS_MAPPING[size];

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
