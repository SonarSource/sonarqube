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

import { ContentCell, NumericalCell, QualityGateIndicator, RatingCell } from '~design-system';
import Measure from '~sonar-aligned/components/measure/Measure';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { Status } from '~sonar-aligned/types/common';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import RatingComponent from '../../../app/components/metrics/RatingComponent';
import { getLeakValue } from '../../../components/measure/utils';
import { OLD_TO_NEW_TAXONOMY_METRICS_MAP } from '../../../helpers/constants';
import {
  areCCTMeasuresComputed as areCCTMeasuresComputedFn,
  isDiffMetric,
} from '../../../helpers/measures';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { BranchLike } from '../../../types/branch-like';
import { isApplication, isProject } from '../../../types/component';
import { Metric, ComponentMeasure as TypeComponentMeasure } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  component: TypeComponentMeasure;
  metric: Metric;
}

export default function ComponentMeasure(props: Readonly<Props>) {
  const { component, metric, branchLike } = props;
  const isProjectLike = isProject(component.qualifier) || isApplication(component.qualifier);
  const { data: isStandardMode } = useStandardExperienceModeQuery();
  const isReleasability = metric.key === MetricKey.releasability_rating;

  let finalMetricKey = isProjectLike && isReleasability ? MetricKey.alert_status : metric.key;
  const finalMetricType = isProjectLike && isReleasability ? MetricType.Level : metric.type;

  const areCCTMeasuresComputed = !isStandardMode && areCCTMeasuresComputedFn(component.measures);
  finalMetricKey = areCCTMeasuresComputed
    ? (OLD_TO_NEW_TAXONOMY_METRICS_MAP[finalMetricKey as MetricKey] ?? finalMetricKey)
    : finalMetricKey;

  const measure = Array.isArray(component.measures)
    ? component.measures.find((measure) => measure.metric === finalMetricKey)
    : undefined;

  const value = isDiffMetric(metric.key) ? getLeakValue(measure) : measure?.value;

  switch (finalMetricType) {
    case MetricType.Level: {
      const formatted = formatMeasure(value, MetricType.Level);

      return (
        <ContentCell className="sw-whitespace-nowrap">
          <QualityGateIndicator
            status={(value as Status) ?? 'NONE'}
            className="sw-mr-2"
            size="sm"
          />
          <span>{formatted}</span>
        </ContentCell>
      );
    }
    case MetricType.Rating:
      return (
        <RatingCell className="sw-whitespace-nowrap">
          <RatingComponent
            branchLike={branchLike}
            componentKey={component.key}
            ratingMetric={metric.key as MetricKey}
          />
        </RatingCell>
      );
    default:
      return (
        <NumericalCell className="sw-whitespace-nowrap">
          <Measure
            branchLike={branchLike}
            componentKey={component.key}
            metricKey={finalMetricKey}
            metricType={finalMetricType}
            value={value}
          />
        </NumericalCell>
      );
  }
}
