/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { DonutChart } from '../charts/donut-chart';
import * as theme from '../../app/theme';

const SIZE_TO_WIDTH_MAPPING = { small: 16, normal: 24, big: 40, huge: 60 };

const SIZE_TO_THICKNESS_MAPPING = { small: 2, normal: 3, big: 3, huge: 4 };

interface Props {
  muted?: boolean;
  size?: 'small' | 'normal' | 'big' | 'huge';
  value: number | string | null | undefined;
}

export default function CoverageRating({ muted = false, size = 'normal', value }: Props) {
  let data = [{ value: 100, fill: '#ccc ' }];

  if (value != null) {
    const numberValue = Number(value);
    data = [
      { value: numberValue, fill: muted ? theme.gray71 : theme.green },
      { value: 100 - numberValue, fill: muted ? theme.barBackgroundColor : theme.red }
    ];
  }

  const width = SIZE_TO_WIDTH_MAPPING[size];
  const thickness = SIZE_TO_THICKNESS_MAPPING[size];

  return <DonutChart data={data} width={width} height={width} thickness={thickness} />;
}
