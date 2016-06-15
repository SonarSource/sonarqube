/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

export default class ProgressBar extends React.Component {
  static propTypes = {
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number,
    count: React.PropTypes.number.isRequired,
    total: React.PropTypes.number.isRequired
  };

  static defaultProps = {
    height: 2
  };

  render () {
    const { width, height } = this.props;
    const p = this.props.total > 0 ? this.props.count / this.props.total : 0;
    const fillWidth = this.props.width * p;

    const commonProps = { x: 0, y: 0, rx: 2, height };

    return (
        <svg width={width} height={height}>
          <rect
              {...commonProps}
              width={width}
              fill="#e6e6e6"/>
          <rect
              {...commonProps}
              width={fillWidth}
              className="bar-chart-bar quality-profile-progress-bar"/>
        </svg>
    );
  }
}
