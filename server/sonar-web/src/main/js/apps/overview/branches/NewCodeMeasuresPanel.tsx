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
  LightGreyCard,
  LightLabel,
  MetricsRatingBadge,
  NoDataIcon,
  SnoozeCircleIcon,
  TextError,
  TextSubdued,
  TrendUpCircleIcon,
  themeColor,
} from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { getTabPanelId } from '../../../components/controls/BoxedTabs';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { findMeasure, formatMeasure, formatRating } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { Branch } from '../../../types/branch-like';
import { isApplication } from '../../../types/component';
import { IssueStatus } from '../../../types/issues';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import { MeasuresTabs, Status, getConditionRequiredLabel } from '../utils';
import MeasuresPanelPercentCards from './MeasuresPanelPercentCards';

interface Props {
  branch?: Branch;
  component: Component;
  measures: MeasureEnhanced[];
  qgStatuses?: QualityGateStatus[];
}

export default function NewCodeMeasuresPanel(props: Readonly<Props>) {
  const { branch, component, measures, qgStatuses } = props;
  const intl = useIntl();
  const isApp = isApplication(component.qualifier);

  const conditions = qgStatuses?.flatMap((qg) => qg.conditions) ?? [];

  const newIssues = getLeakValue(findMeasure(measures, MetricKey.new_violations));
  const newIssuesCondition = conditions.find((c) => c.metric === MetricKey.new_violations);
  const issuesConditionFailed = newIssuesCondition?.level === Status.ERROR;
  const newAcceptedIssues = getLeakValue(findMeasure(measures, MetricKey.new_accepted_issues));
  const newSecurityHotspots = getLeakValue(
    findMeasure(measures, MetricKey.new_security_hotspots),
  ) as string;
  const newSecurityReviewRating = getLeakValue(
    findMeasure(measures, MetricKey.new_security_review_rating),
  );

  let issuesFooter;
  if (newIssuesCondition && !isApp) {
    issuesFooter = issuesConditionFailed ? (
      <TextError
        className="sw-font-regular sw-body-xs sw-inline"
        text={getConditionRequiredLabel(newIssuesCondition, intl, true)}
      />
    ) : (
      <LightLabel className="sw-body-xs">
        {getConditionRequiredLabel(newIssuesCondition, intl)}
      </LightLabel>
    );
  }

  let acceptedIssuesFooter = null;
  if (!newAcceptedIssues) {
    acceptedIssuesFooter = (
      <StyledInfoMessage className="sw-rounded-2 sw-text-xs sw-p-4 sw-flex sw-gap-1 sw-flex-wrap">
        <span>
          {intl.formatMessage({
            id: `overview.run_analysis_to_compute.${component.qualifier}`,
          })}
        </span>
      </StyledInfoMessage>
    );
  } else {
    acceptedIssuesFooter = (
      <TextSubdued className="sw-body-xs">
        {intl.formatMessage({ id: 'overview.accepted_issues.help' })}
      </TextSubdued>
    );
  }

  return (
    <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-6" id={getTabPanelId(MeasuresTabs.New)}>
      <LightGreyCard className="sw-flex sw-col-span-2 sw-rounded-2 sw-gap-4">
        <IssueMeasuresCardInner
          data-testid="overview__measures-new_issues"
          disabled={component.needIssueSync}
          className="sw-w-1/2"
          metric={MetricKey.new_violations}
          value={formatMeasure(newIssues, MetricType.ShortInteger)}
          header={intl.formatMessage({
            id: 'overview.new_issues',
          })}
          url={getComponentIssuesUrl(component.key, {
            ...getBranchLikeQuery(branch),
            ...DEFAULT_ISSUES_QUERY,
            inNewCodePeriod: 'true',
          })}
          failed={issuesConditionFailed}
          icon={issuesConditionFailed && <TrendUpCircleIcon />}
          footer={issuesFooter}
        />
        <StyledCardSeparator />
        <IssueMeasuresCardInner
          data-testid="overview__measures-accepted_issues"
          disabled={Boolean(component.needIssueSync) || !newAcceptedIssues}
          className="sw-w-1/2"
          metric={MetricKey.new_accepted_issues}
          value={formatMeasure(newAcceptedIssues, MetricType.ShortInteger)}
          header={intl.formatMessage({
            id: 'overview.accepted_issues',
          })}
          url={getComponentIssuesUrl(component.key, {
            ...getBranchLikeQuery(branch),
            issueStatuses: IssueStatus.Accepted,
            inNewCodePeriod: 'true',
          })}
          footer={acceptedIssuesFooter}
          icon={
            <SnoozeCircleIcon
              color={
                newAcceptedIssues === '0' ? 'overviewCardDefaultIcon' : 'overviewCardWarningIcon'
              }
            />
          }
        />
      </LightGreyCard>

      <MeasuresPanelPercentCards
        useDiffMetric
        branch={branch}
        component={component}
        measures={measures}
        conditions={conditions}
      />

      <MeasuresCardNumber
        label={
          newSecurityHotspots === '1'
            ? 'issue.type.SECURITY_HOTSPOT'
            : 'issue.type.SECURITY_HOTSPOT.plural'
        }
        url={getComponentSecurityHotspotsUrl(component.key, {
          ...getBranchLikeQuery(branch),
        })}
        value={newSecurityHotspots}
        metric={MetricKey.new_security_hotspots}
        conditions={conditions}
        conditionMetric={MetricKey.new_security_hotspots_reviewed}
        showRequired={!isApp}
        icon={
          newSecurityReviewRating ? (
            <MetricsRatingBadge
              label={newSecurityReviewRating}
              rating={formatRating(newSecurityReviewRating)}
              size="md"
            />
          ) : (
            <NoDataIcon size="md" />
          )
        }
      />
    </div>
  );
}

const StyledCardSeparator = styled.div`
  width: 1px;
  background-color: ${themeColor('projectCardBorder')};
`;

const StyledInfoMessage = styled.div`
  background-color: ${themeColor('projectCardInfo')};
`;
