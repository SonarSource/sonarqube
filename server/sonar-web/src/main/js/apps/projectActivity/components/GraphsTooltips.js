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
import BubblePopup from '../../../components/common/BubblePopup';
import FormattedDate from '../../../components/ui/FormattedDate';
import GraphsTooltipsContent from './GraphsTooltipsContent';
import GraphsTooltipsContentEvents from './GraphsTooltipsContentEvents';
import GraphsTooltipsContentCoverage from './GraphsTooltipsContentCoverage';
import GraphsTooltipsContentDuplication from './GraphsTooltipsContentDuplication';
import GraphsTooltipsContentOverview from './GraphsTooltipsContentOverview';
import type { Event, MeasureHistory, Metric } from '../types';
import type { Serie } from '../../../components/charts/AdvancedTimeline';

type Props = {
  events: Array<Event>,
  formatValue: (number | string) => string,
  graph: string,
  graphWidth: number,
  measuresHistory: Array<MeasureHistory>,
  metrics: Array<Metric>,
  selectedDate: Date,
  series: Array<Serie & { translatedName: string }>,
  tooltipIdx: number,
  tooltipPos: number
};

const TOOLTIP_WIDTH = 250;

export default class GraphsTooltips extends React.PureComponent {
  props: Props;

  render() {
    const { events, measuresHistory, tooltipIdx } = this.props;
    const top = 50;
    let left = this.props.tooltipPos + 60;
    let customClass;
    if (left > this.props.graphWidth - TOOLTIP_WIDTH - 50) {
      left -= TOOLTIP_WIDTH;
      customClass = 'bubble-popup-right';
    }
    return (
      <BubblePopup customClass={customClass} position={{ top, left, width: TOOLTIP_WIDTH }}>
        <div className="project-activity-graph-tooltip">
          <div className="project-activity-graph-tooltip-title spacer-bottom">
            <FormattedDate date={this.props.selectedDate} format="LL" />
          </div>
          <table className="width-100">
            <tbody>
              {this.props.series.map(serie => {
                const point = serie.data[tooltipIdx];
                if (!point || (!point.y && point.y !== 0)) {
                  return null;
                }
                if (this.props.graph === 'overview') {
                  return (
                    <GraphsTooltipsContentOverview
                      key={serie.name}
                      measuresHistory={measuresHistory}
                      serie={serie}
                      tooltipIdx={tooltipIdx}
                      value={this.props.formatValue(point.y)}
                    />
                  );
                } else {
                  const metric = this.props.metrics.find(metric => metric.key === serie.name);
                  return (
                    <GraphsTooltipsContent
                      key={serie.name}
                      serie={serie}
                      translatedName={metric && metric.custom ? metric.name : serie.translatedName}
                      value={this.props.formatValue(point.y)}
                    />
                  );
                }
              })}
            </tbody>
            {this.props.graph === 'coverage' &&
              <GraphsTooltipsContentCoverage
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />}
            {this.props.graph === 'duplications' &&
              <GraphsTooltipsContentDuplication
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />}
            {events && events.length > 0 && <GraphsTooltipsContentEvents events={events} />}
          </table>
        </div>
      </BubblePopup>
    );
  }
}
