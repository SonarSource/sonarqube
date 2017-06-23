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
// @flow
import React from 'react';
import classNames from 'classnames';
import { flatten, sortBy } from 'lodash';
import { extent, max } from 'd3-array';
import { scaleLinear, scalePoint, scaleTime } from 'd3-scale';
import { line as d3Line, area, curveBasis } from 'd3-shape';

type Event = { className?: string, name: string, date: Date };
export type Point = { x: Date, y: number | string };
export type Serie = { name: string, data: Array<Point>, style: string };
type Scale = Function;

type Props = {
  basisCurve?: boolean,
  endDate: ?Date,
  events?: Array<Event>,
  eventSize?: number,
  disableZoom?: boolean,
  formatYTick?: number => string,
  hideGrid?: boolean,
  hideXAxis?: boolean,
  height: number,
  width: number,
  leakPeriodDate?: Date,
  padding: Array<number>,
  series: Array<Serie>,
  showAreas?: boolean,
  showEventMarkers?: boolean,
  startDate: ?Date,
  updateZoom?: (start: ?Date, endDate: ?Date) => void,
  zoomSpeed: number
};

export default class AdvancedTimeline extends React.PureComponent {
  props: Props;

  static defaultProps = {
    eventSize: 8,
    padding: [10, 10, 30, 60],
    zoomSpeed: 1
  };

  getRatingScale = (availableHeight: number) =>
    scalePoint().domain([5, 4, 3, 2, 1]).range([availableHeight, 0]);

  getLevelScale = (availableHeight: number) =>
    scalePoint().domain(['ERROR', 'WARN', 'OK']).range([availableHeight, 0]);

  getYScale = (availableHeight: number, flatData: Array<Point>) => {
    if (this.props.metricType === 'RATING') {
      return this.getRatingScale(availableHeight);
    } else if (this.props.metricType === 'LEVEL') {
      return this.getLevelScale(availableHeight);
    } else {
      return scaleLinear().range([availableHeight, 0]).domain([0, max(flatData, d => d.y)]).nice();
    }
  };

  getXScale = (availableWidth: number, flatData: Array<Point>) => {
    const dateRange = extent(flatData, d => d.x);
    const start = this.props.startDate ? this.props.startDate : dateRange[0];
    const end = this.props.endDate ? this.props.endDate : dateRange[1];
    const xScale = scaleTime().domain(sortBy([start, end])).range([0, availableWidth]).clamp(false);
    return {
      xScale,
      maxXRange: dateRange.map(xScale)
    };
  };

  getScales = () => {
    const availableWidth = this.props.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.props.height - this.props.padding[0] - this.props.padding[2];
    const flatData = flatten(this.props.series.map((serie: Serie) => serie.data));
    return {
      ...this.getXScale(availableWidth, flatData),
      yScale: this.getYScale(availableHeight, flatData)
    };
  };

  getEventMarker = (size: number) => {
    const half = size / 2;
    return `M${half} 0 L${size} ${half} L ${half} ${size} L0 ${half} L${half} 0 L${size} ${half}`;
  };

  handleWheel = (xScale: Scale, maxXRange: Array<number>) => (
    evt: WheelEvent & { target: HTMLElement }
  ) => {
    evt.preventDefault();
    const parentBbox = evt.target.getBoundingClientRect();
    const mouseXPos = (evt.clientX - parentBbox.left) / parentBbox.width;
    const xRange = xScale.range();
    const speed = evt.deltaMode ? 25 / evt.deltaMode * this.props.zoomSpeed : this.props.zoomSpeed;
    const leftPos = xRange[0] - Math.round(speed * evt.deltaY * mouseXPos);
    const rightPos = xRange[1] + Math.round(speed * evt.deltaY * (1 - mouseXPos));
    const startDate = leftPos > maxXRange[0] ? xScale.invert(leftPos) : null;
    const endDate = rightPos < maxXRange[1] ? xScale.invert(rightPos) : null;
    // $FlowFixMe updateZoom can't be undefined at this point
    this.props.updateZoom(startDate, endDate);
  };

  renderHorizontalGrid = (xScale: Scale, yScale: Scale) => {
    const { formatYTick } = this.props;
    const hasTicks = typeof yScale.ticks === 'function';
    const ticks = hasTicks ? yScale.ticks(4) : yScale.domain();

    if (!ticks.length) {
      ticks.push(yScale.domain()[1]);
    }

    return (
      <g>
        {ticks.map(tick => (
          <g key={tick}>
            {formatYTick != null &&
              <text
                className="line-chart-tick line-chart-tick-x"
                dx="-1em"
                dy="0.3em"
                textAnchor="end"
                x={xScale.range()[0]}
                y={yScale(tick)}>
                {formatYTick(tick)}
              </text>}
            <line
              className="line-chart-grid"
              x1={xScale.range()[0]}
              x2={xScale.range()[1]}
              y1={yScale(tick)}
              y2={yScale(tick)}
            />
          </g>
        ))}
      </g>
    );
  };

  renderXAxisTicks = (xScale: Scale, yScale: Scale) => {
    const format = xScale.tickFormat(7);
    const ticks = xScale.ticks(7);
    const y = yScale.range()[0];
    return (
      <g>
        {ticks.slice(0, -1).map((tick, index) => {
          const nextTick = index + 1 < ticks.length ? ticks[index + 1] : xScale.domain()[1];
          const x = (xScale(tick) + xScale(nextTick)) / 2;
          return (
            <text key={index} className="line-chart-tick" x={x} y={y} dy="1.5em">
              {format(tick)}
            </text>
          );
        })}
      </g>
    );
  };

  renderLeak = (xScale: Scale, yScale: Scale) => {
    const yRange = yScale.range();
    const xRange = xScale.range();
    const leakWidth = xRange[xRange.length - 1] - xScale(this.props.leakPeriodDate);
    if (leakWidth < 0) {
      return null;
    }
    return (
      <rect
        x={xScale(this.props.leakPeriodDate)}
        y={yRange[yRange.length - 1]}
        width={leakWidth}
        height={yRange[0] - yRange[yRange.length - 1]}
        fill="#fbf3d5"
      />
    );
  };

  renderLines = (xScale: Scale, yScale: Scale) => {
    const lineGenerator = d3Line()
      .defined(d => d.y || d.y === 0)
      .x(d => xScale(d.x))
      .y(d => yScale(d.y));
    if (this.props.basisCurve) {
      lineGenerator.curve(curveBasis);
    }
    return (
      <g>
        {this.props.series.map((serie, idx) => (
          <path
            key={`${idx}-${serie.name}`}
            className={classNames('line-chart-path', 'line-chart-path-' + serie.style)}
            d={lineGenerator(serie.data)}
          />
        ))}
      </g>
    );
  };

  renderAreas = (xScale: Scale, yScale: Scale) => {
    const areaGenerator = area()
      .defined(d => d.y || d.y === 0)
      .x(d => xScale(d.x))
      .y1(d => yScale(d.y))
      .y0(yScale(0));
    if (this.props.basisCurve) {
      areaGenerator.curve(curveBasis);
    }
    return (
      <g>
        {this.props.series.map((serie, idx) => (
          <path
            key={`${idx}-${serie.name}`}
            className={classNames('line-chart-area', 'line-chart-area-' + serie.style)}
            d={areaGenerator(serie.data)}
          />
        ))}
      </g>
    );
  };

  renderEvents = (xScale: Scale, yScale: Scale) => {
    const { events, eventSize } = this.props;
    if (!events || !eventSize) {
      return null;
    }
    const inRangeEvents = events.filter(
      event => event.date >= xScale.domain()[0] && event.date <= xScale.domain()[1]
    );
    const offset = eventSize / 2;
    return (
      <g>
        {inRangeEvents.map((event, idx) => (
          <path
            d={this.getEventMarker(eventSize)}
            className={classNames('line-chart-event', event.className)}
            key={`${idx}-${event.date.getTime()}`}
            transform={`translate(${xScale(event.date) - offset}, ${yScale.range()[0] + offset})`}
          />
        ))}
      </g>
    );
  };

  renderClipPath = (xScale: Scale, yScale: Scale) => {
    return (
      <defs>
        <clipPath id="chart-clip">
          <rect width={xScale.range()[1]} height={yScale.range()[0] + 10} />
        </clipPath>
      </defs>
    );
  };

  renderZoomOverlay = (xScale: Scale, yScale: Scale, maxXRange: Array<number>) => {
    return (
      <rect
        className="chart-wheel-zoom-overlay"
        width={xScale.range()[1]}
        height={yScale.range()[0]}
        onWheel={this.handleWheel(xScale, maxXRange)}
      />
    );
  };

  render() {
    if (!this.props.width || !this.props.height) {
      return <div />;
    }

    const { maxXRange, xScale, yScale } = this.getScales();
    const zoomEnabled = !this.props.disableZoom && this.props.updateZoom != null;
    const isZoomed = this.props.startDate || this.props.endDate;
    return (
      <svg
        className={classNames('line-chart', { 'chart-zoomed': isZoomed })}
        width={this.props.width}
        height={this.props.height}>
        {zoomEnabled && this.renderClipPath(xScale, yScale)}
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.props.leakPeriodDate != null && this.renderLeak(xScale, yScale)}
          {!this.props.hideGrid && this.renderHorizontalGrid(xScale, yScale)}
          {!this.props.hideXAxis && this.renderXAxisTicks(xScale, yScale)}
          {this.props.showAreas && this.renderAreas(xScale, yScale)}
          {this.renderLines(xScale, yScale)}
          {zoomEnabled && this.renderZoomOverlay(xScale, yScale, maxXRange)}
          {this.props.showEventMarkers && this.renderEvents(xScale, yScale)}
        </g>
      </svg>
    );
  }
}
