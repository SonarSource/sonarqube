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
import { treemap as d3Treemap, hierarchy as d3Hierarchy } from 'd3-hierarchy';
import TreeMapRect from './TreeMapRect';
import { translate } from '../../helpers/l10n';

/*:: export type TreeMapItem = {
  key: string,
  size: number,
  color: string,
  icon?: React.Element<*>,
  tooltip?: string | React.Element<*>,
  label: string,
  link?: string
}; */

/*:: type Props = {|
  items: Array<TreeMapItem>,
  onRectangleClick?: string => void,
  height: number,
  width: number
|}; */

export default class TreeMap extends React.PureComponent {
  /*:: props: Props; */

  mostCommitPrefix = (labels /*: Array<string> */) => {
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

  renderNoData() {
    return (
      <div className="sonar-d3">
        <div
          className="treemap-container"
          style={{ width: this.props.width, height: this.props.height }}>
          {translate('no_data')}
        </div>
      </div>
    );
  }

  render() {
    const { items, height, width } = this.props;
    if (items.length <= 0) {
      return this.renderNoData();
    }

    const hierarchy = d3Hierarchy({ children: items })
      .sum(d => d.size)
      .sort((a, b) => b.value - a.value);

    const treemap = d3Treemap()
      .round(true)
      .size([width, height]);

    const nodes = treemap(hierarchy).leaves();
    const prefix = this.mostCommitPrefix(items.map(item => item.label));
    const halfWidth = width / 2;
    return (
      <div className="sonar-d3">
        <div className="treemap-container" style={{ width, height }}>
          {nodes.map(node => (
            <TreeMapRect
              key={node.data.key}
              x={node.x0}
              y={node.y0}
              width={node.x1 - node.x0}
              height={node.y1 - node.y0}
              fill={node.data.color}
              label={node.data.label}
              prefix={prefix}
              itemKey={node.data.key}
              icon={node.data.icon}
              tooltip={node.data.tooltip}
              link={node.data.link}
              onClick={this.props.onRectangleClick}
              placement={node.x0 === 0 || node.x1 < halfWidth ? 'right' : 'left'}
            />
          ))}
        </div>
      </div>
    );
  }
}
