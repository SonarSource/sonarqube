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
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../helpers/measures';
import { AppState } from '../../types/appstate';
import { MetricKey } from '../../types/metrics';
import { GlobalSettingKeys } from '../../types/settings';
import { KNOWN_RATINGS } from './utils';

const RATING_GRID_SIZE = 4;
const DIFF_METRIC_PREFIX_LENGTH = 4;
const PERCENT_MULTIPLIER = 100;
const GRID_INDEX_OFFSET = 2; // Rating of 2 should get index 0 (threshold between 1 and 2)

export interface RatingTooltipContentProps {
  appState: AppState;
  metricKey: MetricKey | string;
  value: number | string;
}

function getMaintainabilityGrid(ratingGridSetting: string) {
  const numbers = ratingGridSetting
    .split(',')
    .map((s) => parseFloat(s))
    .filter((n) => !isNaN(n));

  return numbers.length === RATING_GRID_SIZE ? numbers : [0, 0, 0, 0];
}

export function RatingTooltipContent(props: RatingTooltipContentProps) {
  const {
    appState: { settings },
    metricKey,
    value,
  } = props;

  const finalMetricKey = isDiffMetric(metricKey)
    ? metricKey.slice(DIFF_METRIC_PREFIX_LENGTH)
    : metricKey;

  if (!KNOWN_RATINGS.includes(finalMetricKey)) {
    return null;
  }

  const rating = Number(value);
  const ratingLetter = formatMeasure(value, 'RATING');

  if (finalMetricKey !== 'sqale_rating' && finalMetricKey !== 'maintainability_rating') {
    return <>{translate('metric', finalMetricKey, 'tooltip', ratingLetter)}</>;
  }

  const maintainabilityGrid = getMaintainabilityGrid(settings[GlobalSettingKeys.RatingGrid] ?? '');
  const maintainabilityRatingThreshold =
    maintainabilityGrid[Math.floor(rating) - GRID_INDEX_OFFSET];

  return (
    // Required to correctly satisfy the context typing
    // eslint-disable-next-line react/jsx-no-useless-fragment
    <>
      {rating === 1
        ? translateWithParameters(
            'metric.sqale_rating.tooltip.A',
            formatMeasure(maintainabilityGrid[0] * PERCENT_MULTIPLIER, 'PERCENT')
          )
        : translateWithParameters(
            'metric.sqale_rating.tooltip',
            ratingLetter,
            formatMeasure(maintainabilityRatingThreshold * PERCENT_MULTIPLIER, 'PERCENT')
          )}
    </>
  );
}

export default withAppStateContext(RatingTooltipContent);
