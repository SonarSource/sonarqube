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
import classNames from 'classnames';
import {
  Card,
  HelperHintIcon,
  LightLabel,
  SnoozeCircleIcon,
  TextError,
  TextSubdued,
  TrendDownCircleIcon,
  TrendUpCircleIcon,
} from 'design-system/lib';
import * as React from 'react';
import { useIntl } from 'react-intl';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import Tooltip from '../../../components/controls/Tooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { findMeasure } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { getBranchLikeQuery } from '../../../sonar-aligned/helpers/branch-like';
import { formatMeasure } from '../../../sonar-aligned/helpers/measures';
import { PullRequest } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, QualityGate } from '../../../types/types';
import {
  GridContainer,
  StyleMeasuresCard,
  StyleMeasuresCardRightBorder,
  StyledConditionsCard,
} from '../branches/BranchSummaryStyles';
import FailedConditions from '../branches/FailedConditions';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import MeasuresCardPercent from '../components/MeasuresCardPercent';
import {
  MeasurementType,
  Status,
  getConditionRequiredLabel,
  getMeasurementMetricKey,
} from '../utils';

interface Props {
  component: Component;
  conditions: QualityGateStatusConditionEnhanced[];
  measures: MeasureEnhanced[];
  pullRequest: PullRequest;
  qualityGate?: QualityGate;
}

export default function MeasuresCardPanel(props: React.PropsWithChildren<Props>) {
  const { pullRequest, component, measures, conditions, qualityGate } = props;

  const newSecurityHotspots = getLeakValue(
    findMeasure(measures, MetricKey.new_security_hotspots),
  ) as string;

  const intl = useIntl();

  const issuesCount = getLeakValue(findMeasure(measures, MetricKey.new_violations));
  const issuesCondition = conditions.find((c) => c.metric === MetricKey.new_violations);
  const isIssuesConditionFailed = issuesCondition?.level === Status.ERROR;
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

  const totalFailedConditions = conditions.filter((condition) => condition.level === Status.ERROR);

  return (
    <Card className="sw-py-8 sw-px-6">
      <GridContainer
        className={classNames('sw-relative sw-overflow-hidden js-summary', {
          'sw-grid-cols-3': totalFailedConditions.length === 0,
          'sw-grid-cols-4': totalFailedConditions.length > 0,
        })}
      >
        {totalFailedConditions.length > 0 && (
          <StyledConditionsCard className="sw-row-span-3">
            <FailedConditions
              qualityGate={qualityGate}
              branchLike={pullRequest}
              failedConditions={totalFailedConditions}
              isNewCode
              component={component}
            />
          </StyledConditionsCard>
        )}
        <StyleMeasuresCard>
          <IssueMeasuresCardInner
            header={intl.formatMessage({ id: 'overview.new_issues' })}
            data-testid={`overview__measures-${MetricKey.new_violations}`}
            metric={MetricKey.new_violations}
            value={formatMeasure(issuesCount, MetricType.ShortInteger)}
            url={issuesUrl}
            failed={isIssuesConditionFailed}
            icon={isIssuesConditionFailed && <TrendUpCircleIcon />}
            footer={
              issuesCondition &&
              (isIssuesConditionFailed ? (
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
        </StyleMeasuresCard>
        <StyleMeasuresCard>
          <IssueMeasuresCardInner
            header={intl.formatMessage({ id: 'overview.accepted_issues' })}
            data-testid={`overview__measures-${MetricKey.new_accepted_issues}`}
            metric={MetricKey.new_accepted_issues}
            value={formatMeasure(acceptedCount, MetricType.ShortInteger)}
            disabled={component.needIssueSync}
            url={acceptedUrl}
            icon={
              acceptedCount && (
                <SnoozeCircleIcon
                  color={
                    acceptedCount === '0' ? 'overviewCardDefaultIcon' : 'overviewCardWarningIcon'
                  }
                />
              )
            }
            footer={
              <TextSubdued className="sw-body-xs">
                {intl.formatMessage({ id: 'overview.accepted_issues.help' })}
              </TextSubdued>
            }
          />
        </StyleMeasuresCard>
        <StyleMeasuresCard>
          <IssueMeasuresCardInner
            header={
              <>
                {intl.formatMessage({ id: 'overview.pull_request.fixed_issues' })}
                <Tooltip
                  content={
                    <div className="sw-flex sw-flex-col sw-gap-4">
                      <span>
                        {intl.formatMessage({
                          id: 'overview.pull_request.fixed_issues.disclaimer',
                        })}
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
        </StyleMeasuresCard>
        <StyleMeasuresCardRightBorder>
          <MeasuresCardPercent
            componentKey={component.key}
            branchLike={pullRequest}
            measurementType={MeasurementType.Coverage}
            label="overview.quality_gate.coverage"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Coverage, true),
              branchLike: pullRequest,
              listView: true,
            })}
            conditions={conditions}
            conditionMetric={MetricKey.new_coverage}
            overallConditionMetric={MetricKey.coverage}
            linesMetric={MetricKey.new_lines_to_cover}
            measures={measures}
            showRequired
            useDiffMetric
          />
        </StyleMeasuresCardRightBorder>
        <StyleMeasuresCardRightBorder>
          <MeasuresCardPercent
            componentKey={component.key}
            branchLike={pullRequest}
            measurementType={MeasurementType.Duplication}
            label="overview.quality_gate.duplications"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Duplication, true),
              branchLike: pullRequest,
              listView: true,
            })}
            conditions={conditions}
            conditionMetric={MetricKey.new_duplicated_lines_density}
            overallConditionMetric={MetricKey.duplicated_lines_density}
            linesMetric={MetricKey.new_lines}
            measures={measures}
            useDiffMetric
            showRequired
          />
        </StyleMeasuresCardRightBorder>
        <div>
          <MeasuresCardNumber
            label={
              newSecurityHotspots === '1'
                ? 'issue.type.SECURITY_HOTSPOT'
                : 'issue.type.SECURITY_HOTSPOT.plural'
            }
            url={getComponentSecurityHotspotsUrl(component.key, pullRequest)}
            value={newSecurityHotspots}
            metric={MetricKey.new_security_hotspots}
            conditions={conditions}
            conditionMetric={MetricKey.new_security_hotspots_reviewed}
            showRequired
          />
        </div>
      </GridContainer>
    </Card>
  );
}
