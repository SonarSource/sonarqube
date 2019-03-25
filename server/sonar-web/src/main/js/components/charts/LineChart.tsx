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
import { extent, max } from 'd3-array';
import { scaleLinear, ScaleLinear } from 'd3-scale';
import { area as d3Area, line as d3Line, curveBasis } from 'd3-shape';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import './LineChart.css';

interface DataPoint {
  x: number;
  y?: number;
}

interface Props {
  backdropConstraints?: [number, number];
  data: DataPoint[];
  displayBackdrop?: boolean;
  displayPoints?: boolean;
  displayVerticalGrid?: boolean;
  domain?: [number, number];
  height: number;
  padding?: [number, number, number, number];
  width?: number;
  xTicks?: {}[];
  xValues?: {}[];
}

export default class LineChart extends React.PureComponent<Props> {
  renderBackdrop(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const { displayBackdrop = true } = this.props;

    if (!displayBackdrop) {
      return null;
    }

    const area = d3Area<DataPoint>()
      .x(d => xScale(d.x))
      .y0(yScale.range()[0])
      .y1(d => yScale(d.y || 0))
      .defined(d => d.y != null)
      .curve(curveBasis);

    let { data } = this.props;
    if (this.props.backdropConstraints) {
      const c = this.props.backdropConstraints;
      data = data.filter(d => c[0] <= d.x && d.x <= c[1]);
    }

    return <path className="line-chart-backdrop" d={area(data) as string} />;
  }

  renderPoints(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const { displayPoints = true } = this.props;

    if (!displayPoints) {
      return null;
    }

    const points = this.props.data
      .filter(point => point.y != null)
      .map((point, index) => {
        const x = xScale(point.x);
        const y = yScale(point.y || 0);
        return <circle className="line-chart-point" cx={x} cy={y} key={index} r="3" />;
      });
    return <g>{points}</g>;
  }

  renderVerticalGrid(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const { displayVerticalGrid = true } = this.props;

    if (!displayVerticalGrid) {
      return null;
    }

    const lines = this.props.data.map((point, index) => {
      const x = xScale(point.x);
      const y1 = yScale.range()[0];
      const y2 = yScale(point.y || 0);
      return <line className="line-chart-grid" key={index} x1={x} x2={x} y1={y1} y2={y2} />;
    });
    return <g>{lines}</g>;
  }

  renderXTicks(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const { xTicks = [] } = this.props;

    if (!xTicks.length) {
      return null;
    }

    const ticks = xTicks.map((tick, index) => {
      const point = this.props.data[index];
      const x = xScale(point.x);
      const y = yScale.range()[0];
      return (
        <text className="line-chart-tick" dy="1.5em" key={index} x={x} y={y}>
          {tick}
        </text>
      );
    });
    return <g>{ticks}</g>;
  }

  renderXValues(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const { xValues = [] } = this.props;

    if (!xValues.length) {
      return null;
    }

    const ticks = xValues.map((value, index) => {
      const point = this.props.data[index];
      const x = xScale(point.x);
      const y = yScale(point.y || 0);
      return (
        <text className="line-chart-tick" dy="-1em" key={index} x={x} y={y}>
          {value}
        </text>
      );
    });
    return <g>{ticks}</g>;
  }

  renderLine(xScale: ScaleLinear<number, number>, yScale: ScaleLinear<number, number>) {
    const p = d3Line<DataPoint>()
      .x(d => xScale(d.x))
      .y(d => yScale(d.y || 0))
      .defined(d => d.y != null)
      .curve(curveBasis);
    return <path className="line-chart-path" d={p(this.props.data) as string} />;
  }

  renderChart = (width: number) => {
    const { height, padding = [10, 10, 10, 10] } = this.props;

    if (!width || !height) {
      return <div />;
    }

    const availableWidth = width - padding[1] - padding[3];
    const availableHeight = height - padding[0] - padding[2];

    const xScale = scaleLinear()
      .domain(extent(this.props.data, d => d.x) as [number, number])
      .range([0, availableWidth]);
    const yScale = scaleLinear().range([availableHeight, 0]);

    if (this.props.domain) {
      yScale.domain(this.props.domain);
    } else {
      const maxY = max(this.props.data, d => d.y) as number;
      yScale.domain([0, maxY]);
    }

    return (
      <svg className="line-chart" height={height} width={width}>
        <g transform={`translate(${padding[3]}, ${padding[0]})`}>
          {this.renderVerticalGrid(xScale, yScale)}
          {this.renderBackdrop(xScale, yScale)}
          {this.renderLine(xScale, yScale)}
          {this.renderPoints(xScale, yScale)}
          {this.renderXTicks(xScale, yScale)}
          {this.renderXValues(xScale, yScale)}
        </g>
      </svg>
    );
  };

  render() {
    return this.props.width !== undefined ? (
      this.renderChart(this.props.width)
    ) : (
      <AutoSizer disableHeight={true}>{size => this.renderChart(size.width)}</AutoSizer>
    );
  }
}
