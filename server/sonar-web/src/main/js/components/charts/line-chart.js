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
import { extent, max } from 'd3-array';
import { scaleLinear } from 'd3-scale';
import { area as d3Area, line as d3Line, curveBasis } from 'd3-shape';
import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';

export const LineChart = createReactClass({
  displayName: 'LineChart',

  propTypes: {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    xTicks: PropTypes.arrayOf(PropTypes.any),
    xValues: PropTypes.arrayOf(PropTypes.any),
    padding: PropTypes.arrayOf(PropTypes.number),
    backdropConstraints: PropTypes.arrayOf(PropTypes.number),
    displayBackdrop: PropTypes.bool,
    displayPoints: PropTypes.bool,
    displayVerticalGrid: PropTypes.bool,
    height: PropTypes.number
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return {
      displayBackdrop: true,
      displayPoints: true,
      displayVerticalGrid: true,
      xTicks: [],
      xValues: [],
      padding: [10, 10, 10, 10]
    };
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  renderBackdrop(xScale, yScale) {
    if (!this.props.displayBackdrop) {
      return null;
    }

    const area = d3Area()
      .x(d => xScale(d.x))
      .y0(yScale.range()[0])
      .y1(d => yScale(d.y))
      .defined(d => d.y != null)
      .curve(curveBasis);

    let data = this.props.data;
    if (this.props.backdropConstraints) {
      const c = this.props.backdropConstraints;
      data = data.filter(d => c[0] <= d.x && d.x <= c[1]);
    }
    return <path className="line-chart-backdrop" d={area(data)} />;
  },

  renderPoints(xScale, yScale) {
    if (!this.props.displayPoints) {
      return null;
    }
    const points = this.props.data.filter(point => point.y != null).map((point, index) => {
      const x = xScale(point.x);
      const y = yScale(point.y);
      return <circle key={index} className="line-chart-point" r="3" cx={x} cy={y} />;
    });
    return <g>{points}</g>;
  },

  renderVerticalGrid(xScale, yScale) {
    if (!this.props.displayVerticalGrid) {
      return null;
    }
    const lines = this.props.data.map((point, index) => {
      const x = xScale(point.x);
      const y1 = yScale.range()[0];
      const y2 = yScale(point.y);
      return <line key={index} className="line-chart-grid" x1={x} x2={x} y1={y1} y2={y2} />;
    });
    return <g>{lines}</g>;
  },

  renderXTicks(xScale, yScale) {
    if (!this.props.xTicks.length) {
      return null;
    }
    const ticks = this.props.xTicks.map((tick, index) => {
      const point = this.props.data[index];
      const x = xScale(point.x);
      const y = yScale.range()[0];
      return (
        <text key={index} className="line-chart-tick" x={x} y={y} dy="1.5em">
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
      const x = xScale(point.x);
      const y = yScale(point.y);
      return (
        <text key={index} className="line-chart-tick" x={x} y={y} dy="-1em">
          {value}
        </text>
      );
    });
    return <g>{ticks}</g>;
  },

  renderLine(xScale, yScale) {
    const p = d3Line()
      .x(d => xScale(d.x))
      .y(d => yScale(d.y))
      .defined(d => d.y != null)
      .curve(curveBasis);
    return <path className="line-chart-path" d={p(this.props.data)} />;
  },

  render() {
    if (!this.state.width || !this.state.height) {
      return <div />;
    }

    const availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    let maxY;
    const xScale = scaleLinear()
      .domain(extent(this.props.data, d => d.x))
      .range([0, availableWidth]);
    const yScale = scaleLinear().range([availableHeight, 0]);

    if (this.props.domain) {
      maxY = this.props.domain[1];
      yScale.domain(this.props.domain);
    } else {
      maxY = max(this.props.data, d => d.y);
      yScale.domain([0, maxY]);
    }

    return (
      <svg className="line-chart" width={this.state.width} height={this.state.height}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.renderVerticalGrid(xScale, yScale, maxY)}
          {this.renderBackdrop(xScale, yScale)}
          {this.renderLine(xScale, yScale)}
          {this.renderPoints(xScale, yScale)}
          {this.renderXTicks(xScale, yScale)}
          {this.renderXValues(xScale, yScale)}
        </g>
      </svg>
    );
  }
});
