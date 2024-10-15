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
import { Spinner } from '@sonarsource/echoes-react';
import React from 'react';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { useComponent } from '../../../app/components/componentContext/withComponentContext';
import { useMetrics } from '../../../app/components/metrics/withMetricsContext';
import {
  DEFAULT_GRAPH,
  getActivityGraph,
  getHistoryMetrics,
  isCustomGraph,
} from '../../../components/activity-graph/utils';
import { mergeRatingMeasureHistory } from '../../../helpers/activity-graph';
import { SOFTWARE_QUALITY_RATING_METRICS } from '../../../helpers/constants';
import { parseDate } from '../../../helpers/dates';
import useApplicationLeakQuery from '../../../queries/applications';
import { useCurrentBranchQuery } from '../../../queries/branch';
import { useAllMeasuresHistoryQuery } from '../../../queries/measures';
import { useAllProjectAnalysesQuery } from '../../../queries/project-analyses';
import { useStandardExperienceMode } from '../../../queries/settings';
import { isApplication, isProject } from '../../../types/component';
import { MeasureHistory, ParsedAnalysis } from '../../../types/project-activity';
import { Query, parseQuery, serializeUrlQuery } from '../utils';
import ProjectActivityAppRenderer from './ProjectActivityAppRenderer';

export interface State {
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  graphLoading: boolean;
  initialized: boolean;
  leakPeriodDate?: Date;
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
  const { data: branchLike, isFetching: isFetchingBranch } = useCurrentBranchQuery(component);
  const enabled =
    component?.key !== undefined &&
    (isPortfolioLike(component?.qualifier) || (Boolean(branchLike) && !isFetchingBranch));

  const { data: appLeaks } = useApplicationLeakQuery(
    component?.key ?? '',
    isApplication(component?.qualifier),
  );

  const { data: analysesData, isLoading: isLoadingAnalyses } = useAllProjectAnalysesQuery(enabled);
  const { data: isStandardMode, isLoading: isLoadingStandardMode } = useStandardExperienceMode();

  const { data: historyData, isLoading: isLoadingHistory } = useAllMeasuresHistoryQuery(
    {
      component: component?.key,
      branchParams: getBranchLikeQuery(branchLike),
      metrics: getHistoryMetrics(query.graph || DEFAULT_GRAPH, parsedQuery.customMetrics).join(','),
    },
    { enabled },
  );

  const analyses = React.useMemo(() => analysesData ?? [], [analysesData]);

  const measuresHistory = React.useMemo(
    () =>
      isLoadingStandardMode
        ? []
        : mergeRatingMeasureHistory(historyData, parseDate, isStandardMode),
    [historyData, isStandardMode, isLoadingStandardMode],
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

  const firstSoftwareQualityRatingMetric = historyData?.measures.find((m) =>
    SOFTWARE_QUALITY_RATING_METRICS.includes(m.metric),
  );

  return (
    component && (
      <Spinner isLoading={isLoadingStandardMode}>
        <ProjectActivityAppRenderer
          analyses={analyses}
          isStandardMode={
            isStandardMode ||
            !firstSoftwareQualityRatingMetric ||
            firstSoftwareQualityRatingMetric.history.every((h) => h.value === undefined)
          }
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
      </Spinner>
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
