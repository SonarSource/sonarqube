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

import styled from '@emotion/styled';
import { extent, max } from 'd3-array';
import { ScaleTime, scaleLinear, scalePoint, scaleTime } from 'd3-scale';
import { area, curveBasis, line as d3Line } from 'd3-shape';
import { flatten, sortBy, throttle } from 'lodash';
import * as React from 'react';
import Draggable, { DraggableBounds, DraggableCore, DraggableData } from 'react-draggable';
import { CSSColor, DraggableIcon, themeColor } from '~design-system';
import { MetricType } from '~sonar-aligned/types/metrics';
import { Chart } from '../../types/types';
import { LINE_CHART_DASHES } from '../activity-graph/utils';

export interface Props {
  basisCurve?: boolean;
  endDate?: Date;
  height: number;
  leakPeriodDate?: Date;
  metricType: string;
  padding?: number[];
  series: Chart.Serie[];
  showAreas?: boolean;
  startDate?: Date;
  updateZoom: (start?: Date, endDate?: Date) => void;
  width: number;
}

const DEFAULT_PADDING = [0, 0, 18, 0];

interface State {
  newZoomStart?: number;
  overlayLeftPos?: number;
}

type XScale = ScaleTime<number, number>;

export class ZoomTimeLine extends React.PureComponent<Props, State> {
  constructor(props: Props) {
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
    const { padding = DEFAULT_PADDING } = this.props;

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
        sortBy([newZoomStart, Math.max(xDim[0], Math.min(data.x - overlayLeftPos, xDim[1]))]),
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
      <StyledBaseLine
        x1={xScale.range()[0]}
        x2={xScale.range()[1]}
        y1={yScale.range()[0]}
        y2={yScale.range()[0]}
      />
    );
  };

  renderNewCode = (xScale: XScale, yScale: { range: () => number[] }) => {
    const { leakPeriodDate } = this.props;

    if (!leakPeriodDate) {
      return null;
    }

    const yRange = yScale.range();

    return (
      <StyledNewCodeLegend
        height={yRange[0] - yRange[yRange.length - 1]}
        width={xScale.range()[1] - xScale(leakPeriodDate)}
        x={xScale(leakPeriodDate)}
        y={yRange[yRange.length - 1]}
      />
    );
  };

  renderLines = (xScale: XScale, yScale: (y: string | number | undefined) => number) => {
    const { series } = this.props;

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
          <StyledPath index={idx} d={lineGenerator(serie.data) ?? undefined} key={serie.name} />
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
          <StyledArea index={idx} d={areaGenerator(serie.data) ?? undefined} key={serie.name} />
        ))}
      </g>
    );
  };

  renderZoomHandle = (options: {
    direction: string;
    fixedPos: number;
    xDim: number[];
    xPos: number;
    xScale: XScale;
    yDim: number[];
  }) => (
    <Draggable
      axis="x"
      bounds={{ left: options.xDim[0], right: options.xDim[1] } as DraggableBounds}
      onDrag={this.handleSelectionHandleDrag(
        options.xScale,
        options.fixedPos,
        options.xDim,
        options.direction,
        true,
      )}
      onStop={this.handleSelectionHandleDrag(
        options.xScale,
        options.fixedPos,
        options.xDim,
        options.direction,
      )}
      position={{ x: options.xPos, y: 0 }}
    >
      <g>
        <ZoomHighlightHandle
          height={options.yDim[0] - options.yDim[1] + 1}
          width={2}
          x={options.direction === 'right' ? 0 : -2}
          y={options.yDim[1]}
        />
        <DraggableIcon
          fill="var(--echoes-color-icon-subdued)"
          x={options.direction === 'right' ? -7 : -9}
          y={16}
        />
      </g>
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
      <g>
        <DraggableCore
          onDrag={this.handleNewZoomDrag(xScale, xDim)}
          onStart={this.handleNewZoomDragStart(xDim)}
          onStop={this.handleNewZoomDragEnd(xScale, xDim)}
        >
          <ZoomOverlay
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
            <ZoomHighlight
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
    const { padding = DEFAULT_PADDING, height, width } = this.props;

    if (width === 0 || height === 0) {
      return <div />;
    }

    const { xScale, yScale } = this.getScales();

    return (
      <svg height={this.props.height} width={this.props.width}>
        <g transform={`translate(${padding[3]}, ${padding[0] + 2})`}>
          {this.renderNewCode(xScale, yScale as Parameters<typeof this.renderNewCode>[1])}
          {this.renderBaseLine(xScale, yScale as Parameters<typeof this.renderBaseLine>[1])}
          {this.props.showAreas &&
            this.renderAreas(xScale, yScale as Parameters<typeof this.renderAreas>[1])}
          {this.renderLines(xScale, yScale as Parameters<typeof this.renderLines>[1])}
          {this.renderZoom(xScale, yScale as Parameters<typeof this.renderZoom>[1])}
        </g>
      </svg>
    );
  }
}

const ZoomHighlight = styled.rect`
  cursor: move;
  fill: ${themeColor('graphZoomBackgroundColor')};
  stroke: ${themeColor('graphZoomBorderColor')};
  fill-opacity: 0.2;
  shape-rendering: crispEdges;
`;

const ZoomHighlightHandle = styled.rect`
  cursor: ew-resize;
  fill-opacity: 1;
  fill: var(--echoes-color-icon-subdued);
  stroke: none;
`;

const ZoomOverlay = styled.rect`
  cursor: crosshair;
  pointer-events: all;
  fill: none;
  stroke: none;
`;

const AREA_OPACITY = 0.15;

const StyledArea = styled.path<{ index: number }>`
  clip-path: url(#chart-clip);
  fill: ${({ index }) => themeColor(`graphLineColor.${index}` as CSSColor, AREA_OPACITY)};
  stroke-width: 0;
`;

const StyledPath = styled.path<{ index: number }>`
  clip-path: url(#chart-clip);
  fill: none;
  stroke: ${({ index }) => themeColor(`graphLineColor.${index}` as CSSColor)};
  stroke-dasharray: ${({ index }) => LINE_CHART_DASHES[index]};
  stroke-width: 2px;
`;

const StyledNewCodeLegend = styled.rect`
  fill: ${themeColor('newCodeLegend')};
`;

const StyledBaseLine = styled('line')`
  shape-rendering: crispedges;
  stroke: ${themeColor('graphGridColor')};
`;
