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
import { Text, TextSize } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import {
  LightLabel,
  NoDataIcon,
  SnoozeCircleIcon,
  TextError,
  TrendUpCircleIcon,
  getTabPanelId,
  themeColor,
} from 'design-system';
import { isEmpty } from 'lodash';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import RatingComponent from '../../../app/components/metrics/RatingComponent';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { translate } from '../../../helpers/l10n';
import { findMeasure, isDiffMetric } from '../../../helpers/measures';
import { CodeScope, getComponentDrilldownUrl } from '../../../helpers/urls';
import { ApplicationPeriod } from '../../../types/application';
import { Branch } from '../../../types/branch-like';
import { isApplication } from '../../../types/component';
import { IssueStatus } from '../../../types/issues';
import { QualityGateStatus } from '../../../types/quality-gates';
import { CaycStatus, Component, MeasureEnhanced, Period, QualityGate } from '../../../types/types';
import { IssueMeasuresCardInner } from '../components/IssueMeasuresCardInner';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import MeasuresCardPercent from '../components/MeasuresCardPercent';
import {
  MeasurementType,
  Status,
  getConditionRequiredLabel,
  getMeasurementMetricKey,
} from '../utils';
import { GridContainer, StyleMeasuresCard, StyledConditionsCard } from './BranchSummaryStyles';
import { LeakPeriodInfo } from './LeakPeriodInfo';
import QualityGatePanel from './QualityGatePanel';

interface Props {
  grc: boolean;
  appLeak?: ApplicationPeriod;
  branch?: Branch;
  component: Component;
  loading?: boolean;
  measures: MeasureEnhanced[];
  period?: Period;
  qgStatuses?: QualityGateStatus[];
  qualityGate?: QualityGate;
}

export default function NewCodeMeasuresPanel(props: Readonly<Props>) {
  const { grc, appLeak, branch, component, measures, qgStatuses, period, loading, qualityGate } = props;
  const intl = useIntl();
  const isApp = isApplication(component.qualifier);

  const conditions = qgStatuses?.flatMap((qg) => qg.conditions) ?? [];

  const totalFailedCondition = qgStatuses?.flatMap((qg) => qg.failedConditions) ?? [];
  const totalNewFailedCondition = totalFailedCondition.filter((c) => isDiffMetric(c.metric));
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
        className="sw-font-regular sw-typo-sm sw-inline"
        text={getConditionRequiredLabel(newIssuesCondition, intl, true)}
      />
    ) : (
      <LightLabel className="sw-typo-sm">
        {getConditionRequiredLabel(newIssuesCondition, intl)}
      </LightLabel>
    );
  }

  let acceptedIssuesFooter = null;
  if (isEmpty(newAcceptedIssues)) {
    acceptedIssuesFooter = (
      <StyledInfoMessage className="sw-rounded-2 sw-p-4">
        <Text size={TextSize.Small}>
          <FormattedMessage id={`overview.run_analysis_to_compute.${component.qualifier}`} />
        </Text>
      </StyledInfoMessage>
    );
  } else {
    acceptedIssuesFooter = (
      <Text size={TextSize.Small} isSubdued>
        {intl.formatMessage({ id: 'overview.accepted_issues.help' })}
      </Text>
    );
  }

  const leakPeriod = isApp ? appLeak : period;

  const nonCaycProjectsInApp =
    isApp && qgStatuses
      ? qgStatuses
          .filter(({ caycStatus }) => caycStatus === CaycStatus.NonCompliant)
          .sort(({ name: a }, { name: b }) =>
            a.localeCompare(b, undefined, { sensitivity: 'base' }),
          )
      : [];

  const showCaycWarningInProject =
    qgStatuses &&
    qgStatuses.length === 1 &&
    qgStatuses[0].caycStatus === CaycStatus.NonCompliant &&
    qualityGate?.actions?.manageConditions &&
    !isApp;

  const showCaycWarningInApp = nonCaycProjectsInApp.length > 0;

  const noConditionsAndWarningForNewCode =
    totalNewFailedCondition.length === 0 && !showCaycWarningInApp && !showCaycWarningInProject;

  const isTwoColumns = !noConditionsAndWarningForNewCode;
  const isThreeColumns = noConditionsAndWarningForNewCode;

  return (
    <div id={getTabPanelId(CodeScope.New)}>
      {leakPeriod && (
        <div className="sw-flex sw-items-center sw-mr-6" data-spotlight-id="cayc-promotion-2">
          <Text isSubdued size={TextSize.Small} className="sw-mr-1">
            {grc ? "New Violations" : translate('overview.new_code')}:
          </Text>
          <Text isHighlighted size={TextSize.Small} className="sw-flex">
            <LeakPeriodInfo leakPeriod={leakPeriod} />
          </Text>
        </div>
      )}
      <GridContainer className="sw-relative sw-overflow-hidden sw-mt-8 js-summary">
        {!noConditionsAndWarningForNewCode && (
          <StyledConditionsCard className="sw-row-span-4 sw-col-span-4">
            <QualityGatePanel
              component={component}
              loading={loading}
              qgStatuses={qgStatuses}
              qualityGate={qualityGate}
              isNewCode
              showCaycWarningInApp={showCaycWarningInApp}
              showCaycWarningInProject={showCaycWarningInProject ?? false}
              totalFailedConditionLength={totalNewFailedCondition.length}
            />
          </StyledConditionsCard>
        )}
        <StyleMeasuresCard
          className={classNames({
            'sw-col-span-4': isTwoColumns,
            'sw-col-span-6': isThreeColumns,
          })}
        >
          <IssueMeasuresCardInner
            data-testid="overview__measures-new_issues"
            disabled={component.needIssueSync}
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
        </StyleMeasuresCard>
        <StyleMeasuresCard
          className={classNames({
            'sw-col-span-4': isTwoColumns,
            'sw-col-span-6': isThreeColumns,
          })}
        >
          <IssueMeasuresCardInner
            data-testid="overview__measures-accepted_issues"
            disabled={Boolean(component.needIssueSync) || isEmpty(newAcceptedIssues)}
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
        </StyleMeasuresCard>
        <StyleMeasuresCard className="sw-col-span-4">
          <MeasuresCardPercent
            branchLike={branch}
            componentKey={component.key}
            conditions={conditions}
            measures={measures}
            measurementType={MeasurementType.Coverage}
            label="overview.quality_gate.coverage"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Coverage, true),
              branchLike: branch,
              listView: true,
            })}
            conditionMetric={MetricKey.new_coverage}
            linesMetric={MetricKey.new_lines_to_cover}
            useDiffMetric
            showRequired={!isApp}
          />
        </StyleMeasuresCard>
        <StyleMeasuresCard className="sw-col-span-4">
          <MeasuresCardPercent
            branchLike={branch}
            componentKey={component.key}
            conditions={conditions}
            measures={measures}
            measurementType={MeasurementType.Duplication}
            label="overview.quality_gate.duplications"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Duplication, true),
              branchLike: branch,
              listView: true,
            })}
            conditionMetric={MetricKey.new_duplicated_lines_density}
            linesMetric={MetricKey.new_lines}
            useDiffMetric
            showRequired={!isApp}
          />
        </StyleMeasuresCard>
        <StyleMeasuresCard className="sw-col-span-4">
          <MeasuresCardNumber
            label={
              newSecurityHotspots === '1'
                ? 'issue.type.SECURITY_HOTSPOT'
                : 'issue.type.SECURITY_HOTSPOT.plural'
            }
            url={getComponentSecurityHotspotsUrl(component.key, branch, {
              inNewCodePeriod: 'true',
            })}
            value={newSecurityHotspots}
            metric={MetricKey.new_security_hotspots}
            conditions={conditions}
            conditionMetric={MetricKey.new_security_hotspots_reviewed}
            showRequired={!isApp}
            icon={
              newSecurityReviewRating ? (
                <RatingComponent
                  branchLike={branch}
                  componentKey={component.key}
                  ratingMetric={MetricKey.new_security_review_rating}
                  size="md"
                />
              ) : (
                <NoDataIcon size="md" />
              )
            }
          />
        </StyleMeasuresCard>
      </GridContainer>
    </div>
  );
}

const StyledInfoMessage = styled.div`
  background-color: ${themeColor('projectCardInfo')};
`;
