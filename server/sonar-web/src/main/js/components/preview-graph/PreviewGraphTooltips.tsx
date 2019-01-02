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
import PreviewGraphTooltipsContent from './PreviewGraphTooltipsContent';
import DateFormatter from '../intl/DateFormatter';
import { Popup, PopupPlacement } from '../ui/popups';
import { Serie } from '../../apps/projectActivity/utils';

interface Props {
  formatValue: (value: number | string) => string;
  graph: string;
  graphWidth: number;
  selectedDate: Date;
  series: Serie[];
  tooltipIdx: number;
  tooltipPos: number;
}

const TOOLTIP_WIDTH = 160;

export default class PreviewGraphTooltips extends React.PureComponent<Props> {
  render() {
    const { tooltipIdx } = this.props;
    const top = 16;
    let left = this.props.tooltipPos;
    let placement = PopupPlacement.RightTop;
    if (left > this.props.graphWidth - TOOLTIP_WIDTH) {
      left -= TOOLTIP_WIDTH;
      placement = PopupPlacement.LeftTop;
    }

    return (
      <Popup
        className="overview-analysis-graph-popup disabled-pointer-events"
        placement={placement}
        style={{ top, left, width: TOOLTIP_WIDTH }}>
        <div className="overview-analysis-graph-tooltip">
          <div className="overview-analysis-graph-tooltip-title">
            <DateFormatter date={this.props.selectedDate} long={true} />
          </div>
          <table className="width-100">
            <tbody>
              {this.props.series.map((serie, idx) => {
                const point = serie.data[tooltipIdx];
                if (!point || (!point.y && point.y !== 0)) {
                  return null;
                }
                return (
                  <PreviewGraphTooltipsContent
                    index={idx}
                    key={serie.name}
                    translatedName={serie.translatedName}
                    value={this.props.formatValue(point.y)}
                  />
                );
              })}
            </tbody>
          </table>
        </div>
      </Popup>
    );
  }
}
