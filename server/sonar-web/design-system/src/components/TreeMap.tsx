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
import { hierarchy as d3Hierarchy, treemap as d3Treemap } from 'd3-hierarchy';
import { sortBy } from 'lodash';
import tw from 'twin.macro';
import { PopupPlacement } from '../helpers/positioning';
import { TreeMapRect } from './TreeMapRect';

export interface TreeMapItem<T> {
  color?: string;
  gradient?: string;
  icon?: React.ReactNode;
  key: string;
  label: string;
  size: number;
  sourceData?: T;
  tooltip?: React.ReactNode;
}

interface HierarchicalTreemapItem<T> extends TreeMapItem<T> {
  children?: Array<TreeMapItem<T>>;
}

export interface TreeMapProps<T> {
  height: number;
  items: Array<TreeMapItem<T>>;
  onRectangleClick?: (item: TreeMapItem<T>) => void;
  width: number;
}

export function TreeMap<T = unknown>(props: TreeMapProps<T>) {
  function mostCommitPrefix(labels: string[]) {
    const sortedLabels = sortBy(labels.slice(0));
    const firstLabel = sortedLabels[0];
    const firstLabelLength = firstLabel.length;
    const lastLabel = sortedLabels[sortedLabels.length - 1];
    let i = 0;
    while (i < firstLabelLength && firstLabel.charAt(i) === lastLabel.charAt(i)) {
      i++;
    }
    const prefix = firstLabel.substring(0, i);
    const prefixTokens = prefix.split(/[\s\\/]/);
    const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
    return prefix.substring(0, prefix.length - lastPrefixPart.length);
  }

  function handleClick(data: TreeMapItem<T>) {
    if (props.onRectangleClick) {
      props.onRectangleClick(data);
    }
  }

  const { items, height, width } = props;
  const hierarchy = d3Hierarchy({ children: items } as HierarchicalTreemapItem<T>)
    .sum((d) => d.size)
    .sort((a, b) => (b.value ?? 0) - (a.value ?? 0));

  const treemap = d3Treemap<TreeMapItem<T>>().round(true).size([width, height]);

  const nodes = treemap(hierarchy).leaves();
  const prefix = mostCommitPrefix(items.map((item) => item.label));
  const halfWidth = width / 2;
  return (
    <StyledContainer style={{ width, height }}>
      {nodes.map(({ data, y0, y1, x0, x1 }) => (
        <TreeMapRect
          fill={data.color}
          gradient={data.gradient}
          height={y1 - y0}
          icon={data.icon}
          itemKey={data.key}
          key={data.key}
          label={data.label}
          onClick={() => {
            handleClick(data);
          }}
          placement={x0 === 0 || x1 < halfWidth ? PopupPlacement.Right : PopupPlacement.Left}
          prefix={prefix}
          tooltip={data.tooltip}
          width={x1 - x0}
          x={x0}
          y={y0}
        />
      ))}
    </StyledContainer>
  );
}

const StyledContainer = styled.ul`
  ${tw`sw-relative`}
`;
