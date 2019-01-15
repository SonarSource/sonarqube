/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import { flatten, isEqual, sortBy, throttle, uniq } from 'lodash';
import { bisector, extent, max } from 'd3-array';
import { scaleLinear, scalePoint, scaleTime, ScaleTime } from 'd3-scale';
import { line as d3Line, area, curveBasis } from 'd3-shape';
import * as theme from '../../app/theme';
import { Serie, Point } from '../../apps/projectActivity/utils';
import { isDefined } from '../../helpers/types';
import './LineChart.css';
import './AdvancedTimeline.css';

export interface Props {
  basisCurve?: boolean;
  endDate?: Date;
  disableZoom?: boolean;
  formatYTick?: (tick: number | string) => string;
  hideGrid?: boolean;
  hideXAxis?: boolean;
  height: number;
  width: number;
  leakPeriodDate?: Date;
  // used to avoid same y ticks labels
  maxYTicksCount: number;
  metricType: string;
  padding: number[];
  selectedDate?: Date;
  series: Serie[];
  showAreas?: boolean;
  startDate?: Date;
  updateSelectedDate?: (selectedDate?: Date) => void;
  updateTooltip?: (selectedDate?: Date, tooltipXPos?: number, tooltipIdx?: number) => void;
  updateZoom?: (start?: Date, endDate?: Date) => void;
  zoomSpeed: number;
}

type XScale = ScaleTime<number, number>;
// TODO it should be `ScaleLinear<number, number> | ScalePoint<number> | ScalePoint<string>`, but it's super hard to make it work :'(
type YScale = any;

interface State {
  maxXRange: number[];
  mouseOver?: boolean;
  selectedDate?: Date;
  selectedDateXPos?: number;
  selectedDateIdx?: number;
  yScale: YScale;
  xScale: XScale;
}

export default class AdvancedTimeline extends React.PureComponent<Props, State> {
  static defaultProps = {
    eventSize: 8,
    maxYTicksCount: 4,
    padding: [10, 10, 30, 60],
    zoomSpeed: 1
  };

  constructor(props: Props) {
    super(props);
    const scales = this.getScales(props);
    const selectedDatePos = this.getSelectedDatePos(scales.xScale, props.selectedDate);
    this.state = { ...scales, ...selectedDatePos };
    this.updateTooltipPos = throttle(this.updateTooltipPos, 40);
    this.handleZoomUpdate = throttle(this.handleZoomUpdate, 40);
  }

  componentWillReceiveProps(nextProps: Props) {
    let scales;
    let selectedDatePos;
    if (
      nextProps.metricType !== this.props.metricType ||
      nextProps.startDate !== this.props.startDate ||
      nextProps.endDate !== this.props.endDate ||
      nextProps.width !== this.props.width ||
      nextProps.padding !== this.props.padding ||
      nextProps.height !== this.props.height ||
      nextProps.series !== this.props.series
    ) {
      scales = this.getScales(nextProps);
      if (this.state.selectedDate != null) {
        selectedDatePos = this.getSelectedDatePos(scales.xScale, this.state.selectedDate);
      }
    }

    if (!isEqual(nextProps.selectedDate, this.props.selectedDate)) {
      const xScale = scales ? scales.xScale : this.state.xScale;
      selectedDatePos = this.getSelectedDatePos(xScale, nextProps.selectedDate);
    }

    if (scales || selectedDatePos) {
      if (scales) {
        this.setState({ ...scales });
      }
      if (selectedDatePos) {
        this.setState({ ...selectedDatePos });
      }

      if (selectedDatePos && nextProps.updateTooltip) {
        nextProps.updateTooltip(
          selectedDatePos.selectedDate,
          selectedDatePos.selectedDateXPos,
          selectedDatePos.selectedDateIdx
        );
      }
    }
  }

  getRatingScale = (availableHeight: number) => {
    return scalePoint<number>()
      .domain([5, 4, 3, 2, 1])
      .range([availableHeight, 0]);
  };

  getLevelScale = (availableHeight: number) => {
    return scalePoint()
      .domain(['ERROR', 'WARN', 'OK'])
      .range([availableHeight, 0]);
  };

  getYScale = (props: Props, availableHeight: number, flatData: Point[]): YScale => {
    if (props.metricType === 'RATING') {
      return this.getRatingScale(availableHeight);
    } else if (props.metricType === 'LEVEL') {
      return this.getLevelScale(availableHeight);
    } else {
      return scaleLinear()
        .range([availableHeight, 0])
        .domain([0, max(flatData, d => Number(d.y || 0)) || 1])
        .nice();
    }
  };

  getXScale = ({ startDate, endDate }: Props, availableWidth: number, flatData: Point[]) => {
    const dateRange = extent(flatData, d => d.x) as [Date, Date];
    const start = startDate && startDate > dateRange[0] ? startDate : dateRange[0];
    const end = endDate && endDate < dateRange[1] ? endDate : dateRange[1];
    const xScale: ScaleTime<number, number> = scaleTime()
      .domain(sortBy([start, end]))
      .range([0, availableWidth])
      .clamp(false);
    return {
      xScale,
      maxXRange: dateRange.map(xScale)
    };
  };

  getScales = (props: Props) => {
    const availableWidth = props.width - props.padding[1] - props.padding[3];
    const availableHeight = props.height - props.padding[0] - props.padding[2];
    const flatData = flatten(props.series.map(serie => serie.data));
    return {
      ...this.getXScale(props, availableWidth, flatData),
      yScale: this.getYScale(props, availableHeight, flatData)
    };
  };

  getSelectedDatePos = (xScale: XScale, selectedDate?: Date) => {
    const firstSerie = this.props.series[0];
    if (selectedDate && firstSerie) {
      const idx = firstSerie.data.findIndex(p => p.x.valueOf() === selectedDate.valueOf());
      const xRange = sortBy(xScale.range());
      const xPos = xScale(selectedDate);
      if (idx >= 0 && xPos >= xRange[0] && xPos <= xRange[1]) {
        return {
          selectedDate,
          selectedDateXPos: xScale(selectedDate),
          selectedDateIdx: idx
        };
      }
    }
    return { selectedDate: undefined, selectedDateXPos: undefined, selectedDateIdx: undefined };
  };

  getEventMarker = (size: number) => {
    const half = size / 2;
    return `M${half} 0 L${size} ${half} L ${half} ${size} L0 ${half} L${half} 0 L${size} ${half}`;
  };

  handleWheel = (event: React.WheelEvent<SVGElement>) => {
    event.preventDefault();
    const { maxXRange, xScale } = this.state;
    const parentBbox = event.currentTarget.getBoundingClientRect();
    const mouseXPos = (event.pageX - parentBbox.left) / parentBbox.width;
    const xRange = xScale.range();
    const speed = event.deltaMode
      ? (25 / event.deltaMode) * this.props.zoomSpeed
      : this.props.zoomSpeed;
    const leftPos = xRange[0] - Math.round(speed * event.deltaY * mouseXPos);
    const rightPos = xRange[1] + Math.round(speed * event.deltaY * (1 - mouseXPos));
    const startDate = leftPos > maxXRange[0] ? xScale.invert(leftPos) : undefined;
    const endDate = rightPos < maxXRange[1] ? xScale.invert(rightPos) : undefined;
    this.handleZoomUpdate(startDate, endDate);
  };

  handleZoomUpdate = (startDate?: Date, endDate?: Date) => {
    if (this.props.updateZoom) {
      this.props.updateZoom(startDate, endDate);
    }
  };

  handleMouseMove = (event: React.MouseEvent<SVGElement>) => {
    const parentBbox = event.currentTarget.getBoundingClientRect();
    this.updateTooltipPos(event.pageX - parentBbox.left);
  };

  handleMouseEnter = () => {
    this.setState({ mouseOver: true });
  };

  handleMouseOut = () => {
    const { updateTooltip } = this.props;
    if (updateTooltip) {
      this.setState({
        mouseOver: false,
        selectedDate: undefined,
        selectedDateXPos: undefined,
        selectedDateIdx: undefined
      });
      updateTooltip(undefined, undefined, undefined);
    }
  };

  handleClick = () => {
    const { updateSelectedDate } = this.props;
    if (updateSelectedDate) {
      updateSelectedDate(this.state.selectedDate || undefined);
    }
  };

  updateTooltipPos = (xPos: number) => {
    const firstSerie = this.props.series[0];
    if (this.state.mouseOver && firstSerie) {
      const { updateTooltip } = this.props;
      const date = this.state.xScale.invert(xPos);
      const bisectX = bisector<Point, Date>(d => d.x).right;
      let idx = bisectX(firstSerie.data, date);
      if (idx >= 0) {
        const previousPoint = firstSerie.data[idx - 1];
        const nextPoint = firstSerie.data[idx];
        if (
          !nextPoint ||
          (previousPoint &&
            date.valueOf() - previousPoint.x.valueOf() <= nextPoint.x.valueOf() - date.valueOf())
        ) {
          idx--;
        }
        const selectedDate = firstSerie.data[idx].x;
        const xPos = this.state.xScale(selectedDate);
        this.setState({ selectedDate, selectedDateXPos: xPos, selectedDateIdx: idx });
        if (updateTooltip) {
          updateTooltip(selectedDate, xPos, idx);
        }
      }
    }
  };

  renderHorizontalGrid = () => {
    const { formatYTick } = this.props;
    const { xScale, yScale } = this.state;
    const hasTicks = typeof yScale.ticks === 'function';
    let ticks: Array<string | number> = hasTicks
      ? yScale.ticks(this.props.maxYTicksCount)
      : yScale.domain();

    if (!ticks.length) {
      ticks.push(yScale.domain()[1]);
    }

    // if there are duplicated ticks, that means 4 ticks are too much for this data
    // so let's just use the domain values (min and max)
    if (formatYTick) {
      const formattedTicks = ticks.map(tick => formatYTick(tick));
      if (ticks.length > uniq(formattedTicks).length) {
        ticks = yScale.domain();
      }
    }

    return (
      <g>
        {ticks.map(tick => (
          <g key={tick}>
            {formatYTick != null && (
              <text
                className="line-chart-tick line-chart-tick-x"
                dx="-1em"
                dy="0.3em"
                textAnchor="end"
                x={xScale.range()[0]}
                y={yScale(tick)}>
                {formatYTick(tick)}
              </text>
            )}
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

  renderXAxisTicks = () => {
    const { xScale, yScale } = this.state;
    const format = xScale.tickFormat(7);
    const ticks = xScale.ticks(7);
    const y = yScale.range()[0];
    return (
      <g>
        {ticks.slice(0, -1).map((tick, index) => {
          const nextTick = index + 1 < ticks.length ? ticks[index + 1] : xScale.domain()[1];
          const x = (xScale(tick) + xScale(nextTick)) / 2;
          return (
            <text className="line-chart-tick" dy="1.5em" key={index} x={x} y={y}>
              {format(tick)}
            </text>
          );
        })}
      </g>
    );
  };

  renderLeak = () => {
    const { leakPeriodDate } = this.props;
    if (!leakPeriodDate) {
      return null;
    }
    const { xScale, yScale } = this.state;
    const yRange = yScale.range();
    const xRange = xScale.range();
    const leakWidth = xRange[xRange.length - 1] - xScale(leakPeriodDate);
    if (leakWidth < 0) {
      return null;
    }
    return (
      <rect
        className="leak-chart-rect"
        fill={theme.leakColor}
        height={yRange[0] - yRange[yRange.length - 1]}
        width={leakWidth}
        x={xScale(leakPeriodDate)}
        y={yRange[yRange.length - 1]}
      />
    );
  };

  renderLines = () => {
    const lineGenerator = d3Line<Point>()
      .defined(d => Boolean(d.y || d.y === 0))
      .x(d => this.state.xScale(d.x))
      .y(d => this.state.yScale(d.y));
    if (this.props.basisCurve) {
      lineGenerator.curve(curveBasis);
    }
    return (
      <g>
        {this.props.series.map((serie, idx) => (
          <path
            className={classNames('line-chart-path', 'line-chart-path-' + idx)}
            d={lineGenerator(serie.data) || undefined}
            key={serie.name}
          />
        ))}
      </g>
    );
  };

  renderDots = () => {
    return (
      <g>
        {this.props.series
          .map((serie, serieIdx) =>
            serie.data
              .map((point, idx) => {
                const pointNotDefined = !point.y && point.y !== 0;
                const hasPointBefore =
                  serie.data[idx - 1] && (serie.data[idx - 1].y || serie.data[idx - 1].y === 0);
                const hasPointAfter =
                  serie.data[idx + 1] && (serie.data[idx + 1].y || serie.data[idx + 1].y === 0);
                if (pointNotDefined || (hasPointBefore || hasPointAfter)) {
                  return undefined;
                }
                return (
                  <circle
                    className={classNames('line-chart-dot', 'line-chart-dot-' + serieIdx)}
                    cx={this.state.xScale(point.x)}
                    cy={this.state.yScale(point.y)}
                    key={serie.name + idx}
                    r="2"
                  />
                );
              })
              .filter(isDefined)
          )
          .filter(dots => dots.length > 0)}
      </g>
    );
  };

  renderAreas = () => {
    const areaGenerator = area<Point>()
      .defined(d => Boolean(d.y || d.y === 0))
      .x(d => this.state.xScale(d.x))
      .y1(d => this.state.yScale(d.y))
      .y0(this.state.yScale(0));
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

  renderSelectedDate = () => {
    const { selectedDateIdx, selectedDateXPos, yScale } = this.state;
    const firstSerie = this.props.series[0];
    if (selectedDateIdx == null || selectedDateXPos == null || !firstSerie) {
      return null;
    }

    return (
      <g>
        <line
          className="line-tooltip"
          x1={selectedDateXPos}
          x2={selectedDateXPos}
          y1={yScale.range()[0]}
          y2={yScale.range()[1]}
        />
        {this.props.series.map((serie, idx) => {
          const point = serie.data[selectedDateIdx];
          if (!point || (!point.y && point.y !== 0)) {
            return null;
          }
          return (
            <circle
              className={classNames('line-chart-dot', 'line-chart-dot-' + idx)}
              cx={selectedDateXPos}
              cy={yScale(point.y)}
              key={serie.name}
              r="4"
            />
          );
        })}
      </g>
    );
  };

  renderClipPath = () => {
    return (
      <defs>
        <clipPath id="chart-clip">
          <rect
            height={this.state.yScale.range()[0] + 10}
            transform="translate(0,-5)"
            width={this.state.xScale.range()[1]}
          />
        </clipPath>
      </defs>
    );
  };

  renderMouseEventsOverlay = (zoomEnabled: boolean) => {
    const mouseEvents: Partial<React.SVGProps<SVGRectElement>> = {};
    if (zoomEnabled) {
      mouseEvents.onWheel = this.handleWheel;
    }
    if (this.props.updateTooltip) {
      mouseEvents.onMouseEnter = this.handleMouseEnter;
      mouseEvents.onMouseMove = this.handleMouseMove;
      mouseEvents.onMouseOut = this.handleMouseOut;
    }
    if (this.props.updateSelectedDate) {
      mouseEvents.onClick = this.handleClick;
    }
    return (
      <rect
        className="chart-mouse-events-overlay"
        height={this.state.yScale.range()[0]}
        width={this.state.xScale.range()[1]}
        {...mouseEvents}
      />
    );
  };

  render() {
    if (!this.props.width || !this.props.height) {
      return <div />;
    }
    const zoomEnabled = !this.props.disableZoom && this.props.updateZoom != null;
    const isZoomed = Boolean(this.props.startDate || this.props.endDate);
    return (
      <svg
        className={classNames('line-chart', { 'chart-zoomed': isZoomed })}
        height={this.props.height}
        width={this.props.width}>
        {zoomEnabled && this.renderClipPath()}
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          {this.props.leakPeriodDate != null && this.renderLeak()}
          {!this.props.hideGrid && this.renderHorizontalGrid()}
          {!this.props.hideXAxis && this.renderXAxisTicks()}
          {this.props.showAreas && this.renderAreas()}
          {this.renderLines()}
          {this.renderDots()}
          {this.renderSelectedDate()}
          {this.renderMouseEventsOverlay(zoomEnabled)}
        </g>
      </svg>
    );
  }
}
