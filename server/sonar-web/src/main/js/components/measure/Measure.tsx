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
import { QualityGateIndicator } from 'design-system';
import * as React from 'react';
import Tooltip from '../../components/controls/Tooltip';
import Rating from '../../components/ui/Rating';
import { translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { MetricType } from '../../types/metrics';
import { Status } from '../../types/types';
import RatingTooltipContent from './RatingTooltipContent';

interface Props {
  className?: string;
  decimals?: number | null;
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
  const rating = ratingComponent ?? <Rating value={value} />;

  if (tooltip) {
    return (
      <Tooltip overlay={tooltip}>
        <span className={className}>{rating}</span>
      </Tooltip>
    );
  }
  return rating;
}
