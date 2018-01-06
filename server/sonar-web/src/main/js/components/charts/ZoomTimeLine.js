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
import classNames from 'classnames';
import { flatten, sortBy, throttle } from 'lodash';
import { extent, max } from 'd3-array';
import { scaleLinear, scalePoint, scaleTime } from 'd3-scale';
import { line as d3Line, area, curveBasis } from 'd3-shape';
import Draggable, { DraggableCore } from 'react-draggable';
/*:: import type { DraggableData } from 'react-draggable'; */
/*:: import type { Point, Serie } from './AdvancedTimeline'; */
import * as theme from '../../app/theme';

/*::
type Scale = Function;
*/

/*::
type Props = {
  basisCurve?: boolean,
  endDate: ?Date,
  height: number,
  width: number,
  leakPeriodDate?: Date,
  padding: Array<number>,
  series: Array<Serie>,
  showAreas?: boolean,
  showXTicks?: boolean,
  startDate: ?Date,
  updateZoom: (start: ?Date, endDate: ?Date) => void
};
*/

/*::
type State = {
  overlayLeftPos: ?number,
  newZoomStart: ?number
};
*/

export default class ZoomTimeLine extends React.PureComponent {
  /*:: props: Props; */
  /*:: state: State; */

  static defaultProps = {
    padding: [0, 0, 18, 0],
    showXTicks: true
  };

  constructor(props /*: Props */) {
    super(props);
    this.state = { overlayLeftPos: null, newZoomStart: null };
    this.handleZoomUpdate = throttle(this.handleZoomUpdate, 40);
  }

  getRatingScale = (availableHeight /*: number */) =>
    scalePoint()
      .domain([5, 4, 3, 2, 1])
      .range([availableHeight, 0]);

  getLevelScale = (availableHeight /*: number */) =>
    scalePoint()
      .domain(['ERROR', 'WARN', 'OK'])
      .range([availableHeight, 0]);

  getYScale = (availableHeight /*: number */, flatData /*: Array<Point> */) => {
    if (this.props.metricType === 'RATING') {
      return this.getRatingScale(availableHeight);
    } else if (this.props.metricType === 'LEVEL') {
      return this.getLevelScale(availableHeight);
    } else {
      return scaleLinear()
        .range([availableHeight, 0])
        .domain([0, max(flatData, d => d.y)])
        .nice();
    }
  };

  getXScale = (availableWidth /*: number */, flatData /*: Array<Point> */) =>
    scaleTime()
      .domain(extent(flatData, d => d.x))
      .range([0, availableWidth])
      .clamp(true);

  getScales = () => {
    const availableWidth = this.props.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.props.height - this.props.padding[0] - this.props.padding[2];
    const flatData = flatten(this.props.series.map((serie /*: Serie */) => serie.data));
    return {
      xScale: this.getXScale(availableWidth, flatData),
      yScale: this.getYScale(availableHeight, flatData)
    };
  };

  getEventMarker = (size /*: number */) => {
    const half = size / 2;
    return `M${half} 0 L${size} ${half} L ${half} ${size} L0 ${half} L${half} 0 L${size} ${half}`;
  };

  handleDoubleClick = (xScale /*: Scale */, xDim /*: Array<number> */) => () => {
    this.handleZoomUpdate(xScale, xDim);
  };

  handleSelectionDrag = (
    xScale /*: Scale */,
    width /*: number */,
    xDim /*: Array<number> */,
    checkDelta /*: ?boolean */
  ) => (e /*: Event */, data /*: DraggableData */) => {
    if (!checkDelta || data.deltaX) {
      const x = Math.max(xDim[0], Math.min(data.x, xDim[1] - width));
      this.handleZoomUpdate(xScale, [x, width + x]);
    }
  };

  handleSelectionHandleDrag = (
    xScale /*: Scale */,
    fixedX /*: number */,
    xDim /*: Array<number> */,
    handleDirection /*: string */,
    checkDelta /*: ?boolean */
  ) => (e /*: Event */, data /*: DraggableData */) => {
    if (!checkDelta || data.deltaX) {
      const x = Math.max(xDim[0], Math.min(data.x, xDim[1]));
      this.handleZoomUpdate(xScale, handleDirection === 'right' ? [fixedX, x] : [x, fixedX]);
    }
  };

  handleNewZoomDragStart = (xDim /*: Array<number> */) => (
    e /*: Event */,
    data /*: DraggableData */
  ) => {
    const overlayLeftPos = data.node.getBoundingClientRect().left;
    this.setState({
      overlayLeftPos,
      newZoomStart: Math.round(Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1])))
    });
  };

  handleNewZoomDrag = (xScale /*: Scale */, xDim /*: Array<number> */) => (
    e /*: Event */,
    data /*: DraggableData */
  ) => {
    const { newZoomStart, overlayLeftPos } = this.state;
    if (newZoomStart != null && overlayLeftPos != null && data.deltaX) {
      this.handleZoomUpdate(
        xScale,
        sortBy([newZoomStart, Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1]))])
      );
    }
  };

  handleNewZoomDragEnd = (xScale /*: Scale */, xDim /*: Array<number> */) => (
    e /*: Event */,
    data /*: DraggableData */
  ) => {
    const { newZoomStart, overlayLeftPos } = this.state;
    if (newZoomStart != null && overlayLeftPos != null) {
      const x = Math.round(Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1])));
      this.handleZoomUpdate(xScale, newZoomStart === x ? xDim : sortBy([newZoomStart, x]));
      this.setState({ newZoomStart: null, overlayLeftPos: null });
    }
  };

  handleZoomUpdate = (xScale /*: Scale */, xArray /*: Array<number> */) => {
    const xRange = xScale.range();
    const startDate =
      xArray[0] > xRange[0] && xArray[0] < xRange[xRange.length - 1]
        ? xScale.invert(xArray[0])
        : null;
    const endDate =
      xArray[1] > xRange[0] && xArray[1] < xRange[xRange.length - 1]
        ? xScale.invert(xArray[1])
        : null;
    if (this.props.startDate !== startDate || this.props.endDate !== endDate) {
      this.props.updateZoom(startDate, endDate);
    }
  };

  renderBaseLine = (xScale /*: Scale */, yScale /*: Scale */) => {
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

  renderTicks = (xScale /*: Scale */, yScale /*: Scale */) => {
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

  renderLeak = (xScale /*: Scale */, yScale /*: Scale */) => {
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
        fill={theme.leakColor}
      />
    );
  };

  renderLines = (xScale /*: Scale */, yScale /*: Scale */) => {
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
            key={serie.name}
            className={classNames('line-chart-path', 'line-chart-path-' + idx)}
            d={lineGenerator(serie.data)}
          />
        ))}
      </g>
    );
  };

  renderAreas = (xScale /*: Scale */, yScale /*: Scale */) => {
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
            key={serie.name}
            className={classNames('line-chart-area', 'line-chart-area-' + idx)}
            d={areaGenerator(serie.data)}
          />
        ))}
      </g>
    );
  };

  renderZoomHandle = (
    options /*: {
    xScale: Scale,
    xPos: number,
    fixedPos: number,
    yDim: Array<number>,
    xDim: Array<number>,
    direction: string
  } */
  ) => (
    <Draggable
      axis="x"
      bounds={{ left: options.xDim[0], right: options.xDim[1] }}
      position={{ x: options.xPos, y: 0 }}
      onDrag={this.handleSelectionHandleDrag(
        options.xScale,
        options.fixedPos,
        options.xDim,
        options.direction,
        true
      )}
      onStop={this.handleSelectionHandleDrag(
        options.xScale,
        options.fixedPos,
        options.xDim,
        options.direction
      )}>
      <rect
        className="zoom-selection-handle"
        x={-3}
        y={options.yDim[1]}
        height={options.yDim[0] - options.yDim[1] + 1}
        width={6}
      />
    </Draggable>
  );

  renderZoom = (xScale /*: Scale */, yScale /*: Scale */) => {
    const xRange = xScale.range();
    const yRange = yScale.range();
    const xDim = [xRange[0], xRange[xRange.length - 1]];
    const yDim = [yRange[0], yRange[yRange.length - 1]];
    const startX = Math.round(this.props.startDate ? xScale(this.props.startDate) : xDim[0]);
    const endX = Math.round(this.props.endDate ? xScale(this.props.endDate) : xDim[1]);
    const xArray = sortBy([startX, endX]);
    const zoomBoxWidth = xArray[1] - xArray[0];
    const showZoomArea =
      this.state.newZoomStart == null ||
      this.state.newZoomStart === startX ||
      this.state.newZoomStart === endX;

    return (
      <g className="chart-zoom">
        <DraggableCore
          onStart={this.handleNewZoomDragStart(xDim)}
          onDrag={this.handleNewZoomDrag(xScale, xDim)}
          onStop={this.handleNewZoomDragEnd(xScale, xDim)}>
          <rect
            className="zoom-overlay"
            x={xDim[0]}
            y={yDim[1]}
            height={yDim[0] - yDim[1]}
            width={xDim[1] - xDim[0]}
          />
        </DraggableCore>
        {showZoomArea && (
          <Draggable
            axis="x"
            bounds={{ left: xDim[0], right: Math.floor(xDim[1] - zoomBoxWidth) }}
            position={{ x: xArray[0], y: 0 }}
            onDrag={this.handleSelectionDrag(xScale, zoomBoxWidth, xDim, true)}
            onStop={this.handleSelectionDrag(xScale, zoomBoxWidth, xDim)}>
            <rect
              className="zoom-selection"
              x={0}
              y={yDim[1]}
              height={yDim[0] - yDim[1] + 1}
              width={zoomBoxWidth}
              onDoubleClick={this.handleDoubleClick(xScale, xDim)}
            />
          </Draggable>
        )}
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
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0] + 2})`}>
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
