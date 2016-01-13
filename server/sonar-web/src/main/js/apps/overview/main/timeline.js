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
import d3 from 'd3';
import React from 'react';

import { LineChart } from '../../../components/charts/line-chart';


const HEIGHT = 80;


export class Timeline extends React.Component {
  filterSnapshots () {
    return this.props.history.filter(s => {
      let matchBefore = !this.props.before || s.date <= this.props.before;
      let matchAfter = !this.props.after || s.date >= this.props.after;
      return matchBefore && matchAfter;
    });
  }

  render () {
    let snapshots = this.filterSnapshots();

    if (snapshots.length < 2) {
      return null;
    }

    let data = snapshots.map((snapshot, index) => {
      return { x: index, y: snapshot.value };
    });

    let domain = [0, d3.max(this.props.history, d => d.value)];

    return <LineChart data={data}
                      domain={domain}
                      interpolate="basis"
                      displayBackdrop={true}
                      displayPoints={false}
                      displayVerticalGrid={false}
                      height={HEIGHT}
                      padding={[0, 0, 0, 0]}/>;
  }
}

Timeline.propTypes = {
  history: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  before: React.PropTypes.object,
  after: React.PropTypes.object
};
