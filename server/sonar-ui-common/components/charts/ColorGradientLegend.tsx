/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { ScaleLinear, ScaleOrdinal } from 'd3-scale';
import * as React from 'react';
import { ThemeConsumer } from '../theme';
import './ColorGradientLegend.css';

interface Props {
  className?: string;
  colorScale:
    | ScaleOrdinal<string, string> // used for LEVEL type
    | ScaleLinear<string, string | number>; // used for RATING or PERCENT type
  height: number;
  padding?: [number, number, number, number];
  showColorNA?: boolean;
  width: number;
}

const NA_SPACING = 4;
const NA_GRADIENT_LINE_INCREMENTS = [0, 8, 16, 24];

export default function ColorGradientLegend({
  className,
  colorScale,
  padding = [12, 24, 0, 0],
  height,
  showColorNA = false,
  width,
}: Props) {
  const colorRange: Array<string | number> = colorScale.range();
  const colorDomain: Array<string | number> = colorScale.domain();
  const lastColorIdx = colorRange.length - 1;
  const lastDomainIdx = colorDomain.length - 1;
  const widthNoPadding = width - padding[1];
  const rectHeight = height - padding[0];
  return (
    <ThemeConsumer>
      {({ colors }) => (
        <svg className={className} height={height} width={width}>
          <defs>
            <linearGradient id="gradient-legend">
              {colorRange.map((color, idx) => (
                <stop key={idx} offset={idx / lastColorIdx} stopColor={String(color)} />
              ))}
            </linearGradient>

            <pattern
              id="stripes"
              width="30"
              height="30"
              patternTransform="rotate(45 0 0)"
              patternUnits="userSpaceOnUse">
              {NA_GRADIENT_LINE_INCREMENTS.map((i) => (
                <React.Fragment key={i}>
                  <line
                    x1={i}
                    y1="0"
                    x2={i}
                    y2="30"
                    style={{ stroke: colors.gray71, strokeWidth: NA_SPACING }}
                  />
                  <line
                    x1={i + NA_SPACING}
                    y1="0"
                    x2={i + NA_SPACING}
                    y2="30"
                    style={{ stroke: colors.gray60, strokeWidth: NA_SPACING }}
                  />
                </React.Fragment>
              ))}
            </pattern>
          </defs>
          <g transform={`translate(${padding[3]}, ${padding[0]})`}>
            <rect
              fill="url(#gradient-legend)"
              height={rectHeight}
              width={widthNoPadding}
              x={0}
              y={0}
            />
            {colorDomain.map((d, idx) => (
              <text
                className="gradient-legend-text"
                dy="-2px"
                key={idx}
                x={widthNoPadding * (idx / lastDomainIdx)}
                y={0}>
                {d}
              </text>
            ))}
          </g>
          {showColorNA && (
            <g transform={`translate(${widthNoPadding}, ${padding[0]})`}>
              <rect
                fill="url(#stripes)"
                height={rectHeight}
                width={padding[1] - NA_SPACING}
                x={NA_SPACING}
                y={0}
              />
              <text
                className="gradient-legend-na"
                dy="-2px"
                x={NA_SPACING + (padding[1] - NA_SPACING) / 2}
                y={0}>
                N/A
              </text>
            </g>
          )}
        </svg>
      )}
    </ThemeConsumer>
  );
}
