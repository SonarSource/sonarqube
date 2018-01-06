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
import { Link } from 'react-router';
import { min, max } from 'd3-array';
import { scaleLinear } from 'd3-scale';
import { sortBy, uniq } from 'lodash';
import AutoSizer from 'react-virtualized/dist/commonjs/AutoSizer';
import { TooltipsContainer } from '../mixins/tooltips-mixin';

/*::
type Scale = {
  (number): number,
  range: () => [number, number],
  ticks: number => Array<number>
};
*/

const TICKS_COUNT = 5;

export class Bubble extends React.PureComponent {
  /*:: props: {
    color?: string,
    link?: string,
    onClick: (?string) => void,
    r: number,
    tooltip?: string,
    x: number,
    y: number
  };
*/

  handleClick = () => {
    if (this.props.onClick) {
      this.props.onClick(this.props.link);
    }
  };

  render() {
    const tooltipAttrs = this.props.tooltip
      ? {
          'data-toggle': 'tooltip',
          title: this.props.tooltip
        }
      : {};

    let circle = (
      <circle
        {...tooltipAttrs}
        onClick={this.props.onClick ? this.handleClick : undefined}
        className="bubble-chart-bubble"
        r={this.props.r}
        style={{
          fill: this.props.color,
          stroke: this.props.color
        }}
        transform={`translate(${this.props.x}, ${this.props.y})`}
      />
    );

    if (this.props.link && !this.props.onClick) {
      circle = <Link to={this.props.link}>{circle}</Link>;
    }

    return this.props.tooltip ? (
      <TooltipsContainer>
        <g>{circle}</g>
      </TooltipsContainer>
    ) : (
      circle
    );
  }
}

export default class BubbleChart extends React.PureComponent {
  /*:: props: {|
    items: Array<{|
      x: number,
      y: number,
      size: number,
      color?: string,
      key?: string,
      link?: string,
      tooltip?: string
    |}>,
    sizeRange?: [number, number],
    displayXGrid: boolean,
    displayXTicks: boolean,
    displayYGrid: boolean,
    displayYTicks: boolean,
    height: number,
    padding: [number, number, number, number],
    formatXTick: number => string,
    formatYTick: number => string,
    onBubbleClick?: (?string) => void,
    xDomain?: [number, number],
    yDomain?: [number, number]
  |};
*/

  static defaultProps = {
    sizeRange: [5, 45],
    displayXGrid: true,
    displayYGrid: true,
    displayXTicks: true,
    displayYTicks: true,
    padding: [10, 10, 10, 10],
    formatXTick: d => d,
    formatYTick: d => d
  };

  getXRange(xScale /*: Scale */, sizeScale /*: Scale */, availableWidth /*: number */) {
    const minX = min(this.props.items, d => xScale(d.x) - sizeScale(d.size));
    const maxX = max(this.props.items, d => xScale(d.x) + sizeScale(d.size));
    const dMinX = minX < 0 ? xScale.range()[0] - minX : xScale.range()[0];
    const dMaxX = maxX > xScale.range()[1] ? maxX - xScale.range()[1] : 0;
    return [dMinX, availableWidth - dMaxX];
  }

  getYRange(yScale /*: Scale */, sizeScale /*: Scale */, availableHeight /*: number */) {
    const minY = min(this.props.items, d => yScale(d.y) - sizeScale(d.size));
    const maxY = max(this.props.items, d => yScale(d.y) + sizeScale(d.size));
    const dMinY = minY < 0 ? yScale.range()[1] - minY : yScale.range()[1];
    const dMaxY = maxY > yScale.range()[0] ? maxY - yScale.range()[0] : 0;
    return [availableHeight - dMaxY, dMinY];
  }

  getTicks(scale /*: Scale */, format /*: number => string */) {
    const ticks = scale.ticks(TICKS_COUNT).map(tick => format(tick));
    const uniqueTicksCount = uniq(ticks).length;
    const ticksCount = uniqueTicksCount < TICKS_COUNT ? uniqueTicksCount - 1 : TICKS_COUNT;
    return scale.ticks(ticksCount);
  }

  renderXGrid(ticks /*: Array<number> */, xScale /*: Scale */, yScale /*: Scale */) {
    if (!this.props.displayXGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const x = xScale(tick);
      const y1 = yScale.range()[0];
      const y2 = yScale.range()[1];
      return <line key={index} x1={x} x2={x} y1={y1} y2={y2} className="bubble-chart-grid" />;
    });

    return <g ref="xGrid">{lines}</g>;
  }

  renderYGrid(ticks /*: Array<number> */, xScale /*: Scale */, yScale /*: Scale */) {
    if (!this.props.displayYGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const y = yScale(tick);
      const x1 = xScale.range()[0];
      const x2 = xScale.range()[1];
      return <line key={index} x1={x1} x2={x2} y1={y} y2={y} className="bubble-chart-grid" />;
    });

    return <g ref="yGrid">{lines}</g>;
  }

  renderXTicks(xTicks /*: Array<number> */, xScale /*: Scale */, yScale /*: Scale */) {
    if (!this.props.displayXTicks) {
      return null;
    }

    const ticks = xTicks.map((tick, index) => {
      const x = xScale(tick);
      const y = yScale.range()[0];
      const innerText = this.props.formatXTick(tick);
      return (
        <text key={index} className="bubble-chart-tick" x={x} y={y} dy="1.5em">
          {innerText}
        </text>
      );
    });

    return <g>{ticks}</g>;
  }

  renderYTicks(yTicks /*: Array<number> */, xScale /*: Scale */, yScale /*: Scale */) {
    if (!this.props.displayYTicks) {
      return null;
    }

    const ticks = yTicks.map((tick, index) => {
      const x = xScale.range()[0];
      const y = yScale(tick);
      const innerText = this.props.formatYTick(tick);
      return (
        <text
          key={index}
          className="bubble-chart-tick bubble-chart-tick-y"
          x={x}
          y={y}
          dx="-0.5em"
          dy="0.3em">
          {innerText}
        </text>
      );
    });

    return <g>{ticks}</g>;
  }

  renderChart(width /*: number */) {
    const availableWidth = width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.props.height - this.props.padding[0] - this.props.padding[2];

    const xScale = scaleLinear()
      .domain(this.props.xDomain || [0, max(this.props.items, d => d.x)])
      .range([0, availableWidth])
      .nice();
    const yScale = scaleLinear()
      .domain(this.props.yDomain || [0, max(this.props.items, d => d.y)])
      .range([availableHeight, 0])
      .nice();
    const sizeScale = scaleLinear()
      .domain(this.props.sizeDomain || [0, max(this.props.items, d => d.size)])
      .range(this.props.sizeRange);

    const xScaleOriginal = xScale.copy();
    const yScaleOriginal = yScale.copy();

    xScale.range(this.getXRange(xScale, sizeScale, availableWidth));
    yScale.range(this.getYRange(yScale, sizeScale, availableHeight));

    const bubbles = sortBy(this.props.items, b => -b.size).map((item, index) => {
      return (
        <Bubble
          key={item.key || index}
          link={item.link}
          tooltip={item.tooltip}
          x={xScale(item.x)}
          y={yScale(item.y)}
          r={sizeScale(item.size)}
          color={item.color}
          onClick={this.props.onBubbleClick}
        />
      );
    });

    const xTicks = this.getTicks(xScale, this.props.formatXTick);
    const yTicks = this.getTicks(yScale, this.props.formatYTick);

    return (
      <svg className="bubble-chart" width={width} height={this.props.height}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.renderXGrid(xTicks, xScale, yScale)}
          {this.renderXTicks(xTicks, xScale, yScaleOriginal)}
          {this.renderYGrid(yTicks, xScale, yScale)}
          {this.renderYTicks(yTicks, xScaleOriginal, yScale)}
          {bubbles}
        </g>
      </svg>
    );
  }

  render() {
    return <AutoSizer disableHeight={true}>{size => this.renderChart(size.width)}</AutoSizer>;
  }
}
