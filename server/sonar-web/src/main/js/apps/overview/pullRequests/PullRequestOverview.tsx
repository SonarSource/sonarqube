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
import { BasicSeparator, CenteredLayout, PageContentFontWrapper, Spinner } from 'design-system';
import { uniq } from 'lodash';
import * as React from 'react';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { useBranchStatusQuery } from '../../../queries/branch';
import { useMeasuresComponentQuery } from '../../../queries/measures';
import { useComponentQualityGateQuery } from '../../../queries/quality-gates';
import { PullRequest } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import QGStatus from '../branches/QualityGateStatus';
import { AnalysisStatus } from '../components/AnalysisStatus';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import LastAnalysisLabel from '../components/LastAnalysisLabel';
import '../styles.css';
import { PR_METRICS } from '../utils';
import MeasuresCardPanel from './MeasuresCardPanel';
import PullRequestMetaTopBar from './PullRequestMetaTopBar';
import SonarLintAd from './SonarLintAd';

interface Props {
  component: Component;
  pullRequest: PullRequest;
}

export default function PullRequestOverview(props: Readonly<Readonly<Props>>) {
  const { component, pullRequest } = props;

  const {
    data: { conditions, ignoredConditions, status } = {},
    isLoading: isLoadingBranchStatusesData,
  } = useBranchStatusQuery(component);

  const { data: qualityGate, isLoading: isLoadingQualityGate } = useComponentQualityGateQuery(
    component.key,
  );

  const { data: componentMeasures, isLoading: isLoadingMeasures } = useMeasuresComponentQuery(
    {
      componentKey: component.key,
      metricKeys: uniq([...PR_METRICS, ...(conditions?.map((c) => c.metric) ?? [])]),
      branchLike: pullRequest,
    },
    { enabled: !isLoadingBranchStatusesData },
  );

  const measures = componentMeasures
    ? enhanceMeasuresWithMetrics(
        componentMeasures.component.measures ?? [],
        componentMeasures.metrics,
      )
    : [];

  const isLoading = isLoadingBranchStatusesData || isLoadingMeasures || isLoadingQualityGate;

  if (isLoading) {
    return (
      <CenteredLayout>
        <div className="sw-p-6">
          <Spinner loading />
        </div>
      </CenteredLayout>
    );
  }

  if (conditions === undefined) {
    return null;
  }

  const enhancedConditions = conditions
    .map((c) => enhanceConditionWithMeasure(c, measures))
    .filter(isDefined);

  return (
    <CenteredLayout>
      <PageContentFontWrapper className="it__pr-overview sw-mt-12 sw-mb-8 sw-grid sw-grid-cols-12 sw-body-sm">
        <div className="sw-col-start-2 sw-col-span-10">
          <PullRequestMetaTopBar pullRequest={pullRequest} measures={measures} />
          <BasicSeparator className="sw-my-4" />

          {ignoredConditions && <IgnoredConditionWarning />}

          <div className="sw-flex sw-justify-between sw-items-start sw-my-6">
            <QGStatus status={status} />
            <LastAnalysisLabel analysisDate={pullRequest.analysisDate} />
          </div>

          <AnalysisStatus className="sw-mb-4" component={component} />

          <MeasuresCardPanel
            pullRequest={pullRequest}
            component={component}
            conditions={enhancedConditions}
            qualityGate={qualityGate}
            measures={measures}
          />

          <SonarLintAd status={status} />
        </div>
      </PageContentFontWrapper>
    </CenteredLayout>
  );
}
