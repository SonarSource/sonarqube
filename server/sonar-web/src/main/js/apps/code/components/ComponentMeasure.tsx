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
import {
  ContentCell,
  MetricsRatingBadge,
  NumericalCell,
  QualityGateIndicator,
  RatingCell,
  RatingEnum,
} from 'design-system';
import * as React from 'react';
import Measure from '../../../components/measure/Measure';
import { getLeakValue } from '../../../components/measure/utils';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  OLD_TO_NEW_TAXONOMY_METRICS_MAP,
} from '../../../helpers/constants';
import { translateWithParameters } from '../../../helpers/l10n';
import {
  areCCTMeasuresComputed as areCCTMeasuresComputedFn,
  formatMeasure,
  isDiffMetric,
} from '../../../helpers/measures';
import { isApplication, isProject } from '../../../types/component';
import { MetricKey, MetricType } from '../../../types/metrics';
import { Metric, Status, ComponentMeasure as TypeComponentMeasure } from '../../../types/types';

interface Props {
  component: TypeComponentMeasure;
  metric: Metric;
}

export default function ComponentMeasure(props: Props) {
  const { component, metric } = props;
  const isProjectLike = isProject(component.qualifier) || isApplication(component.qualifier);
  const isReleasability = metric.key === MetricKey.releasability_rating;

  let finalMetricKey = isProjectLike && isReleasability ? MetricKey.alert_status : metric.key;
  const finalMetricType = isProjectLike && isReleasability ? MetricType.Level : metric.type;

  const areCCTMeasasuresComputed = areCCTMeasuresComputedFn(component.measures);
  finalMetricKey = areCCTMeasasuresComputed
    ? OLD_TO_NEW_TAXONOMY_METRICS_MAP[finalMetricKey as MetricKey] ?? finalMetricKey
    : finalMetricKey;

  const measure = Array.isArray(component.measures)
    ? component.measures.find((measure) => measure.metric === finalMetricKey)
    : undefined;

  let value;
  if (
    measure?.value !== undefined &&
    CCT_SOFTWARE_QUALITY_METRICS.includes(measure.metric as MetricKey)
  ) {
    value = JSON.parse(measure.value).total;
  } else {
    value = isDiffMetric(metric.key) ? getLeakValue(measure) : measure?.value;
  }

  switch (finalMetricType) {
    case MetricType.Level: {
      const formatted = formatMeasure(value, MetricType.Level);
      const ariaLabel = translateWithParameters('overview.quality_gate_x', formatted);

      return (
        <ContentCell className="sw-whitespace-nowrap">
          <QualityGateIndicator
            status={(value as Status) ?? 'NONE'}
            className="sw-mr-2"
            ariaLabel={ariaLabel}
            size="sm"
          />
          <span>{formatted}</span>
        </ContentCell>
      );
    }
    case MetricType.Rating:
      return (
        <RatingCell className="sw-whitespace-nowrap">
          <MetricsRatingBadge
            label={value ?? '—'}
            rating={formatMeasure(value, MetricType.Rating) as RatingEnum}
          />
        </RatingCell>
      );
    default:
      return (
        <NumericalCell className="sw-whitespace-nowrap">
          <Measure metricKey={finalMetricKey} metricType={finalMetricType} value={value} />
        </NumericalCell>
      );
  }
}
