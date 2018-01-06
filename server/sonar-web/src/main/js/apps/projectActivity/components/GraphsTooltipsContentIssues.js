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
import classNames from 'classnames';
import ChartLegendIcon from '../../../components/icons-components/ChartLegendIcon';
import Rating from '../../../components/ui/Rating';
/*:: import type { MeasureHistory } from '../types'; */

/*::
type Props = {
  measuresHistory: Array<MeasureHistory>,
  name: string,
  style: string,
  tooltipIdx: number,
  translatedName: string,
  value: string
};
*/

const METRIC_RATING = {
  bugs: 'reliability_rating',
  vulnerabilities: 'security_rating',
  code_smells: 'sqale_rating'
};

export default function GraphsTooltipsContentIssues(props /*: Props */) {
  const rating = props.measuresHistory.find(
    measure => measure.metric === METRIC_RATING[props.name]
  );
  if (!rating || !rating.history[props.tooltipIdx]) {
    return null;
  }
  const ratingValue = rating.history[props.tooltipIdx].value;
  return (
    <tr key={props.name} className="project-activity-graph-tooltip-issues-line">
      <td className="thin">
        <ChartLegendIcon
          className={classNames(
            'spacer-right line-chart-legend',
            'line-chart-legend-' + props.style
          )}
        />
      </td>
      <td className="text-right spacer-right">
        <span className="project-activity-graph-tooltip-value">{props.value}</span>
        {ratingValue && <Rating className="spacer-left" small={true} value={ratingValue} />}
      </td>
      <td>{props.translatedName}</td>
    </tr>
  );
}
