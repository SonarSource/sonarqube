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
import { MetricsEnum, MetricsRatingBadge } from 'design-system';
import * as React from 'react';
import { ChartLegendIcon } from '../../components/icons/ChartLegendIcon';
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

  if (!rating?.history[tooltipIdx]) {
    return null;
  }

  const ratingValue = rating.history[tooltipIdx].value;

  const ratingEnumValue =
    (ratingValue &&
      {
        '1.0': MetricsEnum.A,
        '2.0': MetricsEnum.B,
        '3.0': MetricsEnum.C,
        '4.0': MetricsEnum.D,
        '5.0': MetricsEnum.E,
      }[ratingValue]) ||
    undefined;

  return (
    <tr className="sw-h-8" key={name}>
      <td className="sw-w-5">
        <ChartLegendIcon className="sw-mr-0" index={index} />
      </td>
      <td>
        <span className="sw-body-sm-highlight sw-ml-2">{value}</span>
      </td>
      <td>
        <span className="sw-body-sm">{translatedName}</span>
      </td>
      {ratingValue && (
        <td>
          <MetricsRatingBadge label={ratingValue} rating={ratingEnumValue} size="xs" />
        </td>
      )}
    </tr>
  );
}
