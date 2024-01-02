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
import styled from '@emotion/styled';
import {
  Card,
  HelperHintIcon,
  LightLabel,
  PopupPlacement,
  SnoozeCircleIcon,
  TextError,
  TextSubdued,
  themeColor,
  Tooltip,
  TrendDownCircleIcon,
  TrendUpCircleIcon,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { findMeasure, formatMeasure } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { PullRequest } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';
import { getConditionRequiredLabel, Status } from '../utils';

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
    <Card className="sw-p-8 sw-rounded-2 sw-flex sw-text-base sw-gap-4" {...rest}>
      <IssueMeasuresCardInner
        className="sw-w-1/3"
        header={intl.formatMessage({ id: 'overview.pull_request.new_issues' })}
        data-test="overview__measures-new-violations"
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
        header={intl.formatMessage({ id: 'overview.pull_request.accepted_issues' })}
        metric={MetricKey.new_accepted_issues}
        value={formatMeasure(acceptedCount, MetricType.ShortInteger)}
        linkDisabled={component.needIssueSync}
        url={acceptedUrl}
        icon={acceptedCount !== '0' && <SnoozeCircleIcon />}
        footer={
          <TextSubdued className="sw-body-xs">
            {intl.formatMessage({ id: 'overview.pull_request.accepted_issues.help' })}
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
              overlay={
                <div className="sw-flex sw-flex-col sw-gap-4">
                  <span>
                    {intl.formatMessage({ id: 'overview.pull_request.fixed_issues.disclaimer' })}
                  </span>
                  <span>
                    {intl.formatMessage({ id: 'overview.pull_request.fixed_issues.disclaimer.2' })}
                  </span>
                </div>
              }
              placement={PopupPlacement.Top}
            >
              <HelperHintIcon raised />
            </Tooltip>
          </>
        }
        metric={MetricKey.pull_request_fixed_issues}
        value={formatMeasure(fixedCount, MetricType.ShortInteger)}
        linkDisabled={component.needIssueSync}
        url={fixedUrl}
        icon={fixedCount !== '0' && <TrendDownCircleIcon />}
        footer={
          <TextSubdued className="sw-body-xs">
            {intl.formatMessage({ id: 'overview.pull_request.fixed_issues.help' })}
          </TextSubdued>
        }
      />
    </Card>
  );
}

const StyledCardSeparator = styled.div`
  width: 1px;
  background-color: ${themeColor('projectCardBorder')};
`;
