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

import styled from '@emotion/styled';
import { useIntl } from 'react-intl';
import { Badge, ButtonSecondary, themeBorder, themeColor } from '~design-system';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { MetricType } from '~sonar-aligned/types/metrics';
import {
  DEFAULT_ISSUES_QUERY,
  isIssueMeasure,
  propsToIssueParams,
} from '../../../components/shared/utils';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { getShortType, isDiffMetric } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
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
    <ul className="sw-flex sw-items-center sw-gap-2 sw-flex-wrap sw-mb-4">
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
    <StyledConditionButton className="sw-px-3 sw-py-2 sw-rounded-1 sw-typo-default" to={url}>
      <Badge className="sw-mr-2 sw-px-1" variant="deleted">
        {translate('overview.measures.failed_badge')}
      </Badge>
      <SpanDanger>
        <FailedMetric condition={condition} />
      </SpanDanger>
    </StyledConditionButton>
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
    actual,
    measure: {
      metric: { type, domain },
    },
  } = condition;
  const intl = useIntl();

  return (
    <>
      {intl.formatMessage(
        { id: 'overview.failed_condition.x_rating_required' },
        {
          rating: `${intl.formatMessage({
            id: `metric_domain.${domain}`,
          })} ${intl.formatMessage({ id: 'metric.type.RATING' }).toLowerCase()}`,
          value: <strong className="sw-typo-semibold">{formatMeasure(actual, type)}</strong>,
          threshold: formatMeasure(error, type),
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
              <strong className="sw-typo-semibold sw-mr-1">
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
            <>
              {condition.op === 'GT' ? <>&le;</> : <>&ge;</>}{' '}
              {formatMeasure(error, getShortType(metric.type), measureFormattingOptions)}
            </>
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
  const ratingIssueType = RATING_METRICS_MAPPING[metric];

  if (ratingIssueType) {
    if (ratingIssueType === IssueType.SecurityHotspot) {
      return getComponentSecurityHotspotsUrl(componentKey, branchLike, {
        ...(sinceLeakPeriod ? { sinceLeakPeriod: 'true' } : {}),
      });
    }
    return getComponentIssuesUrl(componentKey, {
      ...DEFAULT_ISSUES_QUERY,
      types: ratingIssueType,
      ...getBranchLikeQuery(branchLike),
      ...(sinceLeakPeriod ? { sinceLeakPeriod: 'true' } : {}),
      ...(ratingIssueType !== IssueType.CodeSmell
        ? { severities: RATING_TO_SEVERITIES_MAPPING[Number(condition.error) - 1] }
        : {}),
    });
  }

  if (isIssueMeasure(condition.measure.metric.key)) {
    return getComponentIssuesUrl(componentKey, {
      ...propsToIssueParams(condition.measure.metric.key, condition.period != null),
      ...getBranchLikeQuery(branchLike),
    });
  }

  return getComponentDrilldownUrl({
    componentKey,
    metric,
    branchLike,
    listView: true,
  });
}

const StyledConditionButton = styled(ButtonSecondary)`
  --border: ${themeBorder('default')};
`;

const SpanDanger = styled.span`
  color: ${themeColor('danger')};
`;
