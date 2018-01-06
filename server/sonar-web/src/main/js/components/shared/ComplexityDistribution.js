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
import PropTypes from 'prop-types';
import { BarChart } from '../charts/bar-chart';
import { formatMeasure } from '../../helpers/measures';
import { translateWithParameters } from '../../helpers/l10n';

const HEIGHT = 80;

export default class ComplexityDistribution extends React.PureComponent {
  static propTypes = {
    distribution: PropTypes.string.isRequired,
    of: PropTypes.string.isRequired
  };

  renderBarChart = () => {
    const data = this.props.distribution.split(';').map((point, index) => {
      const tokens = point.split('=');
      const y = parseInt(tokens[1], 10);
      const value = parseInt(tokens[0], 10);
      return {
        x: index,
        y,
        value,
        tooltip: translateWithParameters(`overview.complexity_tooltip.${this.props.of}`, y, value)
      };
    });

    const xTicks = data.map(point => point.value);

    const xValues = data.map(point => formatMeasure(point.y, 'INT'));

    return (
      <BarChart
        data={data}
        xTicks={xTicks}
        xValues={xValues}
        height={HEIGHT}
        barsWidth={20}
        padding={[25, 10, 25, 10]}
      />
    );
  };

  render() {
    return (
      <div className="overview-bar-chart" style={{ height: HEIGHT }}>
        {this.renderBarChart()}
      </div>
    );
  }
}
