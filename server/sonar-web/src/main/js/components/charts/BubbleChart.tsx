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
import * as React from 'react';
import * as classNames from 'classnames';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { Link } from 'react-router';
import { min, max } from 'd3-array';
import { scaleLinear, ScaleLinear } from 'd3-scale';
import { sortBy, uniq } from 'lodash';
import Tooltip from '../controls/Tooltip';
import { translate } from '../../helpers/l10n';

const TICKS_COUNT = 5;

interface BubbleProps {
  color?: string;
  link?: string;
  onClick?: (link?: string) => void;
  r: number;
  tooltip?: string | React.ReactNode;
  x: number;
  y: number;
}

export class Bubble extends React.PureComponent<BubbleProps> {
  handleClick = (event: React.MouseEvent<SVGCircleElement>) => {
    if (this.props.onClick) {
      event.stopPropagation();
      event.preventDefault();
      this.props.onClick(this.props.link);
    }
  };

  render() {
    let circle = (
      <circle
        className="bubble-chart-bubble"
        onClick={this.props.onClick ? this.handleClick : undefined}
        r={this.props.r}
        style={{ fill: this.props.color, stroke: this.props.color }}
        transform={`translate(${this.props.x}, ${this.props.y})`}
      />
    );

    if (this.props.link && !this.props.onClick) {
      circle = <Link to={this.props.link}>{circle}</Link>;
    }

    return (
      <Tooltip overlay={this.props.tooltip || undefined}>
        <g>{circle}</g>
      </Tooltip>
    );
  }
}

interface Item {
  color?: string;
  key?: string;
  link?: any;
  size: number;
  tooltip?: React.ReactNode;
  x: number;
  y: number;
}

interface Props {
  displayXGrid?: boolean;
  displayXTicks?: boolean;
  displayYGrid?: boolean;
  displayYTicks?: boolean;
  formatXTick: (tick: number) => string;
  formatYTick: (tick: number) => string;
  height: number;
  items: Item[];
  onBubbleClick?: (link?: string) => void;
  padding: [number, number, number, number];
  sizeDomain?: [number, number];
  sizeRange?: [number, number];
  xDomain?: [number, number];
  yDomain?: [number, number];
}

interface State {
  isMoving: boolean;
  moveOrigin: { x: number; y: number };
  zoom: number;
  zoomOrigin: { x: number; y: number };
}

type Scale = ScaleLinear<number, number>;

export default class BubbleChart extends React.Component<Props, State> {
  node: SVGSVGElement | null = null;

  static defaultProps = {
    displayXGrid: true,
    displayXTicks: true,
    displayYGrid: true,
    displayYTicks: true,
    formatXTick: (d: number) => d,
    formatYTick: (d: number) => d,
    padding: [10, 10, 10, 10],
    sizeRange: [5, 45]
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      isMoving: false,
      moveOrigin: { x: 0, y: 0 },
      zoom: 1,
      zoomOrigin: { x: 0, y: 0 }
    };
  }

  componentDidMount() {
    document.addEventListener('mouseup', this.stopMoving);
    document.addEventListener('mousemove', this.updateZoomCenter);
  }

  componentWillUnmount() {
    document.removeEventListener('mouseup', this.stopMoving);
    document.removeEventListener('mousemove', this.updateZoomCenter);
  }

  startMoving = (event: React.MouseEvent<SVGSVGElement>) => {
    if (this.node && this.state.zoom > 1) {
      const rect = this.node.getBoundingClientRect();
      this.setState({
        isMoving: true,
        moveOrigin: { x: event.clientX - rect.left, y: event.clientY - rect.top }
      });
    }
  };

  updateZoomCenter = (event: MouseEvent) => {
    if (this.node && this.state.isMoving) {
      const rect = this.node.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      this.setState(state => ({
        zoomOrigin: {
          x: Math.max(-100, state.zoomOrigin.x + (state.moveOrigin.x - x) / state.zoom),
          y: Math.max(-100, state.zoomOrigin.y + (state.moveOrigin.y - y) / state.zoom)
        },
        moveOrigin: { x, y }
      }));
    }
  };

  stopMoving = () => {
    this.setState({ isMoving: false });
  };

  onWheel = (event: React.WheelEvent<SVGSVGElement>) => {
    if (this.node) {
      event.stopPropagation();
      event.preventDefault();

      const rect = this.node.getBoundingClientRect();
      const mouseX = event.clientX - rect.left - this.props.padding[1];
      const mouseY = event.clientY - rect.top - this.props.padding[0];

      let delta = event.deltaY;
      if ((event as any).webkitDirectionInvertedFromDevice) {
        delta = -delta;
      }

      if (delta > 0) {
        this.handleZoomOut(mouseX, mouseY);
      } else {
        this.handleZoomIn(mouseX, mouseY);
      }
    }
  };

  handleZoomOut = (x: number, y: number) => {
    if (this.state.zoom === 1) {
      this.setState(state => ({
        zoom: Math.max(1.0, state.zoom - 0.5),
        zoomOrigin: { x, y }
      }));
    } else {
      this.setState(state => ({
        zoom: Math.max(1.0, state.zoom - 0.5)
      }));
    }
  };

  handleZoomIn = (x: number, y: number) => {
    if (this.state.zoom === 1) {
      this.setState(state => ({
        zoom: Math.min(10.0, state.zoom + 0.5),
        zoomOrigin: { x, y }
      }));
    } else {
      this.setState(state => ({
        zoom: Math.min(10.0, state.zoom + 0.5)
      }));
    }
  };

  resetZoom = (event: React.MouseEvent<Link>) => {
    event.stopPropagation();
    event.preventDefault();
    this.setState({
      zoom: 1,
      zoomOrigin: { x: 0, y: 0 }
    });
  };

  getXRange(xScale: Scale, sizeScale: Scale, availableWidth: number) {
    const minX = min(this.props.items, d => xScale(d.x) - sizeScale(d.size)) || 0;
    const maxX = max(this.props.items, d => xScale(d.x) + sizeScale(d.size)) || 0;
    const dMinX = minX < 0 ? xScale.range()[0] - minX : xScale.range()[0];
    const dMaxX = maxX > xScale.range()[1] ? maxX - xScale.range()[1] : 0;
    return [dMinX, availableWidth - dMaxX];
  }

  getYRange(yScale: Scale, sizeScale: Scale, availableHeight: number) {
    const minY = min(this.props.items, d => yScale(d.y) - sizeScale(d.size)) || 0;
    const maxY = max(this.props.items, d => yScale(d.y) + sizeScale(d.size)) || 0;
    const dMinY = minY < 0 ? yScale.range()[1] - minY : yScale.range()[1];
    const dMaxY = maxY > yScale.range()[0] ? maxY - yScale.range()[0] : 0;
    return [availableHeight - dMaxY, dMinY];
  }

  getTicks(scale: Scale, format: (d: number) => string) {
    const ticks = scale.ticks(TICKS_COUNT).map(tick => format(tick));
    const uniqueTicksCount = uniq(ticks).length;
    const ticksCount = uniqueTicksCount < TICKS_COUNT ? uniqueTicksCount - 1 : TICKS_COUNT;
    return scale.ticks(ticksCount);
  }

  getZoomLevelLabel = () => this.state.zoom * 100 + '%';

  renderXGrid = (ticks: Array<number>, xScale: Scale, yScale: Scale, delta: number) => {
    if (!this.props.displayXGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const x = xScale(tick);
      const y1 = yScale.range()[0];
      const y2 = yScale.range()[1];
      return (
        <line
          className="bubble-chart-grid"
          key={index}
          x1={x * this.state.zoom - delta}
          x2={x * this.state.zoom - delta}
          y1={y1}
          y2={y2}
        />
      );
    });

    return <g>{lines}</g>;
  };

  renderYGrid = (ticks: Array<number>, xScale: Scale, yScale: Scale, delta: number) => {
    if (!this.props.displayYGrid) {
      return null;
    }

    const lines = ticks.map((tick, index) => {
      const y = yScale(tick);
      const x1 = xScale.range()[0];
      const x2 = xScale.range()[1];
      return (
        <line
          className="bubble-chart-grid"
          key={index}
          x1={x1}
          x2={x2}
          y1={y * this.state.zoom - delta}
          y2={y * this.state.zoom - delta}
        />
      );
    });

    return <g>{lines}</g>;
  };

  renderXTicks = (xTicks: Array<number>, xScale: Scale, yScale: Scale, delta: number) => {
    if (!this.props.displayXTicks) {
      return null;
    }

    const ticks = xTicks.map((tick, index) => {
      const x = xScale(tick);
      const y = yScale.range()[0];
      const innerText = this.props.formatXTick(tick);
      return (
        <text
          className="bubble-chart-tick"
          dy="1.5em"
          key={index}
          x={x * this.state.zoom - delta}
          y={y}>
          {innerText}
        </text>
      );
    });

    return <g>{ticks}</g>;
  };

  renderYTicks = (yTicks: Array<number>, xScale: Scale, yScale: Scale, delta: number) => {
    if (!this.props.displayYTicks) {
      return null;
    }

    const ticks = yTicks.map((tick, index) => {
      const x = xScale.range()[0];
      const y = yScale(tick);
      const innerText = this.props.formatYTick(tick);
      return (
        <text
          className="bubble-chart-tick bubble-chart-tick-y"
          dx="-0.5em"
          dy="0.3em"
          key={index}
          x={x}
          y={y * this.state.zoom - delta}>
          {innerText}
        </text>
      );
    });

    return <g>{ticks}</g>;
  };

  renderChart = (width: number) => {
    const availableWidth = width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.props.height - this.props.padding[0] - this.props.padding[2];

    const xScale = scaleLinear()
      .domain(this.props.xDomain || [0, max(this.props.items, d => d.x) || 0])
      .range([0, availableWidth])
      .nice();
    const yScale = scaleLinear()
      .domain(this.props.yDomain || [0, max(this.props.items, d => d.y) || 0])
      .range([availableHeight, 0])
      .nice();
    const sizeScale = scaleLinear()
      .domain(this.props.sizeDomain || [0, max(this.props.items, d => d.size) || 0])
      .range(this.props.sizeRange || []);

    const xScaleOriginal = xScale.copy();
    const yScaleOriginal = yScale.copy();

    xScale.range(this.getXRange(xScale, sizeScale, availableWidth));
    yScale.range(this.getYRange(yScale, sizeScale, availableHeight));

    const centerXDelta = this.state.zoomOrigin.x * this.state.zoom - this.state.zoomOrigin.x;
    const centerYDelta = this.state.zoomOrigin.y * this.state.zoom - this.state.zoomOrigin.y;

    const bubbles = sortBy(this.props.items, b => -b.size).map((item, index) => {
      return (
        <Bubble
          color={item.color}
          key={item.key || index}
          link={item.link}
          onClick={this.props.onBubbleClick}
          r={sizeScale(item.size)}
          tooltip={item.tooltip}
          x={xScale(item.x) * this.state.zoom - centerXDelta}
          y={yScale(item.y) * this.state.zoom - centerYDelta}
        />
      );
    });

    const xTicks = this.getTicks(xScale, this.props.formatXTick);
    const yTicks = this.getTicks(yScale, this.props.formatYTick);

    return (
      <svg
        className={classNames('bubble-chart', { 'is-moving': this.state.isMoving })}
        height={this.props.height}
        onMouseDown={this.startMoving}
        onWheel={this.onWheel}
        ref={node => (this.node = node)}
        width={width}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          <svg
            height={this.props.height - this.props.padding[0] - this.props.padding[2]}
            style={{ overflow: 'hidden' }}
            width={width}>
            {this.renderXGrid(xTicks, xScale, yScale, centerXDelta)}
            {this.renderYGrid(yTicks, xScale, yScale, centerYDelta)}
            {bubbles}
          </svg>
          {this.renderXTicks(xTicks, xScale, yScaleOriginal, centerXDelta)}
          {this.renderYTicks(yTicks, xScaleOriginal, yScale, centerYDelta)}
        </g>
      </svg>
    );
  };

  render() {
    return (
      <div>
        <div className="bubble-chart-zoom">
          <Tooltip overlay={translate('component_measures.bubble_chart.zoom_level')}>
            <Link
              className={classNames('outline-badge', { active: this.state.zoom > 1 })}
              onClick={this.resetZoom}
              to="#">
              {this.getZoomLevelLabel()}
            </Link>
          </Tooltip>
        </div>
        <AutoSizer disableHeight={true}>{size => this.renderChart(size.width)}</AutoSizer>
      </div>
    );
  }
}
