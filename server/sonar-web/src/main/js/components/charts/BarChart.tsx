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
import { max } from 'd3-array';
import { scaleLinear, scaleBand, ScaleLinear, ScaleBand } from 'd3-scale';
import Tooltip from '../controls/Tooltip';
import './BarChart.css';

interface DataPoint {
  tooltip?: React.ReactNode;
  x: number;
  y: number;
}

interface Props<T> {
  barsWidth: number;
  data: Array<DataPoint & T>;
  height: number;
  onBarClick?: (point: DataPoint & T) => void;
  padding?: [number, number, number, number];
  width: number;
  xTicks?: string[];
  xValues?: string[];
}

export default class BarChart<T> extends React.PureComponent<Props<T>> {
  handleClick = (point: DataPoint & T) => {
    if (this.props.onBarClick) {
      this.props.onBarClick(point);
    }
  };

  renderXTicks = (xScale: ScaleBand<number>, yScale: ScaleLinear<number, number>) => {
    const { data, xTicks = [] } = this.props;

    if (!xTicks.length) {
      return null;
    }

    const ticks = xTicks.map((tick, index) => {
      const point = data[index];
      const x = Math.round((xScale(point.x) as number) + xScale.bandwidth() / 2);
      const y = yScale.range()[0];
      const d = data[index];
      const text = (
        <text
          className="bar-chart-tick"
          dy="1.5em"
          key={index}
          onClick={() => this.handleClick(point)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}
          x={x}
          y={y}>
          {tick}
        </text>
      );
      return (
        <Tooltip key={index} overlay={d.tooltip || undefined}>
          {text}
        </Tooltip>
      );
    });
    return <g>{ticks}</g>;
  };

  renderXValues = (xScale: ScaleBand<number>, yScale: ScaleLinear<number, number>) => {
    const { data, xValues = [] } = this.props;

    if (!xValues.length) {
      return null;
    }

    const ticks = xValues.map((value, index) => {
      const point = data[index];
      const x = Math.round((xScale(point.x) as number) + xScale.bandwidth() / 2);
      const y = yScale(point.y);
      const text = (
        <text
          className="bar-chart-tick"
          dy="-1em"
          key={index}
          onClick={() => this.handleClick(point)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}
          x={x}
          y={y}>
          {value}
        </text>
      );
      return (
        <Tooltip key={index} overlay={point.tooltip || undefined}>
          {text}
        </Tooltip>
      );
    });
    return <g>{ticks}</g>;
  };

  renderBars = (xScale: ScaleBand<number>, yScale: ScaleLinear<number, number>) => {
    const bars = this.props.data.map((point, index) => {
      const x = Math.round(xScale(point.x) as number);
      const maxY = yScale.range()[0];
      const y = Math.round(yScale(point.y)) - /* minimum bar height */ 1;
      const height = maxY - y;
      const rect = (
        <rect
          className="bar-chart-bar"
          height={height}
          key={index}
          onClick={() => this.handleClick(point)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}
          width={this.props.barsWidth}
          x={x}
          y={y}
        />
      );
      return (
        <Tooltip key={index} overlay={point.tooltip || undefined}>
          {rect}
        </Tooltip>
      );
    });
    return <g>{bars}</g>;
  };

  render() {
    const { barsWidth, data, width, height, padding = [10, 10, 10, 10] } = this.props;

    const availableWidth = width - padding[1] - padding[3];
    const availableHeight = height - padding[0] - padding[2];

    const innerPadding = (availableWidth - barsWidth * data.length) / (data.length - 1);
    const relativeInnerPadding = innerPadding / (innerPadding + barsWidth);

    const maxY = max(data, d => d.y) as number;
    const xScale = scaleBand<number>()
      .domain(data.map(d => d.x))
      .range([0, availableWidth])
      .paddingInner(relativeInnerPadding);
    const yScale = scaleLinear()
      .domain([0, maxY])
      .range([availableHeight, 0]);

    return (
      <svg className="bar-chart" height={height} width={width}>
        <g transform={`translate(${padding[3]}, ${padding[0]})`}>
          {this.renderXTicks(xScale, yScale)}
          {this.renderXValues(xScale, yScale)}
          {this.renderBars(xScale, yScale)}
        </g>
      </svg>
    );
  }
}
