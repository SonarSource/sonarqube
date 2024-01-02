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
import * as React from 'react';
import { colors } from '../../app/theme';
import DonutChart from '../../components/charts/DonutChart';

const SIZE_TO_WIDTH_MAPPING = { small: 20, normal: 24, big: 40, huge: 60 };
const SIZE_TO_THICKNESS_MAPPING = { small: 3, normal: 3, big: 3, huge: 4 };

const FULL_PERCENT = 100;

type SIZE = 'small' | 'normal' | 'big' | 'huge';

export interface CoverageRatingProps {
  muted?: boolean;
  size?: SIZE;
  value?: number | string;
}

export default function CoverageRating({
  muted = false,
  size = 'normal',
  value,
}: CoverageRatingProps) {
  let data = [{ value: FULL_PERCENT, fill: colors.gray71 }];
  let padAngle = 0;

  if (value != null) {
    const numberValue = Number(value);
    data = [
      { value: numberValue, fill: muted ? colors.gray71 : colors.success500 },
      { value: FULL_PERCENT - numberValue, fill: muted ? 'transparent' : colors.error500 },
    ];
    if (numberValue !== 0 && numberValue < FULL_PERCENT) {
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
