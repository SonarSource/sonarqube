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
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';

interface Props {
  colorNA?: string;
  colorScale:
    | ScaleOrdinal<string, string> // used for LEVEL type
    | ScaleLinear<string, string | number>; // used for RATING or PERCENT type
  metricType: string;
}

export default function ColorBoxLegend({ colorScale, colorNA, metricType }: Props) {
  const colorDomain: Array<number | string> = colorScale.domain();
  const colorRange = colorScale.range();
  return (
    <div className="sw-flex sw-justify-center sw-gap-6">
      {colorDomain.map((value, idx) => (
        <div key={value}>
          <LegendRect style={{ borderColor: colorRange[idx] }}>
            <span style={{ backgroundColor: colorRange[idx] }} />
          </LegendRect>
          {formatMeasure(value, metricType)}
        </div>
      ))}
      {colorNA && (
        <div>
          <LegendRect style={{ borderColor: colorNA }}>
            <span style={{ backgroundColor: colorNA }} />
          </LegendRect>
          N/A
        </div>
      )}
    </div>
  );
}

const LegendRect = styled.span`
  display: inline-block;
  margin-top: 1px;
  margin-right: 4px;
  border: 1px solid;

  & span {
    display: block;
    width: 8px;
    height: 8px;
  }
`;
