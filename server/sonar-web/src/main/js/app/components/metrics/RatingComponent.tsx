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
import { Spinner } from '@sonarsource/echoes-react';
import { MetricsRatingBadge, RatingEnum } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { getLeakValue } from '../../../components/measure/utils';
import { isDiffMetric } from '../../../helpers/measures';
import { useMeasureQuery } from '../../../queries/measures';
import { useIsLegacyCCTMode } from '../../../queries/settings';

interface Props {
  className?: string;
  componentKey: string;
  ratingMetric: MetricKey;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

type RatingMetricKeys =
  | MetricKey.reliability_rating
  | MetricKey.sqale_rating
  | MetricKey.security_rating
  | MetricKey.security_review_rating
  | MetricKey.releasability_rating;

const ALLOW_NEW_METRICS = false;

const useGetMetricKeyForRating = (ratingMetric: RatingMetricKeys): MetricKey | null => {
  const { data: isLegacy, isLoading } = useIsLegacyCCTMode();
  if (isLoading) {
    return null;
  }
  return isLegacy || !ALLOW_NEW_METRICS ? ratingMetric : ((ratingMetric + '_new') as MetricKey);
};

export default function RatingComponent({ componentKey, ratingMetric, size, className }: Props) {
  const metricKey = useGetMetricKeyForRating(ratingMetric as RatingMetricKeys);
  const { data: measure, isLoading } = useMeasureQuery(
    { componentKey, metricKey: metricKey ?? '' },
    { enabled: !!metricKey },
  );
  const value = isDiffMetric(metricKey ?? '') ? getLeakValue(measure) : measure?.value;
  return (
    <Spinner isLoading={isLoading}>
      <MetricsRatingBadge
        label={value ?? '—'}
        rating={formatMeasure(value, MetricType.Rating) as RatingEnum}
        size={size}
        className={className}
      />
    </Spinner>
  );
}
