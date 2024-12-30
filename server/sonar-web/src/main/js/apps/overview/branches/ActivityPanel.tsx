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
import * as React from 'react';
import { BasicSeparator, Card } from '~design-system';
import { MetricKey } from '~sonar-aligned/types/metrics';
import GraphsHeader from '../../../components/activity-graph/GraphsHeader';
import GraphsHistory from '../../../components/activity-graph/GraphsHistory';
import {
  DEFAULT_GRAPH,
  generateSeries,
  getDisplayedHistoryMetrics,
  splitSeriesInGraphs,
} from '../../../components/activity-graph/utils';
import ActivityLink from '../../../components/common/ActivityLink';
import { parseDate } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { BranchLike } from '../../../types/branch-like';
import {
  Analysis as AnalysisType,
  GraphType,
  MeasureHistory,
} from '../../../types/project-activity';
import { Component, Metric } from '../../../types/types';
import { getAnalysisVariations } from '../utils';
import Analysis from './Analysis';

export interface ActivityPanelProps {
  analyses?: AnalysisType[];
  branchLike?: BranchLike;
  component: Pick<Component, 'key' | 'qualifier'>;
  graph?: GraphType;
  leakPeriodDate?: Date;
  loading?: boolean;
  measuresHistory: MeasureHistory[];
  metrics: Metric[];
  onGraphChange: (graph: GraphType) => void;
}

export const MAX_ANALYSES_NB = 5;
const MAX_GRAPH_NB = 2;
const MAX_SERIES_PER_GRAPH = 3;

export function ActivityPanel(props: ActivityPanelProps) {
  const {
    analyses = [],
    branchLike,
    component,
    graph = DEFAULT_GRAPH,
    leakPeriodDate,
    loading,
    measuresHistory,
    metrics,
  } = props;

  const { data: isStandardMode = false } = useStandardExperienceModeQuery();
  const displayedMetrics = getDisplayedHistoryMetrics(graph, [], isStandardMode);
  const series = generateSeries(measuresHistory, graph, metrics, displayedMetrics);
  const graphs = splitSeriesInGraphs(series, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH);
  let shownLeakPeriodDate;

  if (leakPeriodDate !== undefined) {
    const startDate = measuresHistory.reduce((oldest: Date, { history }) => {
      if (history.length > 0) {
        const date = parseDate(history[0].date);

        return oldest.getTime() > date.getTime() ? date : oldest;
      }

      return oldest;
    }, new Date());

    shownLeakPeriodDate =
      startDate.getTime() > leakPeriodDate.getTime() ? startDate : leakPeriodDate;
  }

  const displayedAnalyses = analyses.slice(0, MAX_ANALYSES_NB);

  const analysisVariations = React.useMemo(
    () =>
      getAnalysisVariations(
        measuresHistory,
        Math.min(analyses.length, MAX_ANALYSES_NB + 1),
      ).reverse(),
    [measuresHistory, analyses.length],
  );

  const qualityGateStatuses = React.useMemo(
    () =>
      measuresHistory
        .find(({ metric }) => metric === MetricKey.alert_status)
        ?.history.slice(-MAX_ANALYSES_NB)
        .reverse(),
    [measuresHistory],
  );

  return (
    <div>
      <h2 className="sw-pt-6 sw-pb-4 sw-typo-lg-semibold">{translate('overview.activity')}</h2>

      <Card className="sw-rounded-2" data-test="overview__activity-panel">
        <GraphsHeader graph={graph} metrics={metrics} onUpdateGraph={props.onGraphChange} />

        <GraphsHistory
          analyses={[]}
          ariaLabel={translateWithParameters(
            'overview.activity.graph_shows_data_for_x',
            displayedMetrics.map((metricKey) => localizeMetric(metricKey)).join(', '),
          )}
          canShowDataAsTable={false}
          graph={graph}
          graphs={graphs}
          leakPeriodDate={shownLeakPeriodDate}
          loading={Boolean(loading)}
          measuresHistory={measuresHistory}
          series={series}
        />

        <BasicSeparator className="sw-mb-4 sw-mt-16" />

        <Spinner isLoading={loading}>
          {displayedAnalyses.length === 0 ? (
            <p>{translate('no_results')}</p>
          ) : (
            displayedAnalyses.map((analysis, index) => (
              <div key={analysis.key}>
                <Analysis
                  analysis={analysis}
                  isFirstAnalysis={index === analyses.length - 1}
                  qualifier={component.qualifier}
                  qualityGateStatus={qualityGateStatuses?.[index]?.value}
                  variations={analysisVariations[index]}
                  organization={component.organization}
                />

                {index !== displayedAnalyses.length - 1 && <BasicSeparator className="sw-my-3" />}
              </div>
            ))
          )}
        </Spinner>

        <BasicSeparator className="sw-mt-4" />

        <div className="sw-flex sw-justify-center sw-pt-3">
          <ActivityLink branchLike={branchLike} component={component.key} graph={graph} />
        </div>
      </Card>
    </div>
  );
}

export default React.memo(ActivityPanel);
