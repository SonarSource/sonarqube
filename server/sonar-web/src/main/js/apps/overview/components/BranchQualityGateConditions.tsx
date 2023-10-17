/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { ChevronRightIcon, DangerButtonSecondary } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { getLocalizedMetricName } from '../../../helpers/l10n';
import { formatMeasure, getShortType, isDiffMetric } from '../../../helpers/measures';
import {
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { MetricType } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import {
  METRICS_REPORTED_IN_OVERVIEW_CARDS,
  RATING_METRICS_MAPPING,
  RATING_TO_SEVERITIES_MAPPING,
} from '../utils';

interface Props {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  failedConditions: QualityGateStatusConditionEnhanced[];
}

export default function BranchQualityGateConditions(props: Readonly<Props>) {
  const { branchLike, component, failedConditions } = props;

  const filteredFailedConditions = failedConditions.filter(
    (condition) => !METRICS_REPORTED_IN_OVERVIEW_CARDS.includes(condition.metric),
  );

  return (
    <ul className="sw-flex sw-items-center sw-gap-2 sw-flex-wrap">
      {filteredFailedConditions.map((condition) => (
        <li key={condition.metric}>
          <FailedQGCondition branchLike={branchLike} component={component} condition={condition} />
        </li>
      ))}
    </ul>
  );
}

function FailedQGCondition(
  props: Readonly<
    Pick<Props, 'branchLike' | 'component'> & { condition: QualityGateStatusConditionEnhanced }
  >,
) {
  const { branchLike, component, condition } = props;
  const url = getQGConditionUrl(component.key, condition, branchLike);

  return (
    <DangerButtonSecondary className="sw-px-2 sw-py-1 sw-rounded-1/2 sw-body-sm" to={url}>
      <FailedMetric condition={condition} />
      <ChevronRightIcon className="sw-ml-1" />
    </DangerButtonSecondary>
  );
}

interface FailedMetricProps {
  condition: QualityGateStatusConditionEnhanced;
}

export function FailedMetric(props: Readonly<FailedMetricProps>) {
  const {
    condition: {
      measure: { metric },
    },
  } = props;

  if (metric.type === MetricType.Rating) {
    return <FailedRatingMetric {...props} />;
  }

  return <FailedGeneralMetric {...props} />;
}

function FailedRatingMetric({ condition }: Readonly<FailedMetricProps>) {
  const {
    error,
    measure: {
      metric: { type, domain },
    },
  } = condition;
  const intl = useIntl();

  return (
    <>
      {intl.formatMessage(
        { id: 'overview.failed_condition.x_required' },
        {
          metric: `${intl.formatMessage({
            id: `metric_domain.${domain}`,
          })} ${intl.formatMessage({ id: 'metric.type.RATING' }).toLowerCase()}`,
          threshold: (
            <strong className="sw-body-sm-highlight sw-ml-1">{formatMeasure(error, type)}</strong>
          ),
        },
      )}
    </>
  );
}

function FailedGeneralMetric({ condition }: Readonly<FailedMetricProps>) {
  const {
    error,
    measure: { metric },
  } = condition;
  const intl = useIntl();
  const measureFormattingOptions = { decimals: 2, omitExtraDecimalZeros: true };

  return (
    <>
      {intl.formatMessage(
        { id: 'overview.failed_condition.x_required' },
        {
          metric: (
            <>
              <strong className="sw-body-sm-highlight sw-mr-1">
                {formatMeasure(
                  condition.actual,
                  getShortType(metric.type),
                  measureFormattingOptions,
                )}
              </strong>
              {getLocalizedMetricName(metric, true)}
            </>
          ),
          threshold: (
            <strong className="sw-body-sm-highlight sw-ml-1">
              {condition.op === 'GT' ? <>&le;</> : <>&ge;</>}{' '}
              {formatMeasure(error, getShortType(metric.type), measureFormattingOptions)}
            </strong>
          ),
        },
      )}
    </>
  );
}

function getQGConditionUrl(
  componentKey: string,
  condition: QualityGateStatusConditionEnhanced,
  branchLike?: BranchLike,
) {
  const { metric } = condition;
  const sinceLeakPeriod = isDiffMetric(metric);
  const issueType = RATING_METRICS_MAPPING[metric];

  if (issueType) {
    if (issueType === IssueType.SecurityHotspot) {
      return getComponentSecurityHotspotsUrl(componentKey, {
        ...getBranchLikeQuery(branchLike),
        ...(sinceLeakPeriod ? { sinceLeakPeriod: 'true' } : {}),
      });
    }
    return getComponentIssuesUrl(componentKey, {
      resolved: 'false',
      types: issueType,
      ...getBranchLikeQuery(branchLike),
      ...(sinceLeakPeriod ? { sinceLeakPeriod: 'true' } : {}),
      ...(issueType !== IssueType.CodeSmell
        ? { severities: RATING_TO_SEVERITIES_MAPPING[Number(condition.error) - 1] }
        : {}),
    });
  }

  return getComponentDrilldownUrl({
    componentKey,
    metric,
    branchLike,
    listView: true,
  });
}
