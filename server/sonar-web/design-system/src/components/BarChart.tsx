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
import { max } from 'd3-array';
import { ScaleBand, ScaleLinear, scaleBand, scaleLinear } from 'd3-scale';
import { themeColor } from '../helpers';

interface DataPoint {
  description: string;
  tooltip?: string;
  x: number;
  y: number;
}

interface Props<T> {
  barsWidth: number;
  data: Array<DataPoint & T>;
  height: number;
  onBarClick: (point: DataPoint & T) => void;
  padding?: [number, number, number, number];
  width: number;
  xValues?: string[];
}

export function BarChart<T>(props: Props<T>) {
  const { barsWidth, data, width, height, padding = [10, 10, 10, 10], xValues } = props;

  const availableWidth = width - padding[1] - padding[3];
  const availableHeight = height - padding[0] - padding[2];

  const innerPadding = (availableWidth - barsWidth * data.length) / (data.length - 1);
  const relativeInnerPadding = innerPadding / (innerPadding + barsWidth);

  const maxY = max(data, (d) => d.y) as number;
  const xScale = scaleBand<number>()
    .domain(data.map((d) => d.x))
    .range([0, availableWidth])
    .paddingInner(relativeInnerPadding);
  const yScale = scaleLinear().domain([0, maxY]).range([availableHeight, 0]);

  return (
    <svg className="bar-chart" height={height} width={width}>
      <g transform={`translate(${padding[3]}, ${padding[0]})`}>
        <Xvalues
          data={data}
          onBarClick={props.onBarClick}
          xScale={xScale}
          xValues={xValues}
          yScale={yScale}
        />
        <Bars
          barsWidth={barsWidth}
          data={data}
          onBarClick={props.onBarClick}
          xScale={xScale}
          yScale={yScale}
        />
      </g>
    </svg>
  );
}

function Xvalues<T>(
  props: {
    xScale: ScaleBand<number>;
    yScale: ScaleLinear<number, number>;
  } & Pick<Props<T>, 'data' | 'xValues' | 'onBarClick'>,
) {
  const { data, xValues = [], xScale, yScale } = props;

  if (!xValues.length) {
    return null;
  }

  const ticks = xValues.map((value, index) => {
    const point = data[index];
    const x = Math.round((xScale(point.x) as number) + xScale.bandwidth() / 2);
    const y = yScale(point.y);

    return (
      <BarChartTick
        className="sw-body-sm sw-cursor-pointer"
        dy="-0.5em"
        // eslint-disable-next-line react/no-array-index-key
        key={index}
        onClick={() => {
          props.onBarClick(point);
        }}
        x={x}
        y={y}
      >
        {point.tooltip && <title>{point.tooltip}</title>}
        {value}
      </BarChartTick>
    );
  });
  return <g>{ticks}</g>;
}

function Bars<T>(
  props: {
    xScale: ScaleBand<number>;
    yScale: ScaleLinear<number, number>;
  } & Pick<Props<T>, 'data' | 'barsWidth' | 'onBarClick'>,
) {
  const { barsWidth, data, xScale, yScale } = props;

  const bars = data.map((point, index) => {
    const x = Math.round(xScale(point.x) as number);
    const maxY = yScale.range()[0];
    const y = Math.round(yScale(point.y)) - /* minimum bar height */ 1;
    const height = maxY - y;
    const rect = (
      <BarChartBar
        aria-label={point.description}
        className="sw-cursor-pointer"
        height={height}
        // eslint-disable-next-line react/no-array-index-key
        key={index}
        onClick={() => {
          props.onBarClick(point);
        }}
        width={barsWidth}
        x={x}
        y={y}
      >
        <title>{point.tooltip}</title>
      </BarChartBar>
    );
    return rect;
  });
  return <g>{bars}</g>;
}

const BarChartTick = styled.text`
  fill: ${themeColor('pageContentLight')};
  text-anchor: middle;
`;

const BarChartBar = styled.rect`
  fill: ${themeColor('primary')};

  &:hover {
    fill: ${themeColor('primaryDark')};
  }
`;
