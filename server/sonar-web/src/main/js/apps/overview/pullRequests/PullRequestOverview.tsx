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
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { useBranchStatusQuery } from '../../../queries/branch';
import { PullRequest } from '../../../types/branch-like';
import { Component, MeasureEnhanced } from '../../../types/types';
import MeasuresCardPanel from '../branches/MeasuresCardPanel';
import BranchQualityGate from '../components/BranchQualityGate';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import MetaTopBar from '../components/MetaTopBar';
import SonarLintPromotion from '../components/SonarLintPromotion';
import '../styles.css';
import { PR_METRICS, Status } from '../utils';

interface Props {
  branchLike: PullRequest;
  component: Component;
}

export default function PullRequestOverview(props: Props) {
  const { component, branchLike } = props;
  const [loadingMeasure, setLoadingMeasure] = useState(false);
  const [measures, setMeasures] = useState<MeasureEnhanced[]>([]);
  const { data: { conditions, ignoredConditions, status } = {}, isLoading } =
    useBranchStatusQuery(component);
  const loading = isLoading || loadingMeasure;

  useEffect(() => {
    setLoadingMeasure(true);

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
          setLoadingMeasure(false);
          setMeasures(enhanceMeasuresWithMetrics(component.measures || [], metrics));
        }
      },
      () => {
        setLoadingMeasure(false);
      },
    );
  }, [branchLike, component.key, conditions]);

  if (loading) {
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
    .filter((condition) => condition.level === 'ERROR')
    .map((c) => enhanceConditionWithMeasure(c, measures))
    .filter(isDefined);

  return (
    <CenteredLayout>
      <div className="it__pr-overview sw-mt-12 sw-grid sw-grid-cols-12">
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

          <SonarLintPromotion qgConditions={conditions} />
        </div>
      </div>
    </CenteredLayout>
  );
}
