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
import { TableSeparator } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { formatMeasure } from '../../sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '../../types/metrics';
import { MeasureHistory } from '../../types/project-activity';

export interface GraphsTooltipsContentCoverageProps {
  addSeparator: boolean;
  measuresHistory: MeasureHistory[];
  tooltipIdx: number;
}

export default function GraphsTooltipsContentCoverage(props: GraphsTooltipsContentCoverageProps) {
  const { addSeparator, measuresHistory, tooltipIdx } = props;
  const uncovered = measuresHistory.find((measure) => measure.metric === MetricKey.uncovered_lines);
  const coverage = measuresHistory.find((measure) => measure.metric === MetricKey.coverage);
  if (!uncovered?.history[tooltipIdx] || !coverage?.history[tooltipIdx]) {
    return null;
  }
  const uncoveredValue = uncovered.history[tooltipIdx].value;
  const coverageValue = coverage.history[tooltipIdx].value;
  return (
    <>
      {addSeparator && <TableSeparator />}
      {uncoveredValue && (
        <tr className="sw-h-8">
          <td className="sw-font-bold sw-text-right sw-pr-2 thin" colSpan={2}>
            {formatMeasure(uncoveredValue, MetricType.ShortInteger)}
          </td>
          <td>{translate('metric.uncovered_lines.name')}</td>
        </tr>
      )}
      {coverageValue && (
        <tr className="sw-h-8">
          <td className="sw-font-bold sw-text-right sw-pr-2 thin" colSpan={2}>
            {formatMeasure(coverageValue, MetricType.Percent)}
          </td>
          <td>{translate('metric.coverage.name')}</td>
        </tr>
      )}
    </>
  );
}
