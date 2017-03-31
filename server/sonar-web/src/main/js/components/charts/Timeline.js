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
import $ from 'jquery';
import moment from 'moment';
import React from 'react';
import { extent, max } from 'd3-array';
import { scaleLinear, scaleOrdinal, scaleTime } from 'd3-scale';
import { line as d3Line } from 'd3-shape';
import { ResizeMixin } from '../mixins/resize-mixin';
import { TooltipsMixin } from '../mixins/tooltips-mixin';

const Timeline = React.createClass({
  propTypes: {
    data: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    padding: React.PropTypes.arrayOf(React.PropTypes.number),
    height: React.PropTypes.number,
    interpolate: React.PropTypes.string
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return {
      padding: [10, 10, 10, 10],
      interpolate: 'basis'
    };
  },

  getInitialState() {
    return {
      width: this.props.width,
      height: this.props.height
    };
  },

  getRatingScale(availableHeight) {
    return scaleOrdinal().domain([5, 4, 3, 2, 1]).rangePoints([availableHeight, 0]);
  },

  getLevelScale(availableHeight) {
    return scaleOrdinal().domain(['ERROR', 'WARN', 'OK']).rangePoints([availableHeight, 0]);
  },

  getYScale(availableHeight) {
    if (this.props.metricType === 'RATING') {
      return this.getRatingScale(availableHeight);
    } else if (this.props.metricType === 'LEVEL') {
      return this.getLevelScale(availableHeight);
    } else {
      return scaleLinear()
        .range([availableHeight, 0])
        .domain([0, max(this.props.data, d => d.y || 0)])
        .nice();
    }
  },

  handleEventMouseEnter(event) {
    $(`.js-event-circle-${event.date.getTime()}`).tooltip('show');
  },

  handleEventMouseLeave(event) {
    $(`.js-event-circle-${event.date.getTime()}`).tooltip('hide');
  },

  renderHorizontalGrid(xScale, yScale) {
    const hasTicks = typeof yScale.ticks === 'function';
    const ticks = hasTicks ? yScale.ticks(4) : yScale.domain();

    if (!ticks.length) {
      ticks.push(yScale.domain()[1]);
    }

    const grid = ticks.map(tick => {
      const opts = {
        x: xScale.range()[0],
        y: yScale(tick)
      };

      return (
        <g key={tick}>
          <text
            className="line-chart-tick line-chart-tick-x"
            dx="-1em"
            dy="0.3em"
            textAnchor="end"
            {...opts}>
            {this.props.formatYTick(tick)}
          </text>
          <line
            className="line-chart-grid"
            x1={xScale.range()[0]}
            x2={xScale.range()[1]}
            y1={yScale(tick)}
            y2={yScale(tick)}
          />
        </g>
      );
    });

    return <g>{grid}</g>;
  },

  renderTicks(xScale, yScale) {
    const format = xScale.tickFormat(7);
    let ticks = xScale.ticks(7);

    ticks = ticks.slice(0, -1).map((tick, index) => {
      const nextTick = index + 1 < ticks.length ? ticks[index + 1] : xScale.domain()[1];
      const x = (xScale(tick) + xScale(nextTick)) / 2;
      const y = yScale.range()[0];

      return (
        <text key={index} className="line-chart-tick" x={x} y={y} dy="1.5em">
          {format(tick)}
        </text>
      );
    });

    return <g>{ticks}</g>;
  },

  renderLeak(xScale, yScale) {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    const yScaleRange = yScale.range();
    const opts = {
      x: xScale(this.props.leakPeriodDate),
      y: yScaleRange[yScaleRange.length - 1],
      width: xScale.range()[1] - xScale(this.props.leakPeriodDate),
      height: yScaleRange[0] - yScaleRange[yScaleRange.length - 1],
      fill: '#fbf3d5'
    };

    return <rect {...opts} />;
  },

  renderLine(xScale, yScale) {
    const p = d3Line().x(d => xScale(d.x)).y(d => yScale(d.y)).interpolate(this.props.interpolate);
    return <path className="line-chart-path" d={p(this.props.data)} />;
  },

  renderEvents(xScale, yScale) {
    const points = this.props.events
      .map(event => {
        const snapshot = this.props.data.find(d => d.x.getTime() === event.date.getTime());
        return { ...event, snapshot };
      })
      .filter(event => event.snapshot)
      .map(event => {
        const key = `${event.date.getTime()}-${event.snapshot.y}`;
        const className = `line-chart-point js-event-circle-${event.date.getTime()}`;
        const value = event.snapshot.y ? this.props.formatValue(event.snapshot.y) : '—';
        const tooltip = [
          `<span class="nowrap">${event.version}</span>`,
          `<span class="nowrap">${moment(event.date).format('LL')}</span>`,
          `<span class="nowrap">${value}</span>`
        ].join('<br>');
        return (
          <circle
            key={key}
            className={className}
            r="4"
            cx={xScale(event.snapshot.x)}
            cy={yScale(event.snapshot.y)}
            onMouseEnter={this.handleEventMouseEnter.bind(this, event)}
            onMouseLeave={this.handleEventMouseLeave.bind(this, event)}
            data-toggle="tooltip"
            data-title={tooltip}
          />
        );
      });
    return <g>{points}</g>;
  },
  render() {
    if (!this.state.width || !this.state.height) {
      return <div />;
    }
    const availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];
    const xScale = scaleTime()
      .domain(extent(this.props.data, d => d.x || 0))
      .range([0, availableWidth])
      .clamp(true);
    const yScale = this.getYScale(availableHeight);
    return (
      <svg className="line-chart" width={this.state.width} height={this.state.height}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.renderLeak(xScale, yScale)}
          {this.renderHorizontalGrid(xScale, yScale)}
          {this.renderTicks(xScale, yScale)}
          {this.renderLine(xScale, yScale)}
          {this.renderEvents(xScale, yScale)}
        </g>
      </svg>
    );
  }
});
export default Timeline;
