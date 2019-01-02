/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { arc as d3Arc, pie as d3Pie, PieArcDatum } from 'd3-shape';

interface DataPoint {
  fill: string;
  value: number;
}

interface Props {
  data: DataPoint[];
  height: number;
  thickness: number;
  padding?: [number, number, number, number];
  width: number;
}

export default function DonutChart(props: Props) {
  const { height, padding = [0, 0, 0, 0], width } = props;

  const availableWidth = width - padding[1] - padding[3];
  const availableHeight = height - padding[0] - padding[2];

  const size = Math.min(availableWidth, availableHeight);
  const radius = Math.floor(size / 2);

  const pie = d3Pie<any, DataPoint>()
    .sort(null)
    .value(d => d.value);

  const sectors = pie(props.data).map((d, i) => {
    return (
      <Sector
        data={d}
        fill={props.data[i].fill}
        key={i}
        radius={radius}
        thickness={props.thickness}
      />
    );
  });

  return (
    <svg className="donut-chart" height={height} width={width}>
      <g transform={`translate(${padding[3]}, ${padding[0]})`}>
        <g transform={`translate(${radius}, ${radius})`}>{sectors}</g>
      </g>
    </svg>
  );
}

interface SectorProps {
  data: PieArcDatum<DataPoint>;
  fill: string;
  radius: number;
  thickness: number;
}

function Sector(props: SectorProps) {
  const arc = d3Arc<any, PieArcDatum<DataPoint>>()
    .outerRadius(props.radius)
    .innerRadius(props.radius - props.thickness);
  const d = arc(props.data) as string;
  return <path d={d} style={{ fill: props.fill }} />;
}
