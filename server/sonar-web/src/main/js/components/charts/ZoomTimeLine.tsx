/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import classNames from 'classnames';
import { extent, max } from 'd3-array';
import { ScaleTime, scaleLinear, scalePoint, scaleTime } from 'd3-scale';
import { area, curveBasis, line as d3Line } from 'd3-shape';
import { ThemeProp, themeColor, withTheme } from 'design-system';
import { flatten, sortBy, throttle } from 'lodash';
import * as React from 'react';
import Draggable, { DraggableBounds, DraggableCore, DraggableData } from 'react-draggable';
import { MetricType } from '../../types/metrics';
import { Chart } from '../../types/types';
import './LineChart.css';
import './ZoomTimeLine.css';

export interface PropsWithoutTheme {
  basisCurve?: boolean;
  endDate?: Date;
  height: number;
  leakPeriodDate?: Date;
  metricType: string;
  padding?: number[];
  series: Chart.Serie[];
  showAreas?: boolean;
  showXTicks?: boolean;
  startDate?: Date;
  updateZoom: (start?: Date, endDate?: Date) => void;
  width: number;
}

export type Props = PropsWithoutTheme & ThemeProp;

export type PropsWithDefaults = Props & typeof ZoomTimeLineClass.defaultProps;

interface State {
  overlayLeftPos?: number;
  newZoomStart?: number;
}

type XScale = ScaleTime<number, number>;

export class ZoomTimeLineClass extends React.PureComponent<Props, State> {
  static defaultProps = {
    padding: [0, 0, 18, 0],
  };

  constructor(props: PropsWithDefaults) {
    super(props);

    this.state = {};
    this.handleZoomUpdate = throttle(this.handleZoomUpdate, 40);
  }

  getRatingScale = (availableHeight: number) => {
    return scalePoint<number>().domain([5, 4, 3, 2, 1]).range([availableHeight, 0]);
  };

  getLevelScale = (availableHeight: number) => {
    return scalePoint().domain(['ERROR', 'WARN', 'OK']).range([availableHeight, 0]);
  };

  getYScale = (availableHeight: number, flatData: Chart.Point[]) => {
    if (this.props.metricType === MetricType.Rating) {
      return this.getRatingScale(availableHeight);
    } else if (this.props.metricType === MetricType.Level) {
      return this.getLevelScale(availableHeight);
    }

    return scaleLinear()
      .range([availableHeight, 0])
      .domain([0, max(flatData, (d) => Number(d.y || 0)) as number])
      .nice();
  };

  getXScale = (availableWidth: number, flatData: Chart.Point[]): XScale => {
    return scaleTime()
      .domain(extent(flatData, (d) => d.x) as [Date, Date])
      .range([0, availableWidth])
      .clamp(true);
  };

  getScales = () => {
    const { padding } = this.props as PropsWithDefaults;

    const availableWidth = this.props.width - padding[1] - padding[3];
    const availableHeight = this.props.height - padding[0] - padding[2];
    const flatData = flatten(this.props.series.map((serie) => serie.data));

    return {
      xScale: this.getXScale(availableWidth, flatData),
      yScale: this.getYScale(availableHeight, flatData),
    };
  };

  handleDoubleClick = (xScale: XScale, xDim: number[]) => () => {
    this.handleZoomUpdate(xScale, xDim);
  };

  handleSelectionDrag =
    (xScale: XScale, width: number, xDim: number[], checkDelta = false) =>
    (_: MouseEvent, data: DraggableData) => {
      if (!checkDelta || data.deltaX) {
        const x = Math.max(xDim[0], Math.min(data.x, xDim[1] - width));
        this.handleZoomUpdate(xScale, [x, width + x]);
      }
    };

  handleSelectionHandleDrag =
    (xScale: XScale, fixedX: number, xDim: number[], handleDirection: string, checkDelta = false) =>
    (_: MouseEvent, data: DraggableData) => {
      if (!checkDelta || data.deltaX) {
        const x = Math.max(xDim[0], Math.min(data.x, xDim[1]));
        this.handleZoomUpdate(xScale, handleDirection === 'right' ? [fixedX, x] : [x, fixedX]);
      }
    };

  handleNewZoomDragStart = (xDim: number[]) => (_: MouseEvent, data: DraggableData) => {
    const overlayLeftPos = data.node.getBoundingClientRect().left;

    this.setState({
      overlayLeftPos,
      newZoomStart: Math.round(Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1]))),
    });
  };

  handleNewZoomDrag = (xScale: XScale, xDim: number[]) => (_: MouseEvent, data: DraggableData) => {
    const { newZoomStart, overlayLeftPos } = this.state;

    if (newZoomStart != null && overlayLeftPos != null && data.deltaX) {
      this.handleZoomUpdate(
        xScale,
        sortBy([newZoomStart, Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1]))])
      );
    }
  };

  handleNewZoomDragEnd =
    (xScale: XScale, xDim: number[]) => (_: MouseEvent, data: DraggableData) => {
      const { newZoomStart, overlayLeftPos } = this.state;

      if (newZoomStart !== undefined && overlayLeftPos !== undefined) {
        const x = Math.round(Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1])));
        this.handleZoomUpdate(xScale, newZoomStart === x ? xDim : sortBy([newZoomStart, x]));
        this.setState({ newZoomStart: undefined, overlayLeftPos: undefined });
      }
    };

  handleZoomUpdate = (xScale: XScale, xArray: number[]) => {
    const xRange = xScale.range();

    const startDate =
      xArray[0] > xRange[0] && xArray[0] < xRange[xRange.length - 1]
        ? xScale.invert(xArray[0])
        : undefined;

    const endDate =
      xArray[1] > xRange[0] && xArray[1] < xRange[xRange.length - 1]
        ? xScale.invert(xArray[1])
        : undefined;

    this.props.updateZoom(startDate, endDate);
  };

  renderBaseLine = (xScale: XScale, yScale: { range: () => number[] }) => {
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

  renderTicks = (xScale: XScale, yScale: { range: () => number[] }) => {
    const format = xScale.tickFormat(7);
    const ticks = xScale.ticks(7);
    const y = yScale.range()[0];

    return (
      <g>
        {ticks.slice(0, -1).map((tick, index) => {
          const nextTick = index + 1 < ticks.length ? ticks[index + 1] : xScale.domain()[1];
          const x = (xScale(tick) + xScale(nextTick)) / 2;

          return (
            // eslint-disable-next-line react/no-array-index-key
            <text className="chart-zoom-tick" dy="1.3em" key={index} x={x} y={y}>
              {format(tick)}
            </text>
          );
        })}
      </g>
    );
  };

  renderNewCode = (xScale: XScale, yScale: { range: () => number[] }) => {
    const { leakPeriodDate, theme } = this.props;

    if (!leakPeriodDate) {
      return null;
    }

    const yRange = yScale.range();

    return (
      <rect
        fill={themeColor('newCodeLegend')({ theme })}
        height={yRange[0] - yRange[yRange.length - 1]}
        width={xScale.range()[1] - xScale(leakPeriodDate)}
        x={xScale(leakPeriodDate)}
        y={yRange[yRange.length - 1]}
      />
    );
  };

  renderLines = (xScale: XScale, yScale: (y: string | number | undefined) => number) => {
    const { series, theme } = this.props;

    const lineGenerator = d3Line<Chart.Point>()
      .defined((d) => Boolean(d.y || d.y === 0))
      .x((d) => xScale(d.x))
      .y((d) => yScale(d.y));

    if (this.props.basisCurve) {
      lineGenerator.curve(curveBasis);
    }

    return (
      <g>
        {series.map((serie, idx) => (
          <path
            className={classNames('line-chart-path', `line-chart-path-${idx}`)}
            d={lineGenerator(serie.data) ?? undefined}
            key={serie.name}
            stroke={themeColor(`graphLineColor.${idx}` as Parameters<typeof themeColor>[0])({
              theme,
            })}
          />
        ))}
      </g>
    );
  };

  renderAreas = (xScale: XScale, yScale: (y: string | number | undefined) => number) => {
    const areaGenerator = area<Chart.Point>()
      .defined((d) => Boolean(d.y || d.y === 0))
      .x((d) => xScale(d.x))
      .y1((d) => yScale(d.y))
      .y0(yScale(0));

    if (this.props.basisCurve) {
      areaGenerator.curve(curveBasis);
    }

    return (
      <g>
        {this.props.series.map((serie, idx) => (
          <path
            className={classNames('line-chart-area', 'line-chart-area-' + idx)}
            d={areaGenerator(serie.data) || undefined}
            key={serie.name}
          />
        ))}
      </g>
    );
  };

  renderZoomHandle = (options: {
    xScale: XScale;
    xPos: number;
    fixedPos: number;
    yDim: number[];
    xDim: number[];
    direction: string;
  }) => (
    <Draggable
      axis="x"
      bounds={{ left: options.xDim[0], right: options.xDim[1] } as DraggableBounds}
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
      )}
      position={{ x: options.xPos, y: 0 }}
    >
      <rect
        className="zoom-selection-handle"
        height={options.yDim[0] - options.yDim[1] + 1}
        width={6}
        x={-3}
        y={options.yDim[1]}
      />
    </Draggable>
  );

  renderZoom = (xScale: XScale, yScale: { range: () => number[] }) => {
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
          onDrag={this.handleNewZoomDrag(xScale, xDim)}
          onStart={this.handleNewZoomDragStart(xDim)}
          onStop={this.handleNewZoomDragEnd(xScale, xDim)}
        >
          <rect
            className="zoom-overlay"
            height={yDim[0] - yDim[1]}
            width={xDim[1] - xDim[0]}
            x={xDim[0]}
            y={yDim[1]}
          />
        </DraggableCore>
        {showZoomArea && (
          <Draggable
            axis="x"
            bounds={{ left: xDim[0], right: Math.floor(xDim[1] - zoomBoxWidth) } as DraggableBounds}
            onDrag={this.handleSelectionDrag(xScale, zoomBoxWidth, xDim, true)}
            onStop={this.handleSelectionDrag(xScale, zoomBoxWidth, xDim)}
            position={{ x: xArray[0], y: 0 }}
          >
            <rect
              className="zoom-selection"
              height={yDim[0] - yDim[1] + 1}
              onDoubleClick={this.handleDoubleClick(xScale, xDim)}
              width={zoomBoxWidth}
              x={0}
              y={yDim[1]}
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
            direction: 'left',
          })}
        {showZoomArea &&
          this.renderZoomHandle({
            xScale,
            xPos: endX,
            fixedPos: startX,
            xDim,
            yDim,
            direction: 'right',
          })}
      </g>
    );
  };

  render() {
    const { padding, showXTicks = true } = this.props as PropsWithDefaults;

    if (!this.props.width || !this.props.height) {
      return <div />;
    }

    const { xScale, yScale } = this.getScales();

    return (
      <svg className="line-chart " height={this.props.height} width={this.props.width}>
        <g transform={`translate(${padding[3]}, ${padding[0] + 2})`}>
          {this.renderNewCode(xScale, yScale as Parameters<typeof this.renderNewCode>[1])}
          {this.renderBaseLine(xScale, yScale as Parameters<typeof this.renderBaseLine>[1])}
          {showXTicks && this.renderTicks(xScale, yScale as Parameters<typeof this.renderTicks>[1])}
          {this.props.showAreas &&
            this.renderAreas(xScale, yScale as Parameters<typeof this.renderAreas>[1])}
          {this.renderLines(xScale, yScale as Parameters<typeof this.renderLines>[1])}
          {this.renderZoom(xScale, yScale as Parameters<typeof this.renderZoom>[1])}
        </g>
      </svg>
    );
  }
}

export const ZoomTimeLine = withTheme<PropsWithoutTheme>(ZoomTimeLineClass);
