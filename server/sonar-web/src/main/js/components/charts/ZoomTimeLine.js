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
import { extent, max, min } from 'd3-array';
import { scaleLinear, scalePoint, scaleTime } from 'd3-scale';
import { line as d3Line, area, curveBasis } from 'd3-shape';
import Draggable, { DraggableCore } from 'react-draggable';
import type { DraggableData } from 'react-draggable';
import type { Point, Serie } from './AdvancedTimeline';

type Scale = Function;

type Props = {
  basisCurve?: boolean,
  endDate: ?Date,
  height: number,
  width: number,
  leakPeriodDate: Date,
  padding: Array<number>,
  series: Array<Serie>,
  showAreas?: boolean,
  showXTicks?: boolean,
  startDate: ?Date,
  updateZoom: (start: ?Date, endDate: ?Date) => void,
  updateZoomFast: (start: ?Date, endDate: ?Date) => void
};

type State = {
  newZoomStart: ?number
};

export default class ZoomTimeLine extends React.PureComponent {
  props: Props;
  static defaultProps = {
    padding: [0, 0, 18, 0],
    showXTicks: true
  };

  state: State = {
    newZoomStart: null
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

  getXScale = (availableWidth: number, flatData: Array<Point>) =>
    scaleTime().domain(extent(flatData, d => d.x)).range([0, availableWidth]).clamp(true);

  getScales = () => {
    const availableWidth = this.props.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.props.height - this.props.padding[0] - this.props.padding[2];
    const flatData = flatten(this.props.series.map((serie: Serie) => serie.data));
    return {
      xScale: this.getXScale(availableWidth, flatData),
      yScale: this.getYScale(availableHeight, flatData)
    };
  };

  getEventMarker = (size: number) => {
    const half = size / 2;
    return `M${half} 0 L${size} ${half} L ${half} ${size} L0 ${half} L${half} 0 L${size} ${half}`;
  };

  handleSelectionDrag = (
    xScale: Scale,
    updateFunc: (xScale: Scale, xArray: Array<number>) => void,
    checkDelta?: boolean
  ) => (e: Event, data: DraggableData) => {
    if (!checkDelta || data.deltaX) {
      updateFunc(xScale, [data.x, data.node.getBoundingClientRect().width + data.x]);
    }
  };

  handleSelectionHandleDrag = (
    xScale: Scale,
    fixedX: number,
    updateFunc: (xScale: Scale, xArray: Array<number>) => void,
    handleDirection: string,
    checkDelta?: boolean
  ) => (e: Event, data: DraggableData) => {
    if (!checkDelta || data.deltaX) {
      updateFunc(xScale, handleDirection === 'right' ? [fixedX, data.x] : [data.x, fixedX]);
    }
  };

  handleNewZoomDragStart = (e: Event, data: DraggableData) =>
    this.setState({ newZoomStart: data.x - data.node.getBoundingClientRect().left });

  handleNewZoomDrag = (xScale: Scale) => (e: Event, data: DraggableData) => {
    const { newZoomStart } = this.state;
    if (newZoomStart != null && data.deltaX) {
      this.handleFastZoomUpdate(xScale, [
        newZoomStart,
        data.x - data.node.getBoundingClientRect().left
      ]);
    }
  };

  handleNewZoomDragEnd = (xScale: Scale, xDim: Array<number>) => (
    e: Event,
    data: DraggableData
  ) => {
    const { newZoomStart } = this.state;
    if (newZoomStart != null) {
      const x = data.x - data.node.getBoundingClientRect().left;
      this.handleZoomUpdate(xScale, newZoomStart === x ? xDim : [newZoomStart, x]);
      this.setState({ newZoomStart: null });
    }
  };

  handleZoomUpdate = (xScale: Scale, xArray: Array<number>) => {
    const xRange = xScale.range();
    const xStart = min(xArray);
    const xEnd = max(xArray);
    const startDate = xStart > xRange[0] ? xScale.invert(xStart) : null;
    const endDate = xEnd < xRange[xRange.length - 1] ? xScale.invert(xEnd) : null;
    if (this.props.startDate !== startDate || this.props.endDate !== endDate) {
      this.props.updateZoom(startDate, endDate);
    }
  };

  handleFastZoomUpdate = (xScale: Scale, xArray: Array<number>) => {
    const xRange = xScale.range();
    const startDate = xArray[0] > xRange[0] ? xScale.invert(xArray[0]) : null;
    const endDate = xArray[1] < xRange[xRange.length - 1] ? xScale.invert(xArray[1]) : null;
    if (this.props.startDate !== startDate || this.props.endDate !== endDate) {
      this.props.updateZoomFast(startDate, endDate);
    }
  };

  renderBaseLine = (xScale: Scale, yScale: Scale) => {
    return (
      <line
        className="line-chart-grid"
        x1={xScale.range()[0]}
        x2={xScale.range()[1]}
        y1={yScale.range()[0]}
        y2={yScale.range()[0]}
      />
    );
  };

  renderTicks = (xScale: Scale, yScale: Scale) => {
    const format = xScale.tickFormat(7);
    const ticks = xScale.ticks(7);
    const y = yScale.range()[0];
    return (
      <g>
        {ticks.slice(0, -1).map((tick, index) => {
          const nextTick = index + 1 < ticks.length ? ticks[index + 1] : xScale.domain()[1];
          const x = (xScale(tick) + xScale(nextTick)) / 2;
          return (
            <text key={index} className="chart-zoom-tick" x={x} y={y} dy="1.3em">
              {format(tick)}
            </text>
          );
        })}
      </g>
    );
  };

  renderLeak = (xScale: Scale, yScale: Scale) => {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    const yRange = yScale.range();
    return (
      <rect
        x={xScale(this.props.leakPeriodDate)}
        y={yRange[yRange.length - 1]}
        width={xScale.range()[1] - xScale(this.props.leakPeriodDate)}
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

  renderZoomHandle = (
    opts: {
      xScale: Scale,
      xPos: number,
      fixedPos: number,
      yDim: Array<number>,
      xDim: Array<number>,
      direction: string
    }
  ) => (
    <Draggable
      axis="x"
      bounds={{ left: opts.xDim[0], right: opts.xDim[1] }}
      position={{ x: opts.xPos, y: 0 }}
      onDrag={this.handleSelectionHandleDrag(
        opts.xScale,
        opts.fixedPos,
        this.handleFastZoomUpdate,
        opts.direction,
        true
      )}
      onStop={this.handleSelectionHandleDrag(
        opts.xScale,
        opts.fixedPos,
        this.handleZoomUpdate,
        opts.direction
      )}>
      <rect
        className="zoom-selection-handle"
        x={-3}
        y={opts.yDim[1]}
        height={opts.yDim[0] - opts.yDim[1]}
        width={6}
      />
    </Draggable>
  );

  renderZoom = (xScale: Scale, yScale: Scale) => {
    const xRange = xScale.range();
    const yRange = yScale.range();
    const xDim = [xRange[0], xRange[xRange.length - 1]];
    const yDim = [yRange[0], yRange[yRange.length - 1]];
    const startX = Math.round(this.props.startDate ? xScale(this.props.startDate) : xDim[0]);
    const endX = Math.round(this.props.endDate ? xScale(this.props.endDate) : xDim[1]);
    const xArray = sortBy([startX, endX]);
    const showZoomArea = this.state.newZoomStart == null || this.state.newZoomStart === startX;
    return (
      <g className="chart-zoom">
        <DraggableCore
          onStart={this.handleNewZoomDragStart}
          onDrag={this.handleNewZoomDrag(xScale)}
          onStop={this.handleNewZoomDragEnd(xScale, xDim)}>
          <rect
            className="zoom-overlay"
            x={xDim[0]}
            y={yDim[1]}
            height={yDim[0] - yDim[1]}
            width={xDim[1] - xDim[0]}
          />
        </DraggableCore>
        {showZoomArea &&
          <Draggable
            axis="x"
            bounds={{ left: xDim[0], right: xDim[1] - xArray[1] + xArray[0] }}
            position={{ x: xArray[0], y: 0 }}
            onDrag={this.handleSelectionDrag(xScale, this.handleFastZoomUpdate, true)}
            onStop={this.handleSelectionDrag(xScale, this.handleZoomUpdate)}>
            <rect
              className="zoom-selection"
              x={0}
              y={yDim[1]}
              height={yDim[0] - yDim[1]}
              width={xArray[1] - xArray[0]}
            />
          </Draggable>}
        {showZoomArea &&
          this.renderZoomHandle({
            xScale,
            xPos: startX,
            fixedPos: endX,
            xDim,
            yDim,
            direction: 'left'
          })}
        {showZoomArea &&
          this.renderZoomHandle({
            xScale,
            xPos: endX,
            fixedPos: startX,
            xDim,
            yDim,
            direction: 'right'
          })}
      </g>
    );
  };

  render() {
    if (!this.props.width || !this.props.height) {
      return <div />;
    }

    const { xScale, yScale } = this.getScales();
    return (
      <svg className="line-chart " width={this.props.width} height={this.props.height}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.renderLeak(xScale, yScale)}
          {this.renderBaseLine(xScale, yScale)}
          {this.props.showXTicks && this.renderTicks(xScale, yScale)}
          {this.props.showAreas && this.renderAreas(xScale, yScale)}
          {this.renderLines(xScale, yScale)}
          {this.renderZoom(xScale, yScale)}
        </g>
      </svg>
    );
  }
}
