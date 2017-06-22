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
import { some } from 'lodash';
import { AutoSizer } from 'react-virtualized';
import ZoomTimeLine from '../../../components/charts/ZoomTimeLine';
import type { Serie } from '../../../components/charts/AdvancedTimeline';

type Props = {
  graphEndDate: ?Date,
  graphStartDate: ?Date,
  leakPeriodDate: Date,
  loading: boolean,
  metricsType: string,
  series: Array<Serie>,
  showAreas?: boolean,
  updateGraphZoom: (from: ?Date, to: ?Date) => void
};

export default class GraphsZoom extends React.PureComponent {
  props: Props;

  hasHistoryData = () => some(this.props.series, serie => serie.data && serie.data.length > 2);

  render() {
    const { loading } = this.props;
    if (loading || !this.hasHistoryData()) {
      return null;
    }

    return (
      <div className="project-activity-graph-zoom">
        <AutoSizer disableHeight={true}>
          {({ width }) => (
            <ZoomTimeLine
              endDate={this.props.graphEndDate}
              height={64}
              width={width}
              interpolate="linear"
              leakPeriodDate={this.props.leakPeriodDate}
              metricType={this.props.metricsType}
              padding={[0, 10, 18, 60]}
              series={this.props.series}
              showAreas={this.props.showAreas}
              startDate={this.props.graphStartDate}
              updateZoom={this.props.updateGraphZoom}
            />
          )}
        </AutoSizer>
      </div>
    );
  }
}
