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
import { Spinner, Tooltip } from '@sonarsource/echoes-react';
import { MetricsRatingBadge, RatingEnum } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { getLeakValue } from '../../../components/measure/utils';
import { isDiffMetric } from '../../../helpers/measures';
import { useMeasureQuery } from '../../../queries/measures';
import { useIsLegacyCCTMode } from '../../../queries/settings';
import { BranchLike } from '../../../types/branch-like';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  componentKey: string;
  getLabel?: (rating: RatingEnum) => string;
  getTooltip?: (rating: RatingEnum) => React.ReactNode;
  ratingMetric: MetricKey;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
}

type RatingMetricKeys =
  | MetricKey.reliability_rating
  | MetricKey.sqale_rating
  | MetricKey.security_rating
  | MetricKey.security_review_rating
  | MetricKey.releasability_rating;

function isNewRatingMetric(metricKey: MetricKey) {
  return metricKey.includes('_new');
}

const useGetMetricKeyForRating = (ratingMetric: RatingMetricKeys): MetricKey | null => {
  const { data: isLegacy, isLoading } = useIsLegacyCCTMode();

  if (isNewRatingMetric(ratingMetric)) {
    return ratingMetric;
  }

  if (isLoading) {
    return null;
  }
  return isLegacy ? ratingMetric : ((ratingMetric + '_new') as MetricKey);
};

export default function RatingComponent(props: Readonly<Props>) {
  const { componentKey, ratingMetric, size, className, getLabel, branchLike, getTooltip } = props;

  const metricKey = useGetMetricKeyForRating(ratingMetric as RatingMetricKeys);
  const { data: isLegacy } = useIsLegacyCCTMode();
  const { data: targetMeasure, isLoading: isLoadingTargetMeasure } = useMeasureQuery(
    { componentKey, metricKey: metricKey ?? '', branchLike },
    { enabled: !!metricKey },
  );

  const { data: oldMeasure, isLoading: isLoadingOldMeasure } = useMeasureQuery(
    { componentKey, metricKey: ratingMetric, branchLike },
    { enabled: !isLegacy && !isNewRatingMetric(ratingMetric) && targetMeasure === null },
  );

  const isLoading = isLoadingTargetMeasure || isLoadingOldMeasure;

  const measure = targetMeasure ?? oldMeasure;

  const value = isDiffMetric(metricKey ?? '') ? getLeakValue(measure) : measure?.value;
  const rating = formatMeasure(value, MetricType.Rating) as RatingEnum;

  const badge = (
    <MetricsRatingBadge
      label={getLabel ? getLabel(rating) : value ?? '—'}
      rating={rating}
      size={size}
      className={className}
    />
  );

  return (
    <Spinner isLoading={isLoading}>
      {getTooltip ? (
        <>
          <Tooltip content={getTooltip(rating)}>{badge}</Tooltip>
          {/* The badge is not interactive, so show the tooltip content for screen-readers only */}
          <span className="sw-sr-only">{getTooltip(rating)}</span>
        </>
      ) : (
        badge
      )}
    </Spinner>
  );
}
