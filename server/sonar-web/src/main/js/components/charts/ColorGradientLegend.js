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
// @flow
import React from 'react';

/*:: type Props = {
  className?: string,
  colorScale: Object,
  colorNA?: string,
  direction?: number,
  padding?: Array<number>,
  height: number,
  width: number
}; */

const NA_SPACING = 4;

export default function ColorGradientLegend(
  {
    className,
    colorScale,
    colorNA,
    direction,
    padding = [12, 24, 0, 0],
    height,
    width
  } /*: Props */
) {
  const colorRange = colorScale.range();
  if (direction === 1) {
    colorRange.reverse();
  }

  const colorDomain = colorScale.domain();
  const lastColorIdx = colorRange.length - 1;
  const lastDomainIdx = colorDomain.length - 1;
  const widthNoPadding = width - padding[1];
  const rectHeight = height - padding[0];
  return (
    <svg className={className} width={width} height={height}>
      <defs>
        <linearGradient id="gradient-legend">
          {colorRange.map((color, idx) => (
            <stop key={idx} offset={idx / lastColorIdx} stopColor={color} />
          ))}
        </linearGradient>
      </defs>
      <g transform={`translate(${padding[3]}, ${padding[0]})`}>
        <rect fill="url(#gradient-legend)" x={0} y={0} height={rectHeight} width={widthNoPadding} />
        {colorDomain.map((d, idx) => (
          <text
            className="gradient-legend-text"
            key={idx}
            x={widthNoPadding * (idx / lastDomainIdx)}
            y={0}
            dy="-2px">
            {d}
          </text>
        ))}
      </g>
      {colorNA && (
        <g transform={`translate(${widthNoPadding}, ${padding[0]})`}>
          <rect
            fill={colorNA}
            x={NA_SPACING}
            y={0}
            height={rectHeight}
            width={padding[1] - NA_SPACING}
          />
          <text
            className="gradient-legend-na"
            x={NA_SPACING + (padding[1] - NA_SPACING) / 2}
            y={0}
            dy="-2px">
            N/A
          </text>
        </g>
      )}
    </svg>
  );
}
