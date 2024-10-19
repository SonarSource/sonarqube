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
import { ScaleLinear, ScalePoint, ScaleTime } from 'd3-scale';
import * as React from 'react';
import { shouldShowSplitLine } from '../../helpers/activity-graph';

interface Props {
  splitPointDate?: Date;
  xScale: ScaleTime<number, number>;
  yScale: ScaleLinear<number, number> | ScalePoint<number | string>;
}

export default function SplitLine({ splitPointDate, xScale, yScale }: Readonly<Props>) {
  const showSplitLine = shouldShowSplitLine(splitPointDate, xScale);

  if (!showSplitLine) {
    return null;
  }

  return (
    <line
      className="line-tooltip"
      strokeDasharray="2"
      x1={xScale(splitPointDate)}
      x2={xScale(splitPointDate)}
      y1={yScale.range()[0]}
      y2={yScale.range()[1] - 10}
    />
  );
}
