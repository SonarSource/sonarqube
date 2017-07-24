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
import React from 'react';
import PropTypes from 'prop-types';
import { scaleLinear } from 'd3-scale';
import { treemap as d3Treemap, hierarchy as d3Hierarchy } from 'd3-hierarchy';
import { TreemapBreadcrumbs } from './treemap-breadcrumbs';
import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';
import { translate } from '../../helpers/l10n';

const SIZE_SCALE = scaleLinear().domain([3, 15]).range([11, 18]).clamp(true);

function mostCommitPrefix(strings) {
  const sortedStrings = strings.slice(0).sort();
  const firstString = sortedStrings[0];
  const firstStringLength = firstString.length;
  const lastString = sortedStrings[sortedStrings.length - 1];
  let i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  const prefix = firstString.substr(0, i);
  const prefixTokens = prefix.split(/[\s\\\/]/);
  const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
  return prefix.substr(0, prefix.length - lastPrefixPart.length);
}

export const TreemapRect = React.createClass({
  propTypes: {
    x: PropTypes.number.isRequired,
    y: PropTypes.number.isRequired,
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    fill: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onClick: PropTypes.func
  },

  renderLink() {
    if (!this.props.link) {
      return null;
    }

    if (this.props.width < 24 || this.props.height < 24) {
      return null;
    }

    return (
      <a
        onClick={e => e.stopPropagation()}
        className="treemap-link"
        href={this.props.link}
        style={{ fontSize: 12 }}>
        <span className="icon-link" />
      </a>
    );
  },

  render() {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'data-title': this.props.tooltip
      };
    }
    const cellStyles = {
      left: this.props.x,
      top: this.props.y,
      width: this.props.width,
      height: this.props.height,
      backgroundColor: this.props.fill,
      fontSize: SIZE_SCALE(this.props.width / this.props.label.length),
      lineHeight: `${this.props.height}px`,
      cursor: typeof this.props.onClick === 'function' ? 'pointer' : 'default'
    };
    const isTextVisible = this.props.width >= 40 && this.props.height >= 40;
    /* eslint-disable jsx-a11y/onclick-has-focus, jsx-a11y/onclick-has-role */
    return (
      <div
        className="treemap-cell"
        {...tooltipAttrs}
        style={cellStyles}
        onClick={this.props.onClick}>
        <div
          className="treemap-inner"
          dangerouslySetInnerHTML={{ __html: this.props.label }}
          style={{ maxWidth: this.props.width, visibility: isTextVisible ? 'visible' : 'hidden' }}
        />
        {this.renderLink()}
      </div>
    );
  }
});

export const Treemap = React.createClass({
  propTypes: {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    height: PropTypes.number,
    onRectangleClick: PropTypes.func
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  renderWhenNoData() {
    return (
      <div className="sonar-d3">
        <div
          className="treemap-container"
          style={{ width: this.state.width, height: this.state.height }}>
          {translate('no_data')}
        </div>
        <TreemapBreadcrumbs {...this.props} />
      </div>
    );
  },

  render() {
    if (!this.state.width || !this.state.height) {
      return <div>&nbsp;</div>;
    }

    if (!this.props.items.length) {
      return this.renderWhenNoData();
    }

    const hierarchy = d3Hierarchy({ children: this.props.items })
      .sum(d => d.size)
      .sort((a, b) => b.value - a.value);

    const treemap = d3Treemap().round(true).size([this.state.width, this.state.height]);

    const nodes = treemap(hierarchy).leaves();

    const prefix = mostCommitPrefix(this.props.items.map(item => item.label));
    const prefixLength = prefix.length;

    const rectangles = nodes.map(node => {
      const key = node.data.label;
      const label = prefixLength
        ? `${prefix}<br>${node.data.label.substr(prefixLength)}`
        : node.data.label;
      const onClick = this.props.canBeClicked(node.data)
        ? () => this.props.onRectangleClick(node.data)
        : null;
      return (
        <TreemapRect
          key={key}
          x={node.x0}
          y={node.y0}
          width={node.x1 - node.x0}
          height={node.y1 - node.y0}
          fill={node.data.color}
          label={label}
          prefix={prefix}
          tooltip={node.data.tooltip}
          link={node.data.link}
          onClick={onClick}
        />
      );
    });

    return (
      <div className="sonar-d3">
        <div
          className="treemap-container"
          style={{ width: this.state.width, height: this.state.height }}>
          {rectangles}
        </div>
        <TreemapBreadcrumbs {...this.props} />
      </div>
    );
  }
});
