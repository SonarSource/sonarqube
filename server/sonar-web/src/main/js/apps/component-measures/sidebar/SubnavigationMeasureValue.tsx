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
import { MetricsLabel, MetricsRatingBadge, Note } from 'design-system';
import React from 'react';
import Measure from '../../../components/measure/Measure';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { MetricType } from '../../../types/metrics';
import { MeasureEnhanced } from '../../../types/types';

interface Props {
  measure: MeasureEnhanced;
}

export default function SubnavigationMeasureValue({ measure }: Props) {
  const isDiff = isDiffMetric(measure.metric.key);
  const value = isDiff ? measure.leak : measure.value;
  const formatted = formatMeasure(value, MetricType.Rating);

  return (
    <Note
      className="sw-flex sw-items-center sw-mr-1"
      id={`measure-${measure.metric.key}-${isDiff ? 'leak' : 'value'}`}
    >
      <Measure
        ratingComponent={
          <MetricsRatingBadge
            size="xs"
            label={
              value
                ? translateWithParameters('metric.has_rating_X', formatted)
                : translate('metric.no_rating')
            }
            rating={formatted as MetricsLabel}
          />
        }
        metricKey={measure.metric.key}
        metricType={measure.metric.type}
        value={value}
      />
    </Note>
  );
}
