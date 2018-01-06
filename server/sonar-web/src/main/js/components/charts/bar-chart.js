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
import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { max } from 'd3-array';
import { scaleLinear, scaleBand } from 'd3-scale';
import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsContainer } from './../mixins/tooltips-mixin';

export const BarChart = createReactClass({
  displayName: 'BarChart',

  propTypes: {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    xTicks: PropTypes.arrayOf(PropTypes.any),
    xValues: PropTypes.arrayOf(PropTypes.any),
    height: PropTypes.number,
    padding: PropTypes.arrayOf(PropTypes.number),
    barsWidth: PropTypes.number.isRequired,
    onBarClick: PropTypes.func
  },

  mixins: [ResizeMixin],

  getDefaultProps() {
    return {
      xTicks: [],
      xValues: [],
      padding: [10, 10, 10, 10]
    };
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  componentDidUpdate(prevProps) {
    if (this.props.width && prevProps.width !== this.props.width) {
      this.setState({ width: this.props.width });
    }
    if (this.props.height && prevProps.height !== this.props.height) {
      this.setState({ height: this.props.height });
    }
  },

  handleClick(point) {
    this.props.onBarClick(point);
  },

  renderXTicks(xScale, yScale) {
    if (!this.props.xTicks.length) {
      return null;
    }
    const ticks = this.props.xTicks.map((tick, index) => {
      const point = this.props.data[index];
      const x = Math.round(xScale(point.x) + xScale.bandwidth() / 2);
      const y = yScale.range()[0];
      const d = this.props.data[index];
      const tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return (
        <text
          {...tooltipAtts}
          key={index}
          className="bar-chart-tick"
          x={x}
          y={y}
          dy="1.5em"
          onClick={this.props.onBarClick && this.handleClick.bind(this, point)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}>
          {tick}
        </text>
      );
    });
    return <g>{ticks}</g>;
  },

  renderXValues(xScale, yScale) {
    if (!this.props.xValues.length) {
      return null;
    }
    const ticks = this.props.xValues.map((value, index) => {
      const point = this.props.data[index];
      const x = Math.round(xScale(point.x) + xScale.bandwidth() / 2);
      const y = yScale(point.y);
      const d = this.props.data[index];
      const tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return (
        <text
          key={index}
          className="bar-chart-tick"
          x={x}
          y={y}
          dy="-1em"
          onClick={this.props.onBarClick && this.handleClick.bind(this, point)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}
          {...tooltipAtts}>
          {value}
        </text>
      );
    });
    return <g>{ticks}</g>;
  },

  renderBars(xScale, yScale) {
    const bars = this.props.data.map((d, index) => {
      const x = Math.round(xScale(d.x));
      const maxY = yScale.range()[0];
      const y = Math.round(yScale(d.y)) - /* minimum bar height */ 1;
      const height = maxY - y;
      const tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return (
        <rect
          key={index}
          className="bar-chart-bar"
          {...tooltipAtts}
          x={x}
          y={y}
          width={this.props.barsWidth}
          height={height}
          onClick={this.props.onBarClick && this.handleClick.bind(this, d)}
          style={{ cursor: this.props.onBarClick ? 'pointer' : 'default' }}
        />
      );
    });
    return <g>{bars}</g>;
  },

  render() {
    if (!this.state.width || !this.state.height) {
      return <div />;
    }

    const availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    const innerPadding =
      (availableWidth - this.props.barsWidth * this.props.data.length) /
      (this.props.data.length - 1);
    const relativeInnerPadding = innerPadding / (innerPadding + this.props.barsWidth);

    const maxY = max(this.props.data, d => d.y);
    const xScale = scaleBand()
      .domain(this.props.data.map(d => d.x))
      .range([0, availableWidth])
      .paddingInner(relativeInnerPadding);
    const yScale = scaleLinear()
      .domain([0, maxY])
      .range([availableHeight, 0]);

    return (
      <TooltipsContainer>
        <svg className="bar-chart" width={this.state.width} height={this.state.height}>
          <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
            {this.renderXTicks(xScale, yScale)}
            {this.renderXValues(xScale, yScale)}
            {this.renderBars(xScale, yScale)}
          </g>
        </svg>
      </TooltipsContainer>
    );
  }
});
