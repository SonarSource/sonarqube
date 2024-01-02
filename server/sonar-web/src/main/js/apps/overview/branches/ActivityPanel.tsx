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
import * as React from 'react';
import GraphsHeader from '../../../components/activity-graph/GraphsHeader';
import GraphsHistory from '../../../components/activity-graph/GraphsHistory';
import {
  DEFAULT_GRAPH,
  generateSeries,
  getDisplayedHistoryMetrics,
  splitSeriesInGraphs,
} from '../../../components/activity-graph/utils';
import ActivityLink from '../../../components/common/ActivityLink';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { parseDate } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import {
  Analysis as AnalysisType,
  GraphType,
  MeasureHistory,
} from '../../../types/project-activity';
import { Component, Metric } from '../../../types/types';
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

const MAX_ANALYSES_NB = 5;
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

  const displayedMetrics = getDisplayedHistoryMetrics(graph, []);
  const series = generateSeries(measuresHistory, graph, metrics, displayedMetrics);
  const graphs = splitSeriesInGraphs(series, MAX_GRAPH_NB, MAX_SERIES_PER_GRAPH);
  let shownLeakPeriodDate;
  if (leakPeriodDate !== undefined) {
    const startDate = measuresHistory.reduce((oldest: Date, { history }) => {
      if (history.length > 0) {
        const date = parseDate(history[0].date);
        return oldest.getTime() > date.getTime() ? date : oldest;
      } else {
        return oldest;
      }
    }, new Date());
    shownLeakPeriodDate =
      startDate.getTime() > leakPeriodDate.getTime() ? startDate : leakPeriodDate;
  }

  const filteredAnalyses = analyses.filter((a) => a.events.length > 0).slice(0, MAX_ANALYSES_NB);

  return (
    <div className="overview-panel big-spacer-top" data-test="overview__activity-panel">
      <h2 className="overview-panel-title">{translate('overview.activity')}</h2>

      <div className="overview-panel-content">
        <div className="display-flex-row">
          <div className="display-flex-column flex-1">
            <div className="overview-panel-padded display-flex-column flex-1">
              <GraphsHeader graph={graph} metrics={metrics} updateGraph={props.onGraphChange} />
              <GraphsHistory
                analyses={[]}
                ariaLabel={translateWithParameters(
                  'overview.activity.graph_shows_data_for_x',
                  displayedMetrics.map((metricKey) => localizeMetric(metricKey)).join(', ')
                )}
                canShowDataAsTable={false}
                graph={graph}
                graphs={graphs}
                leakPeriodDate={shownLeakPeriodDate}
                loading={Boolean(loading)}
                measuresHistory={measuresHistory}
                series={series}
              />
            </div>

            <div className="overview-panel-padded bordered-top text-right">
              <ActivityLink branchLike={branchLike} component={component.key} graph={graph} />
            </div>
          </div>

          <div className="overview-panel-padded bordered-left width-30">
            <div data-test="overview__activity-analyses">
              <DeferredSpinner
                className="spacer-top spacer-left"
                loading={analyses.length === 0 && loading}
              >
                {analyses.length === 0 ? (
                  <p className="spacer-top spacer-left note">{translate('no_results')}</p>
                ) : (
                  <ul className="spacer-top spacer-left">
                    {filteredAnalyses.map((analysis) => (
                      <Analysis
                        analysis={analysis}
                        key={analysis.key}
                        qualifier={component.qualifier}
                      />
                    ))}
                  </ul>
                )}
              </DeferredSpinner>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default React.memo(ActivityPanel);
