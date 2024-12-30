/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Table, TableSeparator, ThemeProp, themeColor, withTheme } from '~design-system';
import { Popup, PopupPlacement } from '../../components/ui/popups';
import { isDefined } from '../../helpers/types';
import { AnalysisEvent, GraphType, MeasureHistory, Serie } from '../../types/project-activity';
import DateTimeFormatter from '../intl/DateTimeFormatter';
import GraphsTooltipsContent from './GraphsTooltipsContent';
import GraphsTooltipsContentCoverage from './GraphsTooltipsContentCoverage';
import GraphsTooltipsContentDuplication from './GraphsTooltipsContentDuplication';
import GraphsTooltipsContentEvents from './GraphsTooltipsContentEvents';

interface PropsWithoutTheme {
  events: AnalysisEvent[];
  formatValue: (tick: number | string) => string;
  graph: string;
  graphWidth: number;
  measuresHistory: MeasureHistory[];
  selectedDate: Date;
  series: Serie[];
  tooltipIdx: number;
  tooltipPos: number;
}

export type Props = PropsWithoutTheme & ThemeProp;

const TOOLTIP_WIDTH = 280;
const TOOLTIP_LEFT_MARGIN = 60;
const TOOLTIP_LEFT_FLIP_THRESHOLD = 50;

const COLUMNS = 3;

export class GraphsTooltipsClass extends React.PureComponent<Props> {
  renderContent() {
    const { tooltipIdx, series } = this.props;

    return series.map((serie, idx) => {
      const point = serie.data[tooltipIdx];

      if (!point || (!point.y && point.y !== 0)) {
        return null;
      }

      return (
        <GraphsTooltipsContent
          index={idx}
          key={serie.name}
          name={serie.name}
          translatedName={serie.translatedName}
          value={this.props.formatValue(point.y)}
        />
      );
    });
  }

  render() {
    const {
      events,
      measuresHistory,
      tooltipIdx,
      tooltipPos,
      graph,
      graphWidth,
      selectedDate,
      theme,
    } = this.props;

    const top = 30;
    let left = tooltipPos + TOOLTIP_LEFT_MARGIN;
    let placement = PopupPlacement.RightTop;

    if (left > graphWidth - TOOLTIP_WIDTH - TOOLTIP_LEFT_FLIP_THRESHOLD) {
      left -= TOOLTIP_WIDTH;
      placement = PopupPlacement.LeftTop;
    }

    const tooltipContent = this.renderContent().filter(isDefined);
    const addSeparator = tooltipContent.length > 0;

    return (
      <Popup
        className="sw-pointer-events-none"
        noArrow
        placement={placement}
        style={{ top, left, width: TOOLTIP_WIDTH }}
      >
        <div className="sw-p-2">
          <div
            className="sw-typo-lg-semibold sw-whitespace-nowrap"
            style={{ color: themeColor('selectionCardHeader')({ theme }) }}
          >
            <DateTimeFormatter date={selectedDate} />
          </div>
          <Table
            columnCount={COLUMNS}
            noHeaderTopBorder
            style={{ color: 'var(--echoes-color-text-subdued)' }}
          >
            {addSeparator && <TableSeparator />}
            {events?.length > 0 && (
              <GraphsTooltipsContentEvents addSeparator={addSeparator} events={events} />
            )}
            {tooltipContent}
            {graph === GraphType.coverage && (
              <GraphsTooltipsContentCoverage
                addSeparator={addSeparator}
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />
            )}
            {graph === GraphType.duplications && (
              <GraphsTooltipsContentDuplication
                addSeparator={addSeparator}
                measuresHistory={measuresHistory}
                tooltipIdx={tooltipIdx}
              />
            )}
          </Table>
        </div>
      </Popup>
    );
  }
}

export const GraphsTooltips = withTheme<PropsWithoutTheme>(GraphsTooltipsClass);
