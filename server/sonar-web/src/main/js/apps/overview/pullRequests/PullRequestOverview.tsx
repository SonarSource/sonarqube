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
import { BasicSeparator, CenteredLayout, Spinner } from 'design-system';
import { uniq } from 'lodash';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { getMeasuresWithMetrics } from '../../../api/measures';
import { fetchQualityGate, getGateForProject } from '../../../api/quality-gates';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { useBranchStatusQuery } from '../../../queries/branch';
import { PullRequest } from '../../../types/branch-like';
import { Component, MeasureEnhanced, QualityGate } from '../../../types/types';
import MeasuresCardPanel from '../branches/MeasuresCardPanel';
import BranchQualityGate from '../components/BranchQualityGate';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import MetaTopBar from '../components/MetaTopBar';
import ZeroNewIssuesSimplificationGuide from '../components/ZeroNewIssuesSimplificationGuide';
import '../styles.css';
import { PR_METRICS, Status } from '../utils';
import SonarLintAd from './SonarLintAd';

interface Props {
  branchLike: PullRequest;
  component: Component;
}

export default function PullRequestOverview(props: Props) {
  const { component, branchLike } = props;
  const [isLoadingMeasures, setIsLoadingMeasures] = useState(false);
  const [measures, setMeasures] = useState<MeasureEnhanced[]>([]);
  const {
    data: { conditions, ignoredConditions, status } = {},
    isLoading: isLoadingBranchStatusesData,
  } = useBranchStatusQuery(component);
  const [isLoadingQualityGate, setIsLoadingQualityGate] = useState(false);
  const [qualityGate, setQualityGate] = useState<QualityGate>();
  const isLoading = isLoadingBranchStatusesData || isLoadingMeasures || isLoadingQualityGate;

  useEffect(() => {
    setIsLoadingMeasures(true);

    const metricKeys =
      conditions !== undefined
        ? // Also load metrics that apply to failing QG conditions.
          uniq([
            ...PR_METRICS,
            ...conditions.filter((c) => c.level !== Status.OK).map((c) => c.metric),
          ])
        : PR_METRICS;

    getMeasuresWithMetrics(component.key, metricKeys, getBranchLikeQuery(branchLike)).then(
      ({ component, metrics }) => {
        if (component.measures) {
          setIsLoadingMeasures(false);
          setMeasures(enhanceMeasuresWithMetrics(component.measures || [], metrics));
        }
      },
      () => {
        setIsLoadingMeasures(false);
      },
    );
  }, [branchLike, component.key, conditions]);

  useEffect(() => {
    async function fetchQualityGateDate() {
      setIsLoadingQualityGate(true);

      const qualityGate = await getGateForProject({ project: component.key });
      const qgDetails = await fetchQualityGate({ name: qualityGate.name });

      setQualityGate(qgDetails);
      setIsLoadingQualityGate(false);
    }

    fetchQualityGateDate();
  }, [component.key]);

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

  const failedConditions = conditions
    .filter((condition) => condition.level === Status.ERROR)
    .map((c) => enhanceConditionWithMeasure(c, measures))
    .filter(isDefined);

  return (
    <CenteredLayout>
      <div className="it__pr-overview sw-mt-12 sw-mb-8 sw-grid sw-grid-cols-12">
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
            failedConditions={failedConditions}
            measures={measures}
          />

          {qualityGate?.isBuiltIn && <ZeroNewIssuesSimplificationGuide qualityGate={qualityGate} />}

          <SonarLintAd status={status} />
        </div>
      </div>
    </CenteredLayout>
  );
}
