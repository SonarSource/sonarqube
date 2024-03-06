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
import { ScaleLinear, ScaleOrdinal } from 'd3-scale';
import { CSSColor, themeColor } from 'design-system';
import * as React from 'react';

interface Props {
  className?: string;
  colorScale:
    | ScaleOrdinal<string, string> // used for LEVEL type
    | ScaleLinear<string, string | number>; // used for RATING or PERCENT type
  naColors?: [CSSColor, CSSColor];
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
  naColors = ['rgb(36,36,36)', 'rgb(120,120,120)'],
  width,
}: Props) {
  const colorRange: Array<string | number> = colorScale.range();
  const colorDomain: Array<string | number> = colorScale.domain();
  const lastColorIdx = colorRange.length - 1;
  const lastDomainIdx = colorDomain.length - 1;
  const widthNoPadding = width - padding[1];
  const rectHeight = height - padding[0];
  return (
    <svg className={className} height={height} width={width}>
      <defs>
        <linearGradient id="gradient-legend">
          {colorRange.map((color, idx) => (
            // eslint-disable-next-line react/no-array-index-key
            <stop key={idx} offset={idx / lastColorIdx} stopColor={String(color)} />
          ))}
        </linearGradient>

        <pattern
          id="stripes"
          width="30"
          height="30"
          patternTransform="rotate(45 0 0)"
          patternUnits="userSpaceOnUse"
        >
          {NA_GRADIENT_LINE_INCREMENTS.map((i) => (
            <React.Fragment key={i}>
              <line
                x1={i}
                y1="0"
                x2={i}
                y2="30"
                style={{ stroke: naColors[0], strokeWidth: NA_SPACING }}
              />
              <line
                x1={i + NA_SPACING}
                y1="0"
                x2={i + NA_SPACING}
                y2="30"
                style={{ stroke: naColors[1], strokeWidth: NA_SPACING }}
              />
            </React.Fragment>
          ))}
        </pattern>
      </defs>
      <g transform={`translate(${padding[3]}, ${padding[0]})`}>
        <rect fill="url(#gradient-legend)" height={rectHeight} width={widthNoPadding} x={0} y={0} />
        {colorDomain.map((d, idx) => (
          <GradientLegendText
            dy="-2px"
            // eslint-disable-next-line react/no-array-index-key
            key={idx}
            x={widthNoPadding * (idx / lastDomainIdx)}
            y={0}
          >
            {d}
          </GradientLegendText>
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
          <GradientLegendTextBase dy="-2px" x={NA_SPACING + (padding[1] - NA_SPACING) / 2} y={0}>
            N/A
          </GradientLegendTextBase>
        </g>
      )}
    </svg>
  );
}

const GradientLegendTextBase = styled.text`
  text-anchor: middle;
  fill: ${themeColor('pageContent')};
  font-size: 10px;
`;

const GradientLegendText = styled(GradientLegendTextBase)`
  &:first-of-type {
    text-anchor: start;
  }

  &:last-of-type {
    text-anchor: end;
  }
`;
