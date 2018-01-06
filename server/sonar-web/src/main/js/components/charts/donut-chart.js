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
import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { arc as d3Arc, pie as d3Pie } from 'd3-shape';
import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';

function Sector(props) {
  const arc = d3Arc()
    .outerRadius(props.radius)
    .innerRadius(props.radius - props.thickness);
  return <path d={arc(props.data)} style={{ fill: props.fill }} />;
}

export const DonutChart = createReactClass({
  displayName: 'DonutChart',

  propTypes: {
    data: PropTypes.arrayOf(PropTypes.object).isRequired
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return { thickness: 6, padding: [0, 0, 0, 0] };
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  render() {
    if (!this.state.width || !this.state.height) {
      return <div />;
    }

    const availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    const availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    const size = Math.min(availableWidth, availableHeight);
    const radius = Math.floor(size / 2);

    const pie = d3Pie()
      .sort(null)
      .value(d => d.value);
    const sectors = pie(this.props.data).map((d, i) => {
      return (
        <Sector
          key={i}
          data={d}
          radius={radius}
          fill={this.props.data[i].fill}
          thickness={this.props.thickness}
        />
      );
    });

    return (
      <svg className="donut-chart" width={this.state.width} height={this.state.height}>
        <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
          <g transform={`translate(${radius}, ${radius})`}>{sectors}</g>
        </g>
      </svg>
    );
  }
});
