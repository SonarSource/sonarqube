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
import { max } from 'd3-array';
import { LineChart } from '../../../components/charts/line-chart';

const HEIGHT = 80;

export default class Timeline extends React.PureComponent {
  static propTypes = {
    history: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    before: React.PropTypes.object,
    after: React.PropTypes.object
  };

  filterSnapshots() {
    const { history, before, after } = this.props;

    return history.filter(s => {
      const matchBefore = !before || s.date <= before;
      const matchAfter = !after || s.date >= after;
      return matchBefore && matchAfter;
    });
  }

  render() {
    const snapshots = this.filterSnapshots();

    if (snapshots.length < 2) {
      return null;
    }

    const data = snapshots.map((snapshot, index) => {
      return { x: index, y: snapshot.value };
    });
    const domain = [0, max(this.props.history, d => parseFloat(d.value))];
    return (
      <LineChart
        data={data}
        domain={domain}
        interpolate="basis"
        displayBackdrop={true}
        displayPoints={false}
        displayVerticalGrid={false}
        height={HEIGHT}
        padding={[0, 0, 0, 0]}
      />
    );
  }
}
