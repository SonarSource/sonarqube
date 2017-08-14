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
// @flow
import React from 'react';
import { DonutChart } from '../charts/donut-chart';

const SIZE_TO_WIDTH_MAPPING = {
  small: 16,
  normal: 24,
  big: 40,
  huge: 60
};

const SIZE_TO_THICKNESS_MAPPING = {
  small: 2,
  normal: 3,
  big: 3,
  huge: 4
};

export default class CoverageRating extends React.PureComponent {
  /*:: props: {
    value: number | string,
    size?: 'small' | 'normal' | 'big' | 'huge',
    muted?: boolean
  };
*/

  static defaultProps = {
    size: 'normal',
    muted: false
  };

  render() {
    let data = [{ value: 100, fill: '#ccc ' }];

    if (this.props.value != null) {
      const value = Number(this.props.value);
      data = [
        { value, fill: this.props.muted ? '#bdbdbd' : '#00aa00' },
        { value: 100 - value, fill: this.props.muted ? '#f3f3f3' : '#d4333f' }
      ];
    }

    // $FlowFixMe
    const size = SIZE_TO_WIDTH_MAPPING[this.props.size];

    // $FlowFixMe
    const thickness = SIZE_TO_THICKNESS_MAPPING[this.props.size];

    return <DonutChart data={data} width={size} height={size} thickness={thickness} />;
  }
}
