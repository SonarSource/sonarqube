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
import { sortBy, uniq } from 'lodash';
import * as React from 'react';
import { getBranchLikeQuery, isMainBranch } from '~sonar-aligned/helpers/branch-like';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getApplicationDetails, getApplicationLeak } from '../../../api/application';
import { getMeasuresWithPeriodAndMetrics } from '../../../api/measures';
import { getProjectActivity } from '../../../api/projectActivity';
import { fetchQualityGate, getGateForProject } from '../../../api/quality-gates';
import { getAllTimeMachineData } from '../../../api/time-machine';
import {
  getActivityGraph,
  getHistoryMetrics,
  saveActivityGraph,
} from '../../../components/activity-graph/utils';
import { getBranchLikeDisplayName } from '../../../helpers/branch-like';
import { parseDate, toISO8601WithOffsetString } from '../../../helpers/dates';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import {
  extractStatusConditionsFromApplicationStatusChildProject,
  extractStatusConditionsFromProjectStatus,
} from '../../../helpers/qualityGates';
import { isDefined } from '../../../helpers/types';
import { useMeasuresAndLeakQuery } from '../../../queries/measures';
import {
  useApplicationQualityGateStatus,
  useProjectQualityGateStatus,
} from '../../../queries/quality-gates';
import { useStandardExperienceMode } from '../../../queries/settings';
import { ApplicationPeriod } from '../../../types/application';
import { Branch, BranchLike } from '../../../types/branch-like';
import { Analysis, GraphType, MeasureHistory } from '../../../types/project-activity';
import { QualityGateStatus, QualityGateStatusCondition } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, Metric, Period, QualityGate } from '../../../types/types';
import '../styles.css';
import { BRANCH_OVERVIEW_METRICS, HISTORY_METRICS_LIST, Status } from '../utils';
import BranchOverviewRenderer from './BranchOverviewRenderer';

interface Props {
  branch?: Branch;
  branchesEnabled?: boolean;
  component: Component;
}

export const BRANCH_OVERVIEW_ACTIVITY_GRAPH = 'sonar_branch_overview.graph';
export const NO_CI_DETECTED = 'undetected';

// Get all history data over the past year.
const FROM_DATE = toISO8601WithOffsetString(new Date().setFullYear(new Date().getFullYear() - 1));

export default function BranchOverview(props: Readonly<Props>) {
  const { component, branch, branchesEnabled } = props;
  const { data: isStandardMode = false } = useStandardExperienceMode();
  const { graph: initialGraph } = getActivityGraph(
    BRANCH_OVERVIEW_ACTIVITY_GRAPH,
    props.component.key,
  );
  const [graph, setGraph] = React.useState<GraphType>(initialGraph);
  const [loadingStatus, setLoadingStatus] = React.useState<boolean>(true);
  const [appLeak, setAppLeak] = React.useState<ApplicationPeriod | undefined>(undefined);
  const [measures, setMeasures] = React.useState<MeasureEnhanced[] | undefined>(undefined);
  const [metrics, setMetrics] = React.useState<Metric[] | undefined>(undefined);
  const [period, setPeriod] = React.useState<Period | undefined>(undefined);
  const [qgStatuses, setQgStatuses] = React.useState<QualityGateStatus[] | undefined>(undefined);
  const [loadingHistory, setLoadingHistory] = React.useState<boolean>(true);
  const [analyses, setAnalyses] = React.useState<Analysis[] | undefined>(undefined);
  const [detectedCIOnLastAnalysis, setDetectedCIOnLastAnalysis] = React.useState<
    boolean | undefined
  >(undefined);
  const [qualityGate, setQualityGate] = React.useState<QualityGate | undefined>(undefined);
  const [measuresHistory, setMeasuresHistory] = React.useState<MeasureHistory[] | undefined>(
    undefined,
  );

  const { data: projectQualityGateStatus } = useProjectQualityGateStatus(
    {
      projectKey: component.key,
      branchParameters: getBranchLikeQuery(branch),
    },
    { enabled: component.qualifier === ComponentQualifier.Project },
  );

  const { data: applicationQualityGateStatus } = useApplicationQualityGateStatus(
    { application: component.key, ...getBranchLikeQuery(branch) },
    { enabled: component.qualifier === ComponentQualifier.Application },
  );

  const { data: measuresAndLeak } = useMeasuresAndLeakQuery({
    componentKey: component.key,
    branchLike: branch,
    metricKeys:
      component.qualifier === ComponentQualifier.Project &&
      projectQualityGateStatus?.conditions !== undefined
        ? uniq([
            ...BRANCH_OVERVIEW_METRICS,
            ...projectQualityGateStatus.conditions.map((c) => c.metricKey),
          ])
        : BRANCH_OVERVIEW_METRICS,
  });

  const getEnhancedConditions = (
    conditions: QualityGateStatusCondition[],
    measures: MeasureEnhanced[],
  ) => {
    return (
      conditions
        // Enhance them with Metric information, which will be needed
        // to render the conditions properly.
        .map((c) => enhanceConditionWithMeasure(c, measures))
        // The enhancement will return undefined if it cannot find the
        // appropriate measure. Make sure we filter them out.
        .filter(isDefined)
    );
  };

  const loadApplicationStatus = React.useCallback(async () => {
    if (!measuresAndLeak || !applicationQualityGateStatus) {
      return;
    }
    const { component: componentMeasures, metrics, period } = measuresAndLeak;
    const appMeasures = componentMeasures.measures
      ? enhanceMeasuresWithMetrics(componentMeasures.measures, metrics)
      : [];

    const appBranchName =
      (branch && !isMainBranch(branch) && getBranchLikeDisplayName(branch)) || undefined;

    const appDetails = await getApplicationDetails(component.key, appBranchName);

    setLoadingStatus(true);
    // We also need to load the application leak periods separately.
    getApplicationLeak(component.key, appBranchName).then(
      (leaks) => {
        if (leaks && leaks.length > 0) {
          const sortedLeaks = sortBy(leaks, (leak) => {
            return new Date(leak.date);
          });
          setAppLeak(sortedLeaks[0]);
        }
      },
      () => {
        setAppLeak(undefined);
      },
    );

    // We need to load the measures for each project in an application
    // individually, in order to display all QG conditions correctly. Loading
    // them at the parent application level will not get all the necessary
    // information, unfortunately, as they are aggregated.
    Promise.all(
      applicationQualityGateStatus.projects.map((project) => {
        const projectDetails = appDetails.projects.find((p) => p.key === project.key);
        const projectBranchLike = projectDetails
          ? { isMain: projectDetails.isMain, name: projectDetails.branch, excludedFromPurge: false }
          : undefined;

        return loadMeasuresAndMeta(
          project.key,
          projectBranchLike,
          // Only load metrics that apply to failing QG conditions; we don't
          // need the others anyway.
          project.conditions.filter((c) => c.status !== 'OK').map((c) => c.metric),
        ).then(({ measures }) => ({
          measures,
          project,
          projectBranchLike,
        }));
      }),
    ).then(
      (results) => {
        const qgStatuses = results
          .map(({ measures = [], project, projectBranchLike }): QualityGateStatus => {
            const { key, name, status, caycStatus } = project;
            const conditions = extractStatusConditionsFromApplicationStatusChildProject(project);
            const enhancedConditions = getEnhancedConditions(conditions, measures);
            const failedConditions = enhancedConditions.filter((c) => c.level !== Status.OK);

            return {
              conditions: enhancedConditions,
              failedConditions,
              caycStatus,
              key,
              name,
              status,
              branchLike: projectBranchLike,
            };
          })
          .sort((a, b) => Math.sign(b.failedConditions.length - a.failedConditions.length));

        setQgStatuses(qgStatuses);
        setPeriod(period);
        setMetrics(metrics);
        setMeasures(appMeasures);
        setLoadingStatus(false);
      },
      () => {
        setQgStatuses(undefined);
        setLoadingStatus(false);
      },
    );
  }, [applicationQualityGateStatus, branch, component.key, measuresAndLeak]);

  const loadProjectStatus = React.useCallback(() => {
    const { key, name } = component;

    if (!measuresAndLeak || !projectQualityGateStatus) {
      return;
    }
    setLoadingStatus(true);
    const { component: componentMeasures, metrics, period } = measuresAndLeak;
    const projectMeasures = componentMeasures.measures
      ? enhanceMeasuresWithMetrics(componentMeasures.measures, metrics)
      : [];

    if (projectMeasures) {
      const { ignoredConditions, caycStatus, status } = projectQualityGateStatus;
      const conditions = extractStatusConditionsFromProjectStatus(projectQualityGateStatus);
      const enhancedConditions = getEnhancedConditions(conditions, projectMeasures);
      const failedConditions = enhancedConditions.filter((c) => c.level !== Status.OK);

      const qgStatus: QualityGateStatus = {
        ignoredConditions,
        caycStatus,
        conditions: enhancedConditions,
        failedConditions,
        key,
        name,
        status,
        branchLike: branch,
      };

      setMeasures(projectMeasures);
      setMetrics(metrics);
      setPeriod(period);
      setQgStatuses([qgStatus]);
    } else {
      setQgStatuses(undefined);
    }
    setLoadingStatus(false);
  }, [branch, component, measuresAndLeak, projectQualityGateStatus]);

  const loadProjectQualityGate = React.useCallback(async () => {
    const qualityGate = await getGateForProject({ project: component.key });
    const qgDetails = await fetchQualityGate({ name: qualityGate.name });
    setQualityGate(qgDetails);
  }, [component.key]);

  const loadMeasuresAndMeta = (
    componentKey: string,
    branchLike?: BranchLike,
    metricKeys: string[] = [],
  ) => {
    return getMeasuresWithPeriodAndMetrics(
      componentKey,
      metricKeys.length > 0 ? metricKeys : BRANCH_OVERVIEW_METRICS,
      getBranchLikeQuery(branchLike),
    ).then(({ component: { measures }, metrics, period }) => {
      return {
        measures: enhanceMeasuresWithMetrics(measures || [], metrics || []),
        metrics,
        period,
      };
    });
  };

  const loadHistoryMeasures = React.useCallback(() => {
    const graphMetrics = getHistoryMetrics(graph, [], isStandardMode);
    const metrics = uniq([...HISTORY_METRICS_LIST, ...graphMetrics]);

    return getAllTimeMachineData({
      ...getBranchLikeQuery(branch),
      from: FROM_DATE,
      component: component.key,
      metrics: metrics.join(),
    }).then(
      ({ measures }) => {
        setMeasuresHistory(
          measures.map((measure) => ({
            metric: measure.metric,
            history: measure.history.map((analysis) => ({
              date: parseDate(analysis.date),
              value: analysis.value,
            })),
          })),
        );
      },
      () => {},
    );
  }, [branch, component.key, graph]);

  const getTopLevelComponent = React.useCallback(() => {
    let current = component.breadcrumbs.length - 1;
    while (
      current > 0 &&
      !(
        [
          ComponentQualifier.Project,
          ComponentQualifier.Portfolio,
          ComponentQualifier.Application,
        ] as string[]
      ).includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }
    return component.breadcrumbs[current].key;
  }, [component.breadcrumbs]);

  const loadAnalyses = React.useCallback(() => {
    return getProjectActivity({
      ...getBranchLikeQuery(branch),
      project: getTopLevelComponent(),
      from: FROM_DATE,
    }).then(
      ({ analyses }) => {
        setAnalyses(analyses);
        setDetectedCIOnLastAnalysis(
          analyses.length > 0
            ? analyses[0].detectedCI !== undefined && analyses[0].detectedCI !== NO_CI_DETECTED
            : undefined,
        );
      },
      () => {},
    );
  }, [branch, getTopLevelComponent]);

  const loadHistory = React.useCallback(() => {
    setLoadingHistory(true);

    return Promise.all([loadHistoryMeasures(), loadAnalyses()]).then(
      doneLoadingHistory,
      doneLoadingHistory,
    );
  }, [loadAnalyses, loadHistoryMeasures]);

  const doneLoadingHistory = () => {
    setLoadingHistory(false);
  };

  const handleGraphChange = (graph: GraphType) => {
    setGraph(graph);
    saveActivityGraph(BRANCH_OVERVIEW_ACTIVITY_GRAPH, component.key, graph);
    setLoadingHistory(true);
    loadHistoryMeasures().then(doneLoadingHistory, doneLoadingHistory);
  };

  const loadStatus = React.useCallback(() => {
    if (component.qualifier === ComponentQualifier.Application) {
      loadApplicationStatus();
    } else {
      loadProjectStatus();
      loadProjectQualityGate();
    }
  }, [component.qualifier, loadApplicationStatus, loadProjectQualityGate, loadProjectStatus]);

  React.useEffect(() => {
    loadStatus();
    loadHistory();
  }, [branch, loadHistory, loadStatus]);

  const projectIsEmpty =
    !loadingStatus &&
    measures?.find((measure) =>
      ([MetricKey.lines, MetricKey.new_lines] as string[]).includes(measure.metric.key),
    ) === undefined;

  return (
    <BranchOverviewRenderer
      analyses={analyses}
      appLeak={appLeak}
      branch={branch}
      branchesEnabled={branchesEnabled}
      component={component}
      detectedCIOnLastAnalysis={detectedCIOnLastAnalysis}
      graph={graph}
      loadingHistory={loadingHistory}
      loadingStatus={loadingStatus}
      measures={measures}
      measuresHistory={measuresHistory}
      metrics={metrics}
      onGraphChange={handleGraphChange}
      period={period}
      projectIsEmpty={projectIsEmpty}
      qgStatuses={qgStatuses}
      qualityGate={qualityGate}
    />
  );
}
