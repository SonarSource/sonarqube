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
// @flow
import React from 'react';
import GraphsTooltipsContent from './GraphsTooltipsContent';
import GraphsTooltipsContentEvents from './GraphsTooltipsContentEvents';
import GraphsTooltipsContentCoverage from './GraphsTooltipsContentCoverage';
import GraphsTooltipsContentDuplication from './GraphsTooltipsContentDuplication';
import GraphsTooltipsContentIssues from './GraphsTooltipsContentIssues';
import { DEFAULT_GRAPH } from '../utils';
import BubblePopup from '../../../components/common/BubblePopup';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
/*:: import type { Event, MeasureHistory } from '../types'; */
/*:: import type { Serie } from '../../../components/charts/AdvancedTimeline'; */

/*::
type Props = {
  events: Array<Event>,
  formatValue: (number | string) => string,
  graph: string,
  graphWidth: number,
  measuresHistory: Array<MeasureHistory>,
  selectedDate: Date,
  series: Array<Serie & { translatedName: string }>,
  tooltipIdx: number,
  tooltipPos: number
};
*/

const TOOLTIP_WIDTH = 250;

export default class GraphsTooltips extends React.PureComponent {
  /*:: props: Props; */

  renderContent() {
    const { tooltipIdx } = this.props;

    return this.props.series.map((serie, idx) => {
      const point = serie.data[tooltipIdx];
      if (!point || (!point.y && point.y !== 0)) {
        return null;
      }
      if (this.props.graph === DEFAULT_GRAPH) {
        return (
          <GraphsTooltipsContentIssues
            key={serie.name}
            measuresHistory={this.props.measuresHistory}
            name={serie.name}
            style={idx.toString()}
            tooltipIdx={tooltipIdx}
            translatedName={serie.translatedName}
            value={this.props.formatValue(point.y)}
          />
        );
      } else {
        return (
          <GraphsTooltipsContent
            key={serie.name}
            name={serie.name}
            style={idx.toString()}
            translatedName={serie.translatedName}
            value={this.props.formatValue(point.y)}
          />
        );
      }
    });
  }

  render() {
    const { events, measuresHistory, tooltipIdx } = this.props;
    const top = 30;
    let left = this.props.tooltipPos + 60;
    let customClass;
    if (left > this.props.graphWidth - TOOLTIP_WIDTH - 50) {
      left -= TOOLTIP_WIDTH;
      customClass = 'bubble-popup-right';
    }
    const tooltipContent = this.renderContent().filter(Boolean);
    const addSeparator = tooltipContent.length > 0;
    return (
      <BubblePopup customClass={customClass} position={{ top, left, width: TOOLTIP_WIDTH }}>
        <div className="project-activity-graph-tooltip">
          <div className="project-activity-graph-tooltip-title spacer-bottom">
            <DateTimeFormatter date={this.props.selectedDate} />
          </div>
          <table className="width-100">
            <tbody>{tooltipContent}</tbody>
            {this.props.graph === 'coverage' && (
              <GraphsTooltipsContentCoverage
                addSeparator={addSeparator}
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />
            )}
            {this.props.graph === 'duplications' && (
              <GraphsTooltipsContentDuplication
                addSeparator={addSeparator}
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />
            )}
            {events &&
              events.length > 0 && (
                <GraphsTooltipsContentEvents addSeparator={addSeparator} events={events} />
              )}
          </table>
        </div>
      </BubblePopup>
    );
  }
}
