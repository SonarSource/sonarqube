/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { hierarchy as d3Hierarchy, treemap as d3Treemap } from 'd3-hierarchy';
import * as React from 'react';
import { formatMeasure, localizeMetric } from '../../helpers/measures';
import { Location } from '../../helpers/urls';
import './TreeMap.css';
import TreeMapRect from './TreeMapRect';

export interface TreeMapItem {
  color?: string;
  gradient?: string;
  icon?: React.ReactNode;
  key: string;
  label: string;
  link?: string | Location;
  measureValue?: string;
  metric?: { key: string; type: string };
  size: number;
  tooltip?: React.ReactNode;
}

interface HierarchicalTreemapItem extends TreeMapItem {
  children?: TreeMapItem[];
}

interface Props {
  height: number;
  items: TreeMapItem[];
  onRectangleClick?: (item: string) => void;
  width: number;
}

export default class TreeMap extends React.PureComponent<Props> {
  mostCommitPrefix = (labels: string[]) => {
    const sortedLabels = labels.slice(0).sort();
    const firstLabel = sortedLabels[0];
    const firstLabelLength = firstLabel.length;
    const lastLabel = sortedLabels[sortedLabels.length - 1];
    let i = 0;
    while (i < firstLabelLength && firstLabel.charAt(i) === lastLabel.charAt(i)) {
      i++;
    }
    const prefix = firstLabel.substr(0, i);
    const prefixTokens = prefix.split(/[\s\\/]/);
    const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
    return prefix.substr(0, prefix.length - lastPrefixPart.length);
  };

  render() {
    const { items, height, width } = this.props;
    const hierarchy = d3Hierarchy({ children: items } as HierarchicalTreemapItem)
      .sum((d) => d.size)
      .sort((a, b) => (b.value || 0) - (a.value || 0));

    const treemap = d3Treemap<TreeMapItem>().round(true).size([width, height]);

    const nodes = treemap(hierarchy).leaves();
    const prefix = this.mostCommitPrefix(items.map((item) => item.label));
    const halfWidth = width / 2;
    return (
      <div className="sonar-d3">
        <div className="treemap-container" style={{ width, height }}>
          {nodes.map((node) => (
            <TreeMapRect
              fill={node.data.color}
              gradient={node.data.gradient}
              height={node.y1 - node.y0}
              icon={node.data.icon}
              itemKey={node.data.key}
              key={node.data.key}
              label={node.data.label}
              link={node.data.link}
              onClick={this.props.onRectangleClick}
              placement={node.x0 === 0 || node.x1 < halfWidth ? 'right' : 'left'}
              prefix={prefix}
              value={
                node.data.metric && (
                  <>
                    {formatMeasure(node.data.measureValue, node.data.metric.type)}
                    <span className="little-spacer-left">
                      {localizeMetric(node.data.metric.key)}
                    </span>
                  </>
                )
              }
              tooltip={node.data.tooltip}
              width={node.x1 - node.x0}
              x={node.x0}
              y={node.y0}
            />
          ))}
        </div>
      </div>
    );
  }
}
