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

import { Highlight, LinkBox } from '~design-system';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { getComponentIssuesUrl } from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import { propsToIssueParams } from '../../../components/shared/utils';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric } from '../../quality-gates/utils';

interface Props {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  condition: QualityGateStatusConditionEnhanced;
}

export default function QualityGateSimplifiedCondition({
  branchLike,
  component,
  condition,
}: Readonly<Props>) {
  const metrics = useMetrics();
  const getPrimaryText = () => {
    const { measure } = condition;
    const { metric } = measure;

    const subText = getLocalizedMetricNameNoDiffMetric(metric, metrics);

    return subText;
  };

  const { measure } = condition;
  const { metric } = measure;

  const value = (condition.period ? measure.period?.value : measure.value) as string;

  const formattedValue = formatMeasure(value, MetricType.ShortInteger, {
    decimals: 0,
    omitExtraDecimalZeros: metric.type === MetricType.Percent,
  });

  return (
    <LinkBox
      to={getComponentIssuesUrl(component.key, {
        ...propsToIssueParams(condition.measure.metric.key, condition.period != null),
        ...getBranchLikeQuery(branchLike),
      })}
    >
      <div className="sw-flex sw-p-2 sw-items-baseline">
        <Highlight className="sw-mx-4 sw-w-6 sw-my-0 sw-text-right">{formattedValue}</Highlight>
        <Highlight
          className="sw-text-ellipsis sw-pr-4"
          data-guiding-id={
            metric.key === MetricKey.new_violations
              ? 'overviewZeroNewIssuesSimplification'
              : undefined
          }
        >
          {getPrimaryText()}
        </Highlight>
      </div>
    </LinkBox>
  );
}
