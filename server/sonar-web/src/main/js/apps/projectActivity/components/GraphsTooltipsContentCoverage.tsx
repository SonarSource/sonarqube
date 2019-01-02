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
import { formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { MeasureHistory } from '../utils';

interface Props {
  addSeparator: boolean;
  measuresHistory: MeasureHistory[];
  tooltipIdx: number;
}

export default function GraphsTooltipsContentCoverage({
  addSeparator,
  measuresHistory,
  tooltipIdx
}: Props) {
  const uncovered = measuresHistory.find(measure => measure.metric === 'uncovered_lines');
  const coverage = measuresHistory.find(measure => measure.metric === 'coverage');
  if (!uncovered || !uncovered.history[tooltipIdx] || !coverage || !coverage.history[tooltipIdx]) {
    return null;
  }
  const uncoveredValue = uncovered.history[tooltipIdx].value;
  const coverageValue = coverage.history[tooltipIdx].value;
  return (
    <tbody>
      {addSeparator && (
        <tr>
          <td className="project-activity-graph-tooltip-separator" colSpan={3}>
            <hr />
          </td>
        </tr>
      )}
      {uncoveredValue && (
        <tr className="project-activity-graph-tooltip-line">
          <td
            className="project-activity-graph-tooltip-value text-right spacer-right thin"
            colSpan={2}>
            {formatMeasure(uncoveredValue, 'SHORT_INT')}
          </td>
          <td>{translate('metric.uncovered_lines.name')}</td>
        </tr>
      )}
      {coverageValue && (
        <tr className="project-activity-graph-tooltip-line">
          <td
            className="project-activity-graph-tooltip-value text-right spacer-right thin"
            colSpan={2}>
            {formatMeasure(coverageValue, 'PERCENT')}
          </td>
          <td>{translate('metric.coverage.name')}</td>
        </tr>
      )}
    </tbody>
  );
}
