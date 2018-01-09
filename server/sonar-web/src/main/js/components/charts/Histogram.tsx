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
import { max } from 'd3-array';
import { scaleLinear, scaleBand, ScaleLinear, ScaleBand } from 'd3-scale';
import Tooltip from '../controls/Tooltip';

interface Props {
  alignTicks?: boolean;
  bars: number[];
  height: number;
  padding?: [number, number, number, number];
  yTicks?: string[];
  yTooltips?: string[];
  yValues?: string[];
  width: number;
}

const BAR_HEIGHT = 10;
const DEFAULT_PADDING = [10, 10, 10, 10];

type XScale = ScaleLinear<number, number>;
type YScale = ScaleBand<number>;

export default class Histogram extends React.PureComponent<Props> {
  wrapWithTooltip(element: React.ReactNode, index: number) {
    const tooltip = this.props.yTooltips && this.props.yTooltips[index];
    return tooltip ? (
      <Tooltip key={index} overlay={tooltip} placement="top">
        {element}
      </Tooltip>
    ) : (
      element
    );
  }

  renderBar(d: number, index: number, xScale: XScale, yScale: YScale) {
    const { alignTicks, padding = DEFAULT_PADDING } = this.props;

    const width = Math.round(xScale(d)) + /* minimum bar width */ 1;
    const x = xScale.range()[0] + (alignTicks ? padding[3] : 0);
    const y = Math.round(yScale(index)! + yScale.bandwidth() / 2);

    return <rect className="bar-chart-bar" x={x} y={y} width={width} height={BAR_HEIGHT} />;
  }

  renderValue(d: number, index: number, xScale: XScale, yScale: YScale) {
    const { alignTicks, padding = DEFAULT_PADDING, yValues } = this.props;

    const value = yValues && yValues[index];

    if (!value) {
      return null;
    }

    const x = xScale(d) + (alignTicks ? padding[3] : 0);
    const y = Math.round(yScale(index)! + yScale.bandwidth() / 2 + BAR_HEIGHT / 2);

    return this.wrapWithTooltip(
      <text className="bar-chart-tick histogram-value" x={x} y={y} dx="1em" dy="0.3em">
        {value}
      </text>,
      index
    );
  }

  renderTick(index: number, xScale: XScale, yScale: YScale) {
    const { alignTicks, yTicks } = this.props;

    const tick = yTicks && yTicks[index];

    if (!tick) {
      return null;
    }

    const x = xScale.range()[0];
    const y = Math.round(yScale(index)! + yScale.bandwidth() / 2 + BAR_HEIGHT / 2);
    const historyTickClass = alignTicks ? 'histogram-tick-start' : 'histogram-tick';

    return (
      <text
        className={'bar-chart-tick ' + historyTickClass}
        x={x}
        y={y}
        dx={alignTicks ? 0 : '-1em'}
        dy="0.3em">
        {tick}
      </text>
    );
  }

  renderBars(xScale: XScale, yScale: YScale) {
    return (
      <g>
        {this.props.bars.map((d, index) => {
          return (
            <g key={index}>
              {this.renderBar(d, index, xScale, yScale)}
              {this.renderValue(d, index, xScale, yScale)}
              {this.renderTick(index, xScale, yScale)}
            </g>
          );
        })}
      </g>
    );
  }

  render() {
    const { bars, width, height, padding = DEFAULT_PADDING } = this.props;

    const availableWidth = width - padding[1] - padding[3];
    const xScale: XScale = scaleLinear()
      .domain([0, max(bars)!])
      .range([0, availableWidth]);

    const availableHeight = height - padding[0] - padding[2];
    const yScale: YScale = scaleBand<number>()
      .domain(bars.map((_, index) => index))
      .rangeRound([0, availableHeight]);

    return (
      <svg className="bar-chart" width={this.props.width} height={this.props.height}>
        <g transform={`translate(${this.props.alignTicks ? 4 : padding[3]}, ${padding[0]})`}>
          {this.renderBars(xScale, yScale)}
        </g>
      </svg>
    );
  }
}
