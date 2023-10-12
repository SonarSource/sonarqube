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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import {
  GRID_INDEX_OFFSET,
  PERCENT_MULTIPLIER,
  getMaintainabilityGrid,
} from '../../../helpers/ratings';
import { AppState } from '../../../types/appstate';
import { MetricKey, MetricType } from '../../../types/metrics';
import { GlobalSettingKeys } from '../../../types/settings';
import { Condition, Metric } from '../../../types/types';
import { GreenColorText } from './ConditionValue';

const NO_DESCRIPTION_CONDITION = [
  MetricKey.new_violations,
  MetricKey.new_security_hotspots_reviewed,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
];

interface Props {
  appState: AppState;
  condition: Condition;
  metric: Metric;
  isToBeModified?: boolean;
}

function ConditionValueDescription({
  condition,
  appState: { settings },
  metric,
  isToBeModified = false,
}: Readonly<Props>) {
  if (condition.metric === MetricKey.new_maintainability_rating) {
    const maintainabilityGrid = getMaintainabilityGrid(
      settings[GlobalSettingKeys.RatingGrid] ?? '',
    );
    const maintainabilityRatingThreshold =
      maintainabilityGrid[Math.floor(Number(condition.error)) - GRID_INDEX_OFFSET];
    const ratingLetter = formatMeasure(condition.error, MetricType.Rating);

    return (
      <GreenColorText isToBeModified={isToBeModified}>
        (
        {condition.error === '1'
          ? translateWithParameters(
              'quality_gates.cayc.new_maintainability_rating.A',
              formatMeasure(maintainabilityGrid[0] * PERCENT_MULTIPLIER, MetricType.Percent),
            )
          : translateWithParameters(
              'quality_gates.cayc.new_maintainability_rating',
              ratingLetter,
              formatMeasure(
                maintainabilityRatingThreshold * PERCENT_MULTIPLIER,
                MetricType.Percent,
              ),
            )}
        )
      </GreenColorText>
    );
  }

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

export default withAppStateContext(ConditionValueDescription);
