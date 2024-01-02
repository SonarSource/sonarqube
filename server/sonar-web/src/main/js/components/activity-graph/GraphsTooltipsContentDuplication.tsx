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
import { translate } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { MetricKey, MetricType } from '../../types/metrics';
import { MeasureHistory } from '../../types/project-activity';

export interface GraphsTooltipsContentDuplicationProps {
  addSeparator: boolean;
  measuresHistory: MeasureHistory[];
  tooltipIdx: number;
}

export default function GraphsTooltipsContentDuplication(
  props: Readonly<GraphsTooltipsContentDuplicationProps>,
) {
  const { addSeparator, measuresHistory, tooltipIdx } = props;
  const duplicationDensity = measuresHistory.find(
    (measure) => measure.metric === MetricKey.duplicated_lines_density,
  );
  if (!duplicationDensity?.history[tooltipIdx]) {
    return null;
  }
  const duplicationDensityValue = duplicationDensity.history[tooltipIdx].value;
  if (!duplicationDensityValue) {
    return null;
  }
  return (
    <>
      {addSeparator && (
        <tr>
          <td colSpan={3}>
            <hr />
          </td>
        </tr>
      )}
      <tr className="sw-h-8">
        <td className="sw-font-bold sw-text-right sw-pr-2 thin" colSpan={2}>
          {formatMeasure(duplicationDensityValue, MetricType.Percent)}
        </td>
        <td>{translate('metric.duplicated_lines_density.name')}</td>
      </tr>
    </>
  );
}
