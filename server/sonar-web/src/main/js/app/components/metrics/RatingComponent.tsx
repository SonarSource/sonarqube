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
import * as React from 'react';
import { MetricsRatingBadge, RatingEnum } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { getLeakValue } from '../../../components/measure/utils';
import { SOFTWARE_QUALITY_RATING_METRICS_MAP } from '../../../helpers/constants';
import { isDiffMetric } from '../../../helpers/measures';
import { useMeasureQuery } from '../../../queries/measures';
import { useStandardExperienceMode } from '../../../queries/settings';
import { BranchLike } from '../../../types/branch-like';

type SizeType = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  componentKey: string;
  forceMetric?: boolean;
  getLabel?: (rating: RatingEnum) => string;
  getTooltip?: (
    rating: RatingEnum,
    value: string | undefined,
    metricKey?: MetricKey,
  ) => React.ReactNode;
  ratingMetric: MetricKey;
  size?: SizeType;
}

type RatingMetricKeys =
  | MetricKey.reliability_rating
  | MetricKey.sqale_rating
  | MetricKey.security_rating
  | MetricKey.security_review_rating
  | MetricKey.releasability_rating;

function isNewRatingMetric(metricKey: MetricKey) {
  return metricKey.includes('software_quality_');
}

const useGetMetricKeyForRating = (ratingMetric: RatingMetricKeys): MetricKey | null => {
  const { data: isStandardMode, isLoading } = useStandardExperienceMode();

  const hasSoftwareQualityRating = !!SOFTWARE_QUALITY_RATING_METRICS_MAP[ratingMetric];

  if (isNewRatingMetric(ratingMetric)) {
    return ratingMetric;
  }

  if (isLoading) {
    return null;
  }
  return isStandardMode || !hasSoftwareQualityRating
    ? ratingMetric
    : SOFTWARE_QUALITY_RATING_METRICS_MAP[ratingMetric];
};

export default function RatingComponent(props: Readonly<Props>) {
  const {
    componentKey,
    ratingMetric,
    size,
    forceMetric,
    className,
    getLabel,
    branchLike,
    getTooltip,
  } = props;

  const metricKey = useGetMetricKeyForRating(ratingMetric as RatingMetricKeys);
  const { data: isStandardMode } = useStandardExperienceMode();
  const { data: targetMeasure, isLoading: isLoadingTargetMeasure } = useMeasureQuery(
    { componentKey, metricKey: metricKey ?? '', branchLike },
    { enabled: !forceMetric && !!metricKey },
  );

  const { data: oldMeasure, isLoading: isLoadingOldMeasure } = useMeasureQuery(
    { componentKey, metricKey: ratingMetric, branchLike },
    {
      enabled:
        forceMetric ||
        (!isStandardMode && !isNewRatingMetric(ratingMetric) && targetMeasure === null),
    },
  );

  const isLoading = isLoadingTargetMeasure || isLoadingOldMeasure;

  const measure = forceMetric ? oldMeasure : (targetMeasure ?? oldMeasure);

  const value = isDiffMetric(metricKey ?? '') ? getLeakValue(measure) : measure?.value;
  const rating = formatMeasure(value, MetricType.Rating) as RatingEnum;

  const badge = (
    <MetricsRatingBadge
      label={getLabel ? getLabel(rating) : (value ?? '—')}
      rating={rating}
      size={size}
      className={className}
    />
  );

  return (
    <Spinner isLoading={isLoading}>
      {getTooltip ? (
        <>
          <Tooltip content={getTooltip(rating, value, measure?.metric as MetricKey)}>
            {badge}
          </Tooltip>
          {/* The badge is not interactive, so show the tooltip content for screen-readers only */}
          <span className="sw-sr-only">
            {getTooltip(rating, value, measure?.metric as MetricKey)}
          </span>
        </>
      ) : (
        badge
      )}
    </Spinner>
  );
}
