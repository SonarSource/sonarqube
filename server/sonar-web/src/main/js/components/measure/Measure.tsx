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
import { MetricsRatingBadge, QualityGateIndicator, RatingLabel } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { Status } from '~sonar-aligned/types/common';
import { MetricType } from '~sonar-aligned/types/metrics';
import Tooltip from '../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../helpers/l10n';
import RatingTooltipContent from './RatingTooltipContent';

interface Props {
  className?: string;
  decimals?: number;
  metricKey: string;
  metricType: string;
  small?: boolean;
  value: string | number | undefined;
  ratingComponent?: JSX.Element;
}

export default function Measure({
  className,
  decimals,
  metricKey,
  metricType,
  small,
  value,
  ratingComponent,
}: Props) {
  if (value === undefined) {
    return <span className={className}>—</span>;
  }

  if (metricType === MetricType.Level) {
    const formatted = formatMeasure(value, MetricType.Level);
    const ariaLabel = translateWithParameters('overview.quality_gate_x', formatted);

    return (
      <>
        <QualityGateIndicator
          status={(value as Status) ?? 'NONE'}
          className="sw-mr-2"
          ariaLabel={ariaLabel}
          size={small ? 'sm' : 'md'}
        />
        <span className={small ? '' : 'sw-body-md'}>{formatted}</span>
      </>
    );
  }

  if (metricType !== MetricType.Rating) {
    const formattedValue = formatMeasure(value, metricType, {
      decimals,
      omitExtraDecimalZeros: metricType === MetricType.Percent,
    });
    return <span className={className}>{formattedValue ?? '—'}</span>;
  }

  const tooltip = <RatingTooltipContent metricKey={metricKey} value={value} />;
  const rating = ratingComponent ?? (
    <MetricsRatingBadge
      size={small ? 'sm' : 'md'}
      label={
        value
          ? translateWithParameters('metric.has_rating_X', formatMeasure(value, MetricType.Rating))
          : translate('metric.no_rating')
      }
      rating={formatMeasure(value, MetricType.Rating) as RatingLabel}
    />
  );

  return (
    <Tooltip overlay={tooltip}>
      <span className={className}>{rating}</span>
    </Tooltip>
  );
}
