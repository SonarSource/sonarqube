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
import { getRatingTooltip } from './utils';
import Rating from '../ui/Rating';
import Level from '../ui/Level';
import Tooltips from '../controls/Tooltip';
import { formatMeasure } from '../../helpers/measures';

interface Props {
  className?: string;
  decimals?: number | null;
  value?: string;
  metricKey: string;
  metricType: string;
}

export default function Measure({ className, decimals, metricKey, metricType, value }: Props) {
  if (value === undefined) {
    return <span>{'–'}</span>;
  }

  if (metricType === 'LEVEL') {
    return <Level className={className} level={value} />;
  }

  if (metricType !== 'RATING') {
    const formattedValue = formatMeasure(value, metricType, { decimals });
    return <span className={className}>{formattedValue != null ? formattedValue : '–'}</span>;
  }

  const tooltip = getRatingTooltip(metricKey, Number(value));
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
