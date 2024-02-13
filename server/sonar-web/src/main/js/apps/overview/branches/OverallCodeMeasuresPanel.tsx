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
import {
  MetricsRatingBadge,
  NoDataIcon,
  SnoozeCircleIcon,
  TextSubdued,
  getTabPanelId,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { findMeasure, formatMeasure, formatRating } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { Branch } from '../../../types/branch-like';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { isApplication } from '../../../types/component';
import { IssueStatus } from '../../../types/issues';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import MeasuresCard from '../components/MeasuresCard';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import { OverviewDisabledLinkTooltip } from '../components/OverviewDisabledLinkTooltip';
import { MeasuresTabs } from '../utils';
import MeasuresPanelPercentCards from './MeasuresPanelPercentCards';
import SoftwareImpactMeasureCard from './SoftwareImpactMeasureCard';

export interface OverallCodeMeasuresPanelProps {
  branch?: Branch;
  component: Component;
  measures: MeasureEnhanced[];
  qgStatuses?: QualityGateStatus[];
}

export default function OverallCodeMeasuresPanel(props: Readonly<OverallCodeMeasuresPanelProps>) {
  const { branch, qgStatuses, component, measures } = props;

  const intl = useIntl();

  const isApp = isApplication(component.qualifier);
  const conditions = qgStatuses?.flatMap((qg) => qg.conditions) ?? [];
  const acceptedIssues = findMeasure(measures, MetricKey.accepted_issues)?.value;
  const securityHotspots = findMeasure(measures, MetricKey.security_hotspots)?.value;
  const securityRating = findMeasure(measures, MetricKey.security_review_rating)?.value;

  return (
    <div id={getTabPanelId(MeasuresTabs.Overall)} className="sw-mt-6">
      <div className="sw-flex sw-gap-4">
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          softwareQuality={SoftwareQuality.Security}
          ratingMetricKey={MetricKey.security_rating}
          measures={measures}
        />
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          softwareQuality={SoftwareQuality.Reliability}
          ratingMetricKey={MetricKey.reliability_rating}
          measures={measures}
        />
        <SoftwareImpactMeasureCard
          branch={branch}
          component={component}
          softwareQuality={SoftwareQuality.Maintainability}
          ratingMetricKey={MetricKey.sqale_rating}
          measures={measures}
        />
      </div>

      <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-4">
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
          disabled={component.needIssueSync}
          tooltip={component.needIssueSync ? <OverviewDisabledLinkTooltip /> : null}
        >
          <TextSubdued className="sw-body-xs sw-mt-3">
            {intl.formatMessage({
              id: 'overview.accepted_issues.help',
            })}
          </TextSubdued>
        </MeasuresCard>

        <MeasuresPanelPercentCards
          branch={branch}
          component={component}
          measures={measures}
          conditions={conditions}
        />

        <MeasuresCardNumber
          label={
            securityHotspots === '1'
              ? 'issue.type.SECURITY_HOTSPOT'
              : 'issue.type.SECURITY_HOTSPOT.plural'
          }
          url={getComponentSecurityHotspotsUrl(component.key, {
            ...getBranchLikeQuery(branch),
          })}
          value={securityHotspots}
          metric={MetricKey.security_hotspots}
          conditions={conditions}
          conditionMetric={MetricKey.security_hotspots_reviewed}
          showRequired={!isApp}
          icon={
            securityRating ? (
              <MetricsRatingBadge
                label={securityRating}
                rating={formatRating(securityRating)}
                size="md"
              />
            ) : (
              <NoDataIcon size="md" />
            )
          }
        />
      </div>
    </div>
  );
}
