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
import { NoDataIcon, SnoozeCircleIcon, TextSubdued, getTabPanelId } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import RatingComponent from '../../../app/components/metrics/RatingComponent';
import { findMeasure, isDiffMetric } from '../../../helpers/measures';
import { CodeScope, getComponentDrilldownUrl } from '../../../helpers/urls';
import { Branch } from '../../../types/branch-like';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { isApplication } from '../../../types/component';
import { IssueStatus } from '../../../types/issues';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, QualityGate } from '../../../types/types';
import MeasuresCard from '../components/MeasuresCard';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import MeasuresCardPercent from '../components/MeasuresCardPercent';
import { MeasurementType, getMeasurementMetricKey } from '../utils';
import { GridContainer, StyleMeasuresCard, StyledConditionsCard } from './BranchSummaryStyles';
import QualityGatePanel from './QualityGatePanel';
import SoftwareImpactMeasureCard from './SoftwareImpactMeasureCard';

export interface OverallCodeMeasuresPanelProps {
  branch?: Branch;
  component: Component;
  loading?: boolean;
  measures: MeasureEnhanced[];
  qgStatuses?: QualityGateStatus[];
  qualityGate?: QualityGate;
}

export default function OverallCodeMeasuresPanel(props: Readonly<OverallCodeMeasuresPanelProps>) {
  const { branch, qgStatuses, component, measures, loading, qualityGate } = props;

  const intl = useIntl();

  const isApp = isApplication(component.qualifier);
  const conditions = qgStatuses?.flatMap((qg) => qg.conditions) ?? [];
  const totalFailedCondition = qgStatuses?.flatMap((qg) => qg.failedConditions) ?? [];
  const totalOverallFailedCondition = totalFailedCondition.filter((c) => !isDiffMetric(c.metric));
  const acceptedIssues = findMeasure(measures, MetricKey.accepted_issues)?.value;
  const securityHotspots = findMeasure(measures, MetricKey.security_hotspots)?.value;
  const securityRating = findMeasure(measures, MetricKey.security_review_rating)?.value;

  const noConditionsAndWarningForOverallCode = totalOverallFailedCondition.length === 0;

  return (
    <GridContainer
      id={getTabPanelId(CodeScope.Overall)}
      className={classNames('sw-grid sw-gap-12 sw-relative sw-overflow-hidden js-summary', {
        'sw-grid-cols-3': noConditionsAndWarningForOverallCode,
        'sw-grid-cols-4': !noConditionsAndWarningForOverallCode,
      })}
    >
      {!noConditionsAndWarningForOverallCode && (
        <StyledConditionsCard className="sw-row-span-4">
          <QualityGatePanel
            component={component}
            loading={loading}
            qgStatuses={qgStatuses}
            qualityGate={qualityGate}
            totalFailedConditionLength={totalOverallFailedCondition.length}
          />
        </StyledConditionsCard>
      )}
      <StyleMeasuresCard>
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          conditions={conditions}
          softwareQuality={SoftwareQuality.Security}
          ratingMetricKey={MetricKey.security_rating}
          measures={measures}
        />
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          conditions={conditions}
          softwareQuality={SoftwareQuality.Reliability}
          ratingMetricKey={MetricKey.reliability_rating}
          measures={measures}
        />
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          conditions={conditions}
          softwareQuality={SoftwareQuality.Maintainability}
          ratingMetricKey={MetricKey.sqale_rating}
          measures={measures}
        />
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <MeasuresCard
          url={getComponentIssuesUrl(component.key, {
            ...getBranchLikeQuery(branch),
            issueStatuses: IssueStatus.Accepted,
          })}
          value={formatMeasure(acceptedIssues, MetricType.ShortInteger)}
          metric={MetricKey.accepted_issues}
          label="overview.accepted_issues"
          failed={false}
          icon={
            <SnoozeCircleIcon
              color={acceptedIssues === '0' ? 'overviewCardDefaultIcon' : 'overviewCardWarningIcon'}
            />
          }
        >
          <TextSubdued className="sw-body-xs sw-mt-3">
            {intl.formatMessage({
              id: 'overview.accepted_issues.help',
            })}
          </TextSubdued>
        </MeasuresCard>
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <MeasuresCardPercent
          branchLike={branch}
          componentKey={component.key}
          conditions={conditions}
          measures={measures}
          measurementType={MeasurementType.Coverage}
          label="overview.quality_gate.coverage"
          url={getComponentDrilldownUrl({
            componentKey: component.key,
            metric: getMeasurementMetricKey(MeasurementType.Coverage, false),
            branchLike: branch,
            listView: true,
          })}
          conditionMetric={MetricKey.coverage}
          linesMetric={MetricKey.lines_to_cover}
          showRequired={!isApp}
        />
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <MeasuresCardPercent
          branchLike={branch}
          componentKey={component.key}
          conditions={conditions}
          measures={measures}
          measurementType={MeasurementType.Duplication}
          label="overview.quality_gate.duplications"
          url={getComponentDrilldownUrl({
            componentKey: component.key,
            metric: getMeasurementMetricKey(MeasurementType.Duplication, false),
            branchLike: branch,
            listView: true,
          })}
          conditionMetric={MetricKey.duplicated_lines_density}
          linesMetric={MetricKey.lines}
          showRequired={!isApp}
        />
      </StyleMeasuresCard>
      <StyleMeasuresCard>
        <MeasuresCardNumber
          label={
            securityHotspots === '1'
              ? 'issue.type.SECURITY_HOTSPOT'
              : 'issue.type.SECURITY_HOTSPOT.plural'
          }
          url={getComponentSecurityHotspotsUrl(component.key, branch)}
          value={securityHotspots}
          metric={MetricKey.security_hotspots}
          conditions={conditions}
          conditionMetric={MetricKey.security_hotspots_reviewed}
          showRequired={!isApp}
          icon={
            securityRating ? (
              <RatingComponent
                branchLike={branch}
                componentKey={component.key}
                getTooltip={(rating) =>
                  intl.formatMessage({ id: `metric.security_review_rating.tooltip.${rating}` })
                }
                getLabel={(rating) =>
                  intl.formatMessage({ id: 'metric.has_rating_X' }, { 0: rating })
                }
                ratingMetric={MetricKey.security_review_rating}
                size="md"
              />
            ) : (
              <NoDataIcon size="md" />
            )
          }
        />
      </StyleMeasuresCard>
    </GridContainer>
  );
}
