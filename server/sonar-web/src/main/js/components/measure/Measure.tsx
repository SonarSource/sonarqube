/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Rating from '../ui/Rating';
import Level from '../ui/Level';
import Tooltips from '../controls/Tooltip';
import { formatMeasure, isDiffMetric } from '../../helpers/measures';
import { formatLeak, getRatingTooltip, MeasureEnhanced } from './utils';

interface Props {
  className?: string;
  decimals?: number | null;
  measure?: MeasureEnhanced;
}

export default function Measure({ className, decimals, measure }: Props) {
  if (measure === undefined) {
    return <span>{'–'}</span>;
  }

  const metric = measure.metric;
  const value = isDiffMetric(metric.key) ? measure.leak : measure.value;

  if (value === undefined) {
    return <span>{'–'}</span>;
  }

  if (metric.type === 'LEVEL') {
    return <Level className={className} level={value} />;
  }

  if (metric.type !== 'RATING') {
    const formattedValue = isDiffMetric(metric.key)
      ? formatLeak(measure.leak, metric.key, metric.type, { decimals })
      : formatMeasure(measure.value, metric.type, { decimals });
    return <span className={className}>{formattedValue != null ? formattedValue : '–'}</span>;
  }

  const tooltip = getRatingTooltip(metric.key, Number(value));
  const rating = <Rating value={value} />;
  if (tooltip) {
    return (
      <Tooltips overlay={tooltip}>
        <span className={className}>{rating}</span>
      </Tooltips>
    );
  }
  return rating;
}
