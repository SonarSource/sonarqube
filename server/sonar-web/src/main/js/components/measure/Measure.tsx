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
import Tooltip from '../../components/controls/Tooltip';
import Level from '../../components/ui/Level';
import Rating from '../../components/ui/Rating';
import { formatMeasure } from '../../helpers/measures';
import { MetricType } from '../../types/metrics';
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
    return <Level className={className} level={value?.toString()} small={small} />;
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
