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
import ChartLegendIcon from '../../components/icons/ChartLegendIcon';
import Rating from '../../components/ui/Rating';
import { MetricKey } from '../../types/metrics';
import { MeasureHistory } from '../../types/project-activity';
import { Dict } from '../../types/types';

export interface GraphsTooltipsContentIssuesProps {
  index: number;
  measuresHistory: MeasureHistory[];
  name: string;
  tooltipIdx: number;
  translatedName: string;
  value: string;
}

const METRIC_RATING: Dict<string> = {
  [MetricKey.bugs]: MetricKey.reliability_rating,
  [MetricKey.vulnerabilities]: MetricKey.security_rating,
  [MetricKey.code_smells]: MetricKey.sqale_rating,
};

export default function GraphsTooltipsContentIssues(props: GraphsTooltipsContentIssuesProps) {
  const { index, measuresHistory, name, tooltipIdx, translatedName, value } = props;
  const rating = measuresHistory.find((measure) => measure.metric === METRIC_RATING[name]);
  if (!rating || !rating.history[tooltipIdx]) {
    return null;
  }
  const ratingValue = rating.history[tooltipIdx].value;
  return (
    <tr className="activity-graph-tooltip-issues-line" key={name}>
      <td className="thin">
        <ChartLegendIcon className="spacer-right" index={index} />
      </td>
      <td className="text-right spacer-right">
        <span className="activity-graph-tooltip-value">{value}</span>
        {ratingValue && <Rating className="spacer-left" value={ratingValue} />}
      </td>
      <td>{translatedName}</td>
    </tr>
  );
}
