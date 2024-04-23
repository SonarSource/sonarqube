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
import React from 'react';
import {
  useComponent,
  useTopLevelComponentKey,
} from '../../../app/components/componentContext/withComponentContext';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import {
  DEFAULT_GRAPH,
  getActivityGraph,
  getHistoryMetrics,
  isCustomGraph,
} from '../../../components/activity-graph/utils';
import { useLocation, useRouter } from '../../../components/hoc/withRouter';
import { parseDate } from '../../../helpers/dates';
import useApplicationLeakQuery from '../../../queries/applications';
import { useBranchesQuery } from '../../../queries/branch';
import { useAllMeasuresHistoryQuery } from '../../../queries/measures';
import { useAllProjectAnalysesQuery } from '../../../queries/project-analyses';
import { getBranchLikeQuery } from '../../../sonar-aligned/helpers/branch-like';
import { isApplication, isPortfolioLike, isProject } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import { MeasureHistory, ParsedAnalysis } from '../../../types/project-activity';
import { Query, parseQuery, serializeUrlQuery } from '../utils';
import ProjectActivityAppRenderer from './ProjectActivityAppRenderer';

export interface State {
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  leakPeriodDate?: Date;
  graphLoading: boolean;
  initialized: boolean;
  measuresHistory: MeasureHistory[];
  query: Query;
}

export const PROJECT_ACTIVITY_GRAPH = 'sonar_project_activity.graph';

export function ProjectActivityApp() {
  const { query, pathname } = useLocation();
  const parsedQuery = parseQuery(query);
  const router = useRouter();
  const { component } = useComponent();
  const metrics = useMetrics();
  const { data: { branchLike } = {}, isFetching: isFetchingBranch } = useBranchesQuery(component);
  const enabled =
    component?.key !== undefined &&
    (isPortfolioLike(component?.qualifier) || (Boolean(branchLike) && !isFetchingBranch));

  const componentKey = useTopLevelComponentKey();
  const { data: appLeaks } = useApplicationLeakQuery(
    componentKey ?? '',
    isApplication(component?.qualifier),
  );

  const { data: analysesData, isLoading: isLoadingAnalyses } = useAllProjectAnalysesQuery(enabled);

  const { data: historyData, isLoading: isLoadingHistory } = useAllMeasuresHistoryQuery(
    componentKey,
    getBranchLikeQuery(branchLike),
    getHistoryMetrics(query.graph || DEFAULT_GRAPH, parsedQuery.customMetrics).join(','),
    enabled,
  );

  const analyses = React.useMemo(() => analysesData ?? [], [analysesData]);

  const measuresHistory = React.useMemo(
    () =>
      historyData?.measures?.map((measure) => ({
        metric: measure.metric,
        history: measure.history.map((historyItem) => ({
          date: parseDate(historyItem.date),
          value: historyItem.value,
        })),
      })) ?? [],
    [historyData],
  );

  const leakPeriodDate = React.useMemo(() => {
    if (appLeaks?.[0]) {
      return parseDate(appLeaks[0].date);
    } else if (isProject(component?.qualifier) && component?.leakPeriodDate !== undefined) {
      return parseDate(component.leakPeriodDate);
    }

    return undefined;
  }, [appLeaks, component?.leakPeriodDate, component?.qualifier]);

  const filteredMetrics = React.useMemo(() => {
    return Object.values(metrics).filter((metric) => {
      if (
        isPortfolioLike(component?.qualifier) &&
        metric.key === MetricKey.security_hotspots_reviewed
      ) {
        return false;
      }
      if (isProject(component?.qualifier) && metric.key === MetricKey.security_review_rating) {
        return false;
      }

      return true;
    });
  }, [component?.qualifier, metrics]);

  const handleUpdateQuery = (newQuery: Query) => {
    const q = serializeUrlQuery({
      ...parsedQuery,
      ...newQuery,
    });

    router.push({
      pathname,
      query: {
        ...q,
        ...getBranchLikeQuery(branchLike),
        id: component?.key,
      },
    });
  };

  return (
    component && (
      <ProjectActivityAppRenderer
        analyses={analyses}
        analysesLoading={isLoadingAnalyses}
        graphLoading={isLoadingHistory}
        leakPeriodDate={leakPeriodDate}
        initializing={isLoadingAnalyses || isLoadingHistory}
        measuresHistory={measuresHistory}
        metrics={filteredMetrics}
        project={component}
        onUpdateQuery={handleUpdateQuery}
        query={parsedQuery}
      />
    )
  );
}

export default function RedirectWrapper() {
  const { query } = useLocation();
  const { component } = useComponent();
  const router = useRouter();

  const filtered = React.useMemo(() => {
    for (const key in query) {
      if (key !== 'id' && query[key] !== '') {
        return true;
      }
    }
    return false;
  }, [query]);

  const { graph, customGraphs } = getActivityGraph(PROJECT_ACTIVITY_GRAPH, component?.key ?? '');
  const emptyCustomGraph = isCustomGraph(graph) && customGraphs.length <= 0;

  // if there is no filter, but there are saved preferences in the localStorage
  // also don't redirect to custom if there is no metrics selected for it
  const shouldRedirect = !filtered && graph != null && graph !== DEFAULT_GRAPH && !emptyCustomGraph;

  React.useEffect(() => {
    if (shouldRedirect) {
      const newQuery = { ...query, graph };

      if (isCustomGraph(newQuery.graph)) {
        router.replace({ query: { ...newQuery, custom_metrics: customGraphs.join(',') } });
      } else {
        router.replace({ query: newQuery });
      }
    }
  }, [shouldRedirect, router, query, graph, customGraphs]);

  return shouldRedirect ? null : <ProjectActivityApp />;
}
