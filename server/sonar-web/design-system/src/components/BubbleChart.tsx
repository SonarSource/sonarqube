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
import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import { max, min } from 'd3-array';
import { ScaleLinear, scaleLinear } from 'd3-scale';
import { select } from 'd3-selection';
import { D3ZoomEvent, ZoomBehavior, zoom, zoomIdentity } from 'd3-zoom';
import { sortBy, uniq } from 'lodash';
import * as React from 'react';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers';
import { ButtonSecondary } from '../sonar-aligned/components/buttons';
import { Note } from '../sonar-aligned/components/typography';
import { BubbleColorVal } from '../types/charts';
import { Tooltip } from './Tooltip';

const TICKS_COUNT = 5;

interface BubbleItem<T> {
  color?: BubbleColorVal;
  data?: T;
  key?: string;
  size: number;
  tooltip?: React.ReactNode;
  x: number;
  y: number;
}

export interface BubbleChartProps<T> {
  'data-testid'?: string;
  displayXGrid?: boolean;
  displayXTicks?: boolean;
  displayYGrid?: boolean;
  displayYTicks?: boolean;
  formatXTick: (tick: number) => string;
  formatYTick: (tick: number) => string;
  height: number;
  items: Array<BubbleItem<T>>;
  onBubbleClick?: (ref?: T) => void;
  padding: [number, number, number, number];
  sizeDomain?: [number, number];
  sizeRange?: [number, number];
  xDomain?: [number, number];
  yDomain?: [number, number];
  zoomLabel?: string;
  zoomResetLabel?: string;
  zoomTooltipText?: string;
}

type Scale = ScaleLinear<number, number>;

BubbleChart.defaultProps = {
  displayXGrid: true,
  displayXTicks: true,
  displayYGrid: true,
  displayYTicks: true,
  formatXTick: (d: number) => String(d),
  formatYTick: (d: number) => String(d),
  padding: [10, 10, 10, 10],
  sizeRange: [5, 45],
};

export function BubbleChart<T>(props: BubbleChartProps<T>) {
  const {
    padding,
    height,
    items,
    xDomain,
    yDomain,
    sizeDomain,
    sizeRange,
    zoomResetLabel = 'Reset',
    zoomTooltipText,
    zoomLabel = 'Zoom',
    displayXTicks,
    displayYTicks,
    displayXGrid,
    displayYGrid,
    formatXTick,
    formatYTick,
  } = props;
  const [transform, setTransform] = React.useState({ x: 0, y: 0, k: 1 });
  const nodeRef = React.useRef<SVGSVGElement>();
  const zoomRef = React.useRef<ZoomBehavior<Element, unknown>>();
  const zoomLevelLabel = `${Math.floor(transform.k * 100)}%`;

  if (zoomRef.current && nodeRef.current) {
    const rect = nodeRef.current.getBoundingClientRect();
    zoomRef.current.translateExtent([
      [0, 0],
      [rect.width, rect.height],
    ]);
  }

  const zoomed = React.useCallback(
    (event: D3ZoomEvent<SVGSVGElement, void>) => {
      const { x, y, k } = event.transform;
      setTransform({
        x: x + padding[3] * (k - 1),
        y: y + padding[0] * (k - 1),
        k,
      });
    },
    [padding],
  );

  const boundNode = React.useCallback(
    (node: SVGSVGElement) => {
      nodeRef.current = node;
      zoomRef.current = zoom().scaleExtent([1, 10]).on('zoom', zoomed);
      // @ts-expect-error Type instantiation is excessively deep and possibly infinite.
      select(nodeRef.current).call(zoomRef.current);
    },
    [zoomed],
  );

  const resetZoom = React.useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    e.preventDefault();
    if (zoomRef.current && nodeRef.current) {
      select(nodeRef.current).call(zoomRef.current.transform, zoomIdentity);
    }
  }, []);

  const getXRange = React.useCallback(
    (xScale: Scale, sizeScale: Scale, availableWidth: number) => {
      const [x1, x2] = xScale.range();
      const minX = min(items, (d) => xScale(d.x) - sizeScale(d.size)) ?? 0;
      const maxX = max(items, (d) => xScale(d.x) + sizeScale(d.size)) ?? 0;
      const dMinX = minX < 0 ? x1 - minX : x1;
      const dMaxX = maxX > x2 ? maxX - x2 : 0;
      return [dMinX, availableWidth - dMaxX];
    },
    [items],
  );

  const getYRange = React.useCallback(
    (yScale: Scale, sizeScale: Scale, availableHeight: number) => {
      const [y1, y2] = yScale.range();
      const minY = min(items, (d) => yScale(d.y) - sizeScale(d.size)) ?? 0;
      const maxY = max(items, (d) => yScale(d.y) + sizeScale(d.size)) ?? 0;
      const dMinY = minY < 0 ? y2 - minY : y2;
      const dMaxY = maxY > y1 ? maxY - y1 : 0;
      return [availableHeight - dMaxY, dMinY];
    },
    [items],
  );

  const getTicks = React.useCallback(
    (scale: Scale, format: (d: number) => string) => {
      const zoomAmount = Math.ceil(transform.k);
      const ticks = scale.ticks(TICKS_COUNT * zoomAmount).map((tick) => format(tick));
      const uniqueTicksCount = uniq(ticks).length;
      const ticksCount =
        uniqueTicksCount < TICKS_COUNT * zoomAmount
          ? uniqueTicksCount - 1
          : TICKS_COUNT * zoomAmount;
      return scale.ticks(ticksCount);
    },
    [transform],
  );

  const renderXGrid = React.useCallback(
    (ticks: number[], xScale: Scale, yScale: Scale) => {
      if (!displayXGrid) {
        return null;
      }

      const lines = ticks.map((tick, index) => {
        const x = xScale(tick);
        const [y1, y2] = yScale.range();
        return (
          <BubbleChartGrid
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            x1={x * transform.k + transform.x}
            x2={x * transform.k + transform.x}
            y1={y1 * transform.k}
            y2={transform.k > 1 ? 0 : y2}
          />
        );
      });

      return <g>{lines}</g>;
    },
    [transform, displayXGrid],
  );

  const renderYGrid = React.useCallback(
    (ticks: number[], xScale: Scale, yScale: Scale) => {
      if (!displayYGrid) {
        return null;
      }

      const lines = ticks.map((tick, index) => {
        const y = yScale(tick);
        const [x1, x2] = xScale.range();
        return (
          <BubbleChartGrid
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            x1={transform.k > 1 ? 0 : x1}
            x2={x2 * transform.k}
            y1={y * transform.k + transform.y}
            y2={y * transform.k + transform.y}
          />
        );
      });

      return <g>{lines}</g>;
    },
    [displayYGrid, transform],
  );

  const renderXTicks = React.useCallback(
    (xTicks: number[], xScale: Scale, yScale: Scale) => {
      if (!displayXTicks) {
        return null;
      }

      const ticks = xTicks.map((tick, index) => {
        const x = xScale(tick) * transform.k + transform.x;
        const y = yScale.range()[0];
        const innerText = formatXTick(tick);
        // as we modified the `x` using `transform`, check that it is inside the range again
        return x > 0 && x < xScale.range()[1] ? (
          // eslint-disable-next-line react/no-array-index-key
          <BubbleChartTick dy="1.5em" key={index} style={{ '--align': 'middle' }} x={x} y={y}>
            {innerText}
          </BubbleChartTick>
        ) : null;
      });

      return <g>{ticks}</g>;
    },
    [displayXTicks, formatXTick, transform],
  );

  const renderYTicks = React.useCallback(
    (yTicks: number[], xScale: Scale, yScale: Scale) => {
      if (!displayYTicks) {
        return null;
      }

      const ticks = yTicks.map((tick, index) => {
        const x = xScale.range()[0];
        const y = yScale(tick) * transform.k + transform.y;
        const innerText = formatYTick(tick);
        // as we modified the `y` using `transform`, check that it is inside the range again
        return y > 0 && y < yScale.range()[0] ? (
          <BubbleChartTick
            dx="-0.5em"
            dy="0.3em"
            // eslint-disable-next-line react/no-array-index-key
            key={index}
            style={{ '--align': 'end' }}
            x={x}
            y={y}
          >
            {innerText}
          </BubbleChartTick>
        ) : null;
      });

      return <g>{ticks}</g>;
    },
    [displayYTicks, formatYTick, transform],
  );

  const renderChart = (width: number) => {
    const availableWidth = width - padding[1] - padding[3];
    const availableHeight = height - padding[0] - padding[2];

    const xScale = scaleLinear()
      .domain(xDomain ?? [0, max(items, (d) => d.x) ?? 0])
      .range([0, availableWidth])
      .nice();
    const yScale = scaleLinear()
      .domain(yDomain ?? [0, max(items, (d) => d.y) ?? 0])
      .range([availableHeight, 0])
      .nice();
    const sizeScale = scaleLinear()
      .domain(sizeDomain ?? [0, max(items, (d) => d.size) ?? 0])
      .range(sizeRange ?? []);

    const xScaleOriginal = xScale.copy();
    const yScaleOriginal = yScale.copy();

    xScale.range(getXRange(xScale, sizeScale, availableWidth));
    yScale.range(getYRange(yScale, sizeScale, availableHeight));

    const bubbles = sortBy(items, (b) => -b.size).map((item, index) => {
      return (
        <Bubble
          color={item.color}
          data={item.data}
          key={item.key ?? index}
          onClick={props.onBubbleClick}
          r={sizeScale(item.size)}
          scale={1 / transform.k}
          tooltip={item.tooltip}
          x={xScale(item.x)}
          y={yScale(item.y)}
        />
      );
    });

    const xTicks = getTicks(xScale, props.formatXTick);
    const yTicks = getTicks(yScale, props.formatYTick);

    return (
      <svg
        className={classNames('bubble-chart')}
        data-testid={props['data-testid']}
        height={height}
        ref={boundNode}
        width={width}
      >
        <defs>
          <clipPath id="graph-region">
            <rect
              // Extend clip by 2 pixels: one for clipRect border, and one for Bubble borders
              height={availableHeight + 4}
              width={availableWidth + 4}
              x={-2}
              y={-2}
            />
          </clipPath>
        </defs>
        <g transform={`translate(${padding[3]}, ${padding[0]})`}>
          <g clipPath="url(#graph-region)">
            {renderXGrid(xTicks, xScale, yScale)}
            {renderYGrid(yTicks, xScale, yScale)}
            <g transform={`translate(${transform.x}, ${transform.y}) scale(${transform.k})`}>
              {bubbles}
            </g>
          </g>
          {renderXTicks(xTicks, xScale, yScaleOriginal)}
          {renderYTicks(yTicks, xScaleOriginal, yScale)}
        </g>
      </svg>
    );
  };

  return (
    <div>
      <div className="sw-flex sw-items-center sw-justify-end sw-h-control sw-mb-4">
        <Tooltip content={zoomTooltipText}>
          <span>
            <Note className="sw-body-sm-highlight">{zoomLabel}</Note>
            {': '}
            {zoomLevelLabel}
          </span>
        </Tooltip>
        {zoomLevelLabel !== '100%' && (
          <ButtonSecondary
            className="sw-ml-2"
            disabled={zoomLevelLabel === '100%'}
            onClick={resetZoom}
          >
            {zoomResetLabel}
          </ButtonSecondary>
        )}
      </div>
      <AutoSizer disableHeight>{(size) => renderChart(size.width)}</AutoSizer>
    </div>
  );
}

interface BubbleProps<T> {
  color?: BubbleColorVal;
  data?: T;
  onClick?: (ref?: T) => void;
  r: number;
  scale: number;
  tooltip?: string | React.ReactNode;
  x: number;
  y: number;
}

function Bubble<T>(props: BubbleProps<T>) {
  const theme = useTheme();
  const { color, data, onClick, r, scale, tooltip, x, y } = props;
  const handleClick = React.useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      event.stopPropagation();
      event.preventDefault();
      onClick?.(data);
    },
    [data, onClick],
  );

  const circle = (
    <a href="#" onClick={handleClick}>
      <BubbleStyled
        r={r}
        style={{
          fill: color ? themeColor(`bubble.${color}`)({ theme }) : '',
          stroke: color ? themeContrast(`bubble.${color}`)({ theme }) : '',
        }}
        transform={`translate(${x}, ${y}) scale(${scale})`}
      />
    </a>
  );

  return <Tooltip content={tooltip}>{circle}</Tooltip>;
}

const BubbleStyled = styled.circle`
  ${tw`sw-cursor-pointer`}

  transition: fill-opacity 0.2s ease;
  fill: ${themeColor('bubbleDefault')};
  stroke: ${themeContrast('bubbleDefault')};

  &:hover {
    fill-opacity: 0.8;
  }
`;

const BubbleChartGrid = styled.line`
  shape-rendering: crispedges;
  stroke: ${themeColor('bubbleChartLine')};
`;

const BubbleChartTick = styled.text`
  ${tw`sw-body-sm`}
  ${tw`sw-select-none`}
  fill: ${themeColor('pageContentLight')};
  text-anchor: var(--align);
`;
