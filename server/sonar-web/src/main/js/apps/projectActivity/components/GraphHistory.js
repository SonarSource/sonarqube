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
import AutoSizer from 'react-virtualized/dist/commonjs/AutoSizer';
import GraphsTooltips from './GraphsTooltips';
import GraphsLegendCustom from './GraphsLegendCustom';
import GraphsLegendStatic from './GraphsLegendStatic';
import AdvancedTimeline from '../../../components/charts/AdvancedTimeline';
import { formatMeasure, getShortType } from '../../../helpers/measures';
/*:: import type { Event, MeasureHistory } from '../types'; */
/*:: import type { Serie } from '../../../components/charts/AdvancedTimeline'; */

/*::
type Props = {
  events: Array<Event>,
  graph: string,
  graphEndDate: ?Date,
  graphStartDate: ?Date,
  leakPeriodDate: Date,
  isCustom: boolean,
  measuresHistory: Array<MeasureHistory>,
  metricsType: string,
  removeCustomMetric: (metric: string) => void,
  showAreas: boolean,
  series: Array<Serie>,
  selectedDate?: ?Date,
  updateGraphZoom: (from: ?Date, to: ?Date) => void,
  updateSelectedDate: (selectedDate: ?Date) => void,
  updateTooltip: (selectedDate: ?Date) => void
};
*/

/*::
type State = {
  tooltipIdx: ?number,
  tooltipXPos: ?number
};
*/

export default class GraphHistory extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    tooltipIdx: null,
    tooltipXPos: null
  };

  formatValue = (tick /*: string | number */) =>
    formatMeasure(tick, getShortType(this.props.metricsType));

  formatTooltipValue = (tick /*: string | number */) => formatMeasure(tick, this.props.metricsType);

  updateTooltip = (
    selectedDate /*: ?Date */,
    tooltipXPos /*: ?number */,
    tooltipIdx /*: ?number */
  ) => {
    this.props.updateTooltip(selectedDate);
    this.setState({ tooltipXPos, tooltipIdx });
  };

  render() {
    const { graph, selectedDate, series } = this.props;
    const { tooltipIdx, tooltipXPos } = this.state;

    return (
      <div className="project-activity-graph-container">
        {this.props.isCustom ? (
          <GraphsLegendCustom series={series} removeMetric={this.props.removeCustomMetric} />
        ) : (
          <GraphsLegendStatic series={series} />
        )}
        <div className="project-activity-graph">
          <AutoSizer>
            {({ height, width }) => (
              <div>
                <AdvancedTimeline
                  endDate={this.props.graphEndDate}
                  height={height}
                  width={width}
                  interpolate="linear"
                  formatYTick={this.formatValue}
                  leakPeriodDate={this.props.leakPeriodDate}
                  metricType={this.props.metricsType}
                  selectedDate={selectedDate}
                  series={series}
                  showAreas={this.props.showAreas}
                  startDate={this.props.graphStartDate}
                  updateSelectedDate={this.props.updateSelectedDate}
                  updateTooltip={this.updateTooltip}
                  updateZoom={this.props.updateGraphZoom}
                />
                {selectedDate != null &&
                  tooltipXPos != null && (
                    <GraphsTooltips
                      events={this.props.events}
                      formatValue={this.formatTooltipValue}
                      graph={graph}
                      graphWidth={width}
                      measuresHistory={this.props.measuresHistory}
                      selectedDate={selectedDate}
                      series={series}
                      tooltipIdx={tooltipIdx}
                      tooltipPos={tooltipXPos}
                    />
                  )}
              </div>
            )}
          </AutoSizer>
        </div>
      </div>
    );
  }
}
