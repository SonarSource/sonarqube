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
/* eslint-disable @typescript-eslint/prefer-optional-chain */
import styled from '@emotion/styled';
import { max } from 'd3-array';
import { scaleBand, ScaleBand, scaleLinear, ScaleLinear } from 'd3-scale';
import React from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers';
import { Tooltip, TooltipWrapper } from './Tooltip';

interface Props {
  bars: number[];
  height: number;
  leftAlignTicks?: boolean;
  padding?: [number, number, number, number];
  width: number;
  yTicks?: string[];
  yTooltips?: string[];
  yValues?: string[];
}

const BAR_HEIGHT = 10;
const DEFAULT_PADDING = [10, 10, 10, 10];

type XScale = ScaleLinear<number, number>;
type YScale = ScaleBand<number>;

export class Histogram extends React.PureComponent<Props> {
  renderBar(d: number, index: number, xScale: XScale, yScale: YScale) {
    const { leftAlignTicks, padding = DEFAULT_PADDING } = this.props;

    const width = Math.round(xScale(d)) + /* minimum bar width */ 1;
    const x = xScale.range()[0] + (leftAlignTicks ? padding[3] : 0);
    const y = Math.round((yScale(index) ?? 0) + yScale.bandwidth() / 2);

    return <HistogramBar height={BAR_HEIGHT} width={width} x={x} y={y} />;
  }

  renderValue(d: number, index: number, xScale: XScale, yScale: YScale) {
    const { leftAlignTicks, padding = DEFAULT_PADDING, yValues } = this.props;

    const value = yValues && yValues[index];

    if (!value) {
      return null;
    }

    const x = xScale(d) + (leftAlignTicks ? padding[3] : 0);
    const y = Math.round((yScale(index) ?? 0) + yScale.bandwidth() / 2 + BAR_HEIGHT / 2);

    return (
      <Tooltip content={this.props.yTooltips && this.props.yTooltips[index]}>
        <HistogramTick dx="1em" dy="0.3em" textAnchor="start" x={x} y={y}>
          {value}
        </HistogramTick>
      </Tooltip>
    );
  }

  renderTick(index: number, xScale: XScale, yScale: YScale) {
    const { leftAlignTicks, yTicks } = this.props;

    const tick = yTicks && yTicks[index];

    if (!tick) {
      return null;
    }

    const x = xScale.range()[0];
    const y = Math.round((yScale(index) ?? 0) + yScale.bandwidth() / 2 + BAR_HEIGHT / 2);

    return (
      <HistogramTick
        dx={leftAlignTicks ? 0 : '-1em'}
        dy="0.3em"
        textAnchor={leftAlignTicks ? 'start' : 'end'}
        x={x}
        y={y}
      >
        {tick}
      </HistogramTick>
    );
  }

  renderBars(xScale: XScale, yScale: YScale) {
    return (
      <g>
        {this.props.bars.map((d, index) => (
          <g key={index}>
            {this.renderBar(d, index, xScale, yScale)}
            {this.renderValue(d, index, xScale, yScale)}
            {this.renderTick(index, xScale, yScale)}
          </g>
        ))}
      </g>
    );
  }

  render() {
    const { bars, height, leftAlignTicks, padding = DEFAULT_PADDING, width } = this.props;

    const availableWidth = width - padding[1] - padding[3];
    const xScale: XScale = scaleLinear()
      .domain([0, max(bars) ?? 0])
      .range([0, availableWidth]);

    const availableHeight = height - padding[0] - padding[2];
    const yScale: YScale = scaleBand<number>()
      .domain(bars.map((_, index) => index))
      .rangeRound([0, availableHeight]);

    return (
      <svg height={this.props.height} width={this.props.width}>
        <g transform={`translate(${leftAlignTicks ? 0 : padding[3]}, ${padding[0]})`}>
          {this.renderBars(xScale, yScale)}
        </g>
      </svg>
    );
  }
}

const HistogramTick = styled.text`
  ${tw`sw-body-sm`}
  fill: ${themeColor('pageContentLight')};

  ${TooltipWrapper} & {
    fill: ${themeContrast('primary')};
  }
`;

const HistogramBar = styled.rect`
  fill: ${themeColor('primary')};
`;
