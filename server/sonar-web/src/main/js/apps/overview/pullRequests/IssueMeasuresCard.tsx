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
import {
  HelperHintIcon,
  LightGreyCard,
  LightLabel,
  SnoozeCircleIcon,
  TextError,
  TextSubdued,
  TrendDownCircleIcon,
  TrendUpCircleIcon,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { getComponentIssuesUrl } from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import Tooltip from '../../../components/controls/Tooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { findMeasure } from '../../../helpers/measures';
import { PullRequest } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';
import { Status, getConditionRequiredLabel } from '../utils';

interface Props {
  conditions: QualityGateStatusConditionEnhanced[];
  measures: MeasureEnhanced[];
  component: Component;
  pullRequest: PullRequest;
}

export default function IssueMeasuresCard(
  props: React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>,
) {
  const { measures, conditions, component, pullRequest, ...rest } = props;

  const intl = useIntl();

  const issuesCount = getLeakValue(findMeasure(measures, MetricKey.new_violations));
  const issuesCondition = conditions.find((c) => c.metric === MetricKey.new_violations);
  const issuesConditionFailed = issuesCondition?.level === Status.ERROR;
  const fixedCount = findMeasure(measures, MetricKey.pull_request_fixed_issues)?.value;
  const acceptedCount = getLeakValue(findMeasure(measures, MetricKey.new_accepted_issues));

  const issuesUrl = getComponentIssuesUrl(component.key, {
    ...getBranchLikeQuery(pullRequest),
    ...DEFAULT_ISSUES_QUERY,
  });
  const fixedUrl = getComponentIssuesUrl(component.key, {
    fixedInPullRequest: pullRequest.key,
  });
  const acceptedUrl = getComponentIssuesUrl(component.key, {
    ...getBranchLikeQuery(pullRequest),
    ...DEFAULT_ISSUES_QUERY,
    issueStatuses: 'ACCEPTED',
  });

  return (
    <LightGreyCard className="sw-p-8 sw-rounded-2 sw-flex sw-text-base sw-gap-4" {...rest}>
      <IssueMeasuresCardInner
        className="sw-w-1/3"
        header={intl.formatMessage({ id: 'overview.new_issues' })}
        data-testid={`overview__measures-${MetricKey.new_violations}`}
        data-guiding-id={issuesConditionFailed ? 'overviewZeroNewIssuesSimplification' : undefined}
        metric={MetricKey.new_violations}
        value={formatMeasure(issuesCount, MetricType.ShortInteger)}
        url={issuesUrl}
        failed={issuesConditionFailed}
        icon={issuesConditionFailed && <TrendUpCircleIcon />}
        footer={
          issuesCondition &&
          (issuesConditionFailed ? (
            <TextError
              className="sw-font-regular sw-body-xs sw-inline"
              text={getConditionRequiredLabel(issuesCondition, intl, true)}
            />
          ) : (
            <LightLabel className="sw-body-xs">
              {getConditionRequiredLabel(issuesCondition, intl)}
            </LightLabel>
          ))
        }
      />
      <StyledCardSeparator />
      <IssueMeasuresCardInner
        className="sw-w-1/3"
        header={intl.formatMessage({ id: 'overview.accepted_issues' })}
        data-testid={`overview__measures-${MetricKey.new_accepted_issues}`}
        metric={MetricKey.new_accepted_issues}
        value={formatMeasure(acceptedCount, MetricType.ShortInteger)}
        disabled={component.needIssueSync}
        url={acceptedUrl}
        icon={
          acceptedCount && (
            <SnoozeCircleIcon
              color={acceptedCount === '0' ? 'overviewCardDefaultIcon' : 'overviewCardWarningIcon'}
            />
          )
        }
        footer={
          <TextSubdued className="sw-body-xs">
            {intl.formatMessage({ id: 'overview.accepted_issues.help' })}
          </TextSubdued>
        }
      />
      <StyledCardSeparator />
      <IssueMeasuresCardInner
        className="sw-w-1/3"
        header={
          <>
            {intl.formatMessage({ id: 'overview.pull_request.fixed_issues' })}
            <Tooltip
              content={
                <div className="sw-flex sw-flex-col sw-gap-4">
                  <span>
                    {intl.formatMessage({ id: 'overview.pull_request.fixed_issues.disclaimer' })}
                  </span>
                  <span>
                    {intl.formatMessage({
                      id: 'overview.pull_request.fixed_issues.disclaimer.2',
                    })}
                  </span>
                </div>
              }
              side="top"
            >
              <HelperHintIcon raised />
            </Tooltip>
          </>
        }
        data-testid={`overview__measures-${MetricKey.pull_request_fixed_issues}`}
        metric={MetricKey.pull_request_fixed_issues}
        value={formatMeasure(fixedCount, MetricType.ShortInteger)}
        disabled={component.needIssueSync}
        url={fixedUrl}
        icon={fixedCount && fixedCount !== '0' && <TrendDownCircleIcon />}
        footer={
          <TextSubdued className="sw-body-xs">
            {intl.formatMessage({ id: 'overview.pull_request.fixed_issues.help' })}
          </TextSubdued>
        }
      />
    </LightGreyCard>
  );
}

const StyledCardSeparator = styled.div`
  width: 1px;
  background-color: ${themeColor('projectCardBorder')};
`;
