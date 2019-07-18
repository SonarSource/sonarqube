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
import { Popup, PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { isDefined } from 'sonar-ui-common/helpers/types';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { DEFAULT_GRAPH, MeasureHistory, Serie } from '../utils';
import GraphsTooltipsContent from './GraphsTooltipsContent';
import GraphsTooltipsContentCoverage from './GraphsTooltipsContentCoverage';
import GraphsTooltipsContentDuplication from './GraphsTooltipsContentDuplication';
import GraphsTooltipsContentEvents from './GraphsTooltipsContentEvents';
import GraphsTooltipsContentIssues from './GraphsTooltipsContentIssues';

interface Props {
  events: T.AnalysisEvent[];
  formatValue: (tick: number | string) => string;
  graph: string;
  graphWidth: number;
  measuresHistory: MeasureHistory[];
  selectedDate: Date;
  series: Serie[];
  tooltipIdx: number;
  tooltipPos: number;
}

const TOOLTIP_WIDTH = 250;

export default class GraphsTooltips extends React.PureComponent<Props> {
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
            index={idx}
            key={serie.name}
            measuresHistory={this.props.measuresHistory}
            name={serie.name}
            tooltipIdx={tooltipIdx}
            translatedName={serie.translatedName}
            value={this.props.formatValue(point.y)}
          />
        );
      } else {
        return (
          <GraphsTooltipsContent
            index={idx}
            key={serie.name}
            name={serie.name}
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
    let placement = PopupPlacement.RightTop;
    if (left > this.props.graphWidth - TOOLTIP_WIDTH - 50) {
      left -= TOOLTIP_WIDTH;
      placement = PopupPlacement.LeftTop;
    }
    const tooltipContent = this.renderContent().filter(isDefined);
    const addSeparator = tooltipContent.length > 0;
    return (
      <Popup
        className="disabled-pointer-events"
        placement={placement}
        style={{ top, left, width: TOOLTIP_WIDTH }}>
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
            {events && events.length > 0 && (
              <GraphsTooltipsContentEvents addSeparator={addSeparator} events={events} />
            )}
          </table>
        </div>
      </Popup>
    );
  }
}
