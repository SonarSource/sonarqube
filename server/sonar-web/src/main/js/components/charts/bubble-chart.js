/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import d3 from 'd3';
import React from 'react';
import sortBy from 'lodash/sortBy';
import uniq from 'lodash/uniq';
import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';

const TICKS_COUNT = 5;

export const Bubble = React.createClass({
  propTypes: {
    x: React.PropTypes.number.isRequired,
    y: React.PropTypes.number.isRequired,
    r: React.PropTypes.number.isRequired,
    tooltip: React.PropTypes.string,
    link: React.PropTypes.any
  },

  handleClick () {
    if (this.props.onClick) {
      this.props.onClick(this.props.link);
    }
  },

  render () {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'title': this.props.tooltip
      };
    }
    return (
        <circle onClick={this.handleClick} className="bubble-chart-bubble"
                r={this.props.r} {...tooltipAttrs}
                transform={`translate(${this.props.x}, ${this.props.y})`}/>
    );
  }
});

export const BubbleChart = React.createClass({
  propTypes: {
    items: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    sizeRange: React.PropTypes.arrayOf(React.PropTypes.number),
    displayXGrid: React.PropTypes.bool,
    displayXTicks: React.PropTypes.bool,
    displayYGrid: React.PropTypes.bool,
    displayYTicks: React.PropTypes.bool,
    height: React.PropTypes.number,
    padding: React.PropTypes.arrayOf(React.PropTypes.number),
    formatXTick: React.PropTypes.func,
    formatYTick: React.PropTypes.func,
    onBubbleClick: React.PropTypes.func
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps () {
    return {
      sizeRange: [5, 45],
      displayXGrid: true,
      displayYGrid: true,
      displayXTicks: true,
      displayYTicks: true,
      padding: [10, 10, 10, 10],
      formatXTick: d => d,
      formatYTick: d => d
    };
  },

  getInitialState () {
    return { width: this.props.width, height: this.props.height };
  },

  getXRange (xScale, sizeScale, availableWidth) {
    const minX = d3.min(this.props.items, d => xScale(d.x) - sizeScale(d.size));
    const maxX = d3.max(this.props.items, d => xScale(d.x) + sizeScale(d.size));
    const dMinX = minX < 0 ? xScale.range()[0] - minX : xScale.range()[0];
    const dMaxX = maxX > xScale.range()[1] ? maxX - xScale.range()[1] : 0;
    return [dMinX, availableWidth - dMaxX];
  },

  getYRange (yScale, sizeScale, availableHeight) {
    const minY = d3.min(this.props.items, d => yScale(d.y) - sizeScale(d.size));
    const maxY = d3.max(this.props.items, d => yScale(d.y) + sizeScale(d.size));
    const dMinY = minY < 0 ? yScale.range()[1] - minY : yScale.range()[1];
    const dMaxY = maxY > yScale.range()[0] ? maxY - yScale.range()[0] : 0;
    return [availableHeight - dMaxY, dMinY];
  },

  getTicks (scale, format) {
    const ticks = scale.ticks(TICKS_COUNT).map(tick => format(tick));
    const uniqueTicksCount = uniq(ticks).length;
    const ticksCount = uniqueTicksCount < TICKS_COUNT ? uniqueTicksCount - 1 : TICKS_COUNT;
    return scale.ticks(ticksCount);
  },

  renderXGrid (ticks, xScale, yScale) {
    if (!this.props.displayXGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const x = xScale(tick);
      const y1 = yScale.range()[0];
      const y2 = yScale.range()[1];
      return (
          <line
              key={index}
              x1={x}
              x2={x}
              y1={y1}
              y2={y2}
              className="bubble-chart-grid"/>
      );
    });

    return <g ref="xGrid">{lines}</g>;
  },

  renderYGrid (ticks, xScale, yScale) {
    if (!this.props.displayYGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const y = yScale(tick);
      const x1 = xScale.range()[0];
      const x2 = xScale.range()[1];
      return (
          <line
              key={index}
              x1={x1}
              x2={x2}
              y1={y}
              y2={y}
              className="bubble-chart-grid"/>
      );
    });

    return <g ref="yGrid">{lines}</g>;
  },

  renderXTicks (xTicks, xScale, yScale) {
    if (!this.props.displayXTicks) {
      return null;
    }

    const ticks = xTicks.map((tick, index) => {
      const x = xScale(tick);
      const y = yScale.range()[0];
      const innerText = this.props.formatXTick(tick);
      return (
          <text
              key={index}
              className="bubble-chart-tick"
              x={x}
              y={y}
              dy="1.5em">
            {innerText}
          </text>
      );
    });

    return <g>{ticks}</g>;
  },

  renderYTicks (yTicks, xScale, yScale) {
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
  },

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    const availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    const xScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.x)])
        .range([0, availableWidth])
        .nice();
    const yScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.y)])
        .range([availableHeight, 0])
        .nice();
    const sizeScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.size)])
        .range(this.props.sizeRange);

    const xScaleOriginal = xScale.copy();
    const yScaleOriginal = yScale.copy();

    xScale.range(this.getXRange(xScale, sizeScale, availableWidth));
    yScale.range(this.getYRange(yScale, sizeScale, availableHeight));

    const bubbles = sortBy(this.props.items, b => -b.size)
        .map((item, index) => {
          return (
              <Bubble
                  key={index}
                  link={item.link}
                  tooltip={item.tooltip}
                  x={xScale(item.x)} y={yScale(item.y)} r={sizeScale(item.size)}
                  onClick={this.props.onBubbleClick}/>
          );
        });

    const xTicks = this.getTicks(xScale, this.props.formatXTick);
    const yTicks = this.getTicks(yScale, this.props.formatYTick);

    return (
        <svg className="bubble-chart" width={this.state.width} height={this.state.height}>
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
});
