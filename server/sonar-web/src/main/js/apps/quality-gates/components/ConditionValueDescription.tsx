/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';
import { Condition, Metric } from '../../../types/types';
import { GreenColorText } from './ConditionValue';

const NO_DESCRIPTION_CONDITION = [
  MetricKey.new_violations,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
  MetricKey.new_reliability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_maintainability_rating,
];

interface Props {
  condition: Condition;
  metric: Metric;
  isToBeModified?: boolean;
}

export default function ConditionValueDescription({
  condition,
  metric,
  isToBeModified = false,
}: Readonly<Props>) {
  return (
    <GreenColorText isToBeModified={isToBeModified}>
      {condition.isCaycCondition &&
        !NO_DESCRIPTION_CONDITION.includes(condition.metric as MetricKey) && (
          <>
            (
            {translate(
              `quality_gates.cayc.${condition.metric}.${formatMeasure(
                condition.error,
                metric.type,
              )}`,
            )}
            )
          </>
        )}
    </GreenColorText>
  );
}
