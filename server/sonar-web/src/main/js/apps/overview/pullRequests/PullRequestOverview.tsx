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
import { BasicSeparator, CenteredLayout, PageContentFontWrapper, Spinner } from 'design-system';
import { uniq } from 'lodash';
import * as React from 'react';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { useBranchStatusQuery } from '../../../queries/branch';
import { useComponentMeasuresWithMetricsQuery } from '../../../queries/component';
import { useComponentQualityGateQuery } from '../../../queries/quality-gates';
import { PullRequest } from '../../../types/branch-like';
import { Component } from '../../../types/types';
import BranchQualityGate from '../components/BranchQualityGate';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import MetaTopBar from '../components/MetaTopBar';
import ZeroNewIssuesSimplificationGuide from '../components/ZeroNewIssuesSimplificationGuide';
import '../styles.css';
import { PR_METRICS, Status } from '../utils';
import MeasuresCardPanel from './MeasuresCardPanel';
import SonarLintAd from './SonarLintAd';

interface Props {
  branchLike: PullRequest;
  component: Component;
}

export default function PullRequestOverview(props: Readonly<Props>) {
  const { component, branchLike } = props;

  const {
    data: { conditions, ignoredConditions, status } = {},
    isLoading: isLoadingBranchStatusesData,
  } = useBranchStatusQuery(component);

  const { data: qualityGate, isLoading: isLoadingQualityGate } = useComponentQualityGateQuery(
    component.key,
  );

  const { data: componentMeasures, isLoading: isLoadingMeasures } =
    useComponentMeasuresWithMetricsQuery(
      component.key,
      uniq([...PR_METRICS, ...(conditions?.map((c) => c.metric) ?? [])]),
      getBranchLikeQuery(branchLike),
      !isLoadingBranchStatusesData,
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

  const failedConditions = enhancedConditions.filter(
    (condition) => condition.level === Status.ERROR,
  );

  return (
    <CenteredLayout>
      <PageContentFontWrapper className="it__pr-overview sw-mt-12 sw-mb-8 sw-grid sw-grid-cols-12 sw-body-sm">
        <div className="sw-col-start-2 sw-col-span-10">
          <MetaTopBar branchLike={branchLike} measures={measures} />
          <BasicSeparator className="sw-my-4" />

          {ignoredConditions && <IgnoredConditionWarning />}

          {status && (
            <BranchQualityGate
              branchLike={branchLike}
              component={component}
              status={status}
              failedConditions={failedConditions}
            />
          )}

          <MeasuresCardPanel
            className="sw-flex-1"
            branchLike={branchLike}
            component={component}
            conditions={enhancedConditions}
            measures={measures}
          />

          {qualityGate?.isBuiltIn && <ZeroNewIssuesSimplificationGuide qualityGate={qualityGate} />}

          <SonarLintAd status={status} />
        </div>
      </PageContentFontWrapper>
    </CenteredLayout>
  );
}
