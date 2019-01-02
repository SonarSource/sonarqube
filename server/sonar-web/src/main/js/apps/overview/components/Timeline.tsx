/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { max } from 'd3-array';
import LineChart from '../../../components/charts/LineChart';

const HEIGHT = 80;

interface Props {
  history: Array<{ date: Date; value?: string }>;
  before?: Date;
  after?: Date;
}

export default class Timeline extends React.PureComponent<Props> {
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
      return { x: index, y: snapshot.value !== undefined ? Number(snapshot.value) : undefined };
    });
    const domain = [
      0,
      max(this.props.history, d => (d.value !== undefined ? parseFloat(d.value) : 0))
    ] as [number, number];
    return (
      <LineChart
        data={data}
        displayBackdrop={true}
        displayPoints={false}
        displayVerticalGrid={false}
        domain={domain}
        height={HEIGHT}
        padding={[0, 0, 0, 0]}
      />
    );
  }
}
