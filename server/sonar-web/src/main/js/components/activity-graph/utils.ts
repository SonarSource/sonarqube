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

import { chunk, flatMap, groupBy, sortBy } from 'lodash';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  OLD_TO_NEW_TAXONOMY_METRICS_MAP,
} from '../../helpers/constants';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
import { localizeMetric } from '../../helpers/measures';
import { get, save } from '../../helpers/storage';
import { GraphType, MeasureHistory, ParsedAnalysis, Serie } from '../../types/project-activity';
import { Dict, Metric } from '../../types/types';

export const DEFAULT_GRAPH = GraphType.issues;

const GRAPHS_METRICS_DISPLAYED: Dict<string[]> = {
  [GraphType.issues]: [MetricKey.violations],
  [GraphType.coverage]: [MetricKey.lines_to_cover, MetricKey.uncovered_lines],
  [GraphType.duplications]: [MetricKey.ncloc, MetricKey.duplicated_lines],
};

const LEGACY_GRAPHS_METRICS_DISPLAYED: Dict<string[]> = {
  ...GRAPHS_METRICS_DISPLAYED,
  [GraphType.issues]: [MetricKey.bugs, MetricKey.code_smells, MetricKey.vulnerabilities],
};

const GRAPHS_METRICS: Dict<string[]> = {
  [GraphType.issues]: GRAPHS_METRICS_DISPLAYED[GraphType.issues].concat([
    MetricKey.reliability_rating,
    MetricKey.security_rating,
    MetricKey.sqale_rating,
  ]),
  [GraphType.coverage]: [...GRAPHS_METRICS_DISPLAYED[GraphType.coverage], MetricKey.coverage],
  [GraphType.duplications]: [
    ...GRAPHS_METRICS_DISPLAYED[GraphType.duplications],
    MetricKey.duplicated_lines_density,
  ],
};

const LEGACY_GRAPHS_METRICS: Dict<string[]> = {
  ...GRAPHS_METRICS,
  [GraphType.issues]: LEGACY_GRAPHS_METRICS_DISPLAYED[GraphType.issues].concat([
    MetricKey.reliability_rating,
    MetricKey.security_rating,
    MetricKey.sqale_rating,
  ]),
};

export const LINE_CHART_DASHES = [0, 3, 7];

export function isCustomGraph(graph: GraphType) {
  return graph === GraphType.custom;
}

export function getGraphTypes(ignoreCustom = false) {
  const graphs = [GraphType.issues, GraphType.coverage, GraphType.duplications];

  return ignoreCustom ? graphs : [...graphs, GraphType.custom];
}

export function hasDataValues(serie: Serie) {
  return serie.data.some((point) => Boolean(point.y || point.y === 0));
}

export function hasHistoryData(series: Serie[]) {
  return series.some((serie) => serie.data && serie.data.length > 1);
}

export function getSeriesMetricType(series: Serie[]) {
  return series.length > 0 ? series[0].type : MetricType.Integer;
}

export function getDisplayedHistoryMetrics(
  graph: GraphType,
  customMetrics: string[],
  isStandardMode = false,
) {
  if (isCustomGraph(graph)) {
    return customMetrics;
  }

  return isStandardMode ? LEGACY_GRAPHS_METRICS_DISPLAYED[graph] : GRAPHS_METRICS_DISPLAYED[graph];
}

export function getHistoryMetrics(
  graph: GraphType,
  customMetrics: string[],
  isStandardMode = false,
) {
  if (isCustomGraph(graph)) {
    return customMetrics;
  }
  return isStandardMode ? LEGACY_GRAPHS_METRICS[graph] : GRAPHS_METRICS[graph];
}

export function hasHistoryDataValue(series: Serie[]) {
  return series.some((serie) => serie.data && serie.data.length > 1 && hasDataValues(serie));
}

export function splitSeriesInGraphs(series: Serie[], maxGraph: number, maxSeries: number) {
  return flatMap(
    groupBy(series, (serie) => serie.type),
    (type) => chunk(type, maxSeries),
  ).slice(0, maxGraph);
}

export function generateCoveredLinesMetric(
  uncoveredLines: MeasureHistory,
  measuresHistory: MeasureHistory[],
): Serie {
  const linesToCover = measuresHistory.find(
    (measure) => measure.metric === MetricKey.lines_to_cover,
  );

  return {
    data: linesToCover
      ? uncoveredLines.history.map((analysis, idx) => ({
          x: analysis.date,
          y: Number(linesToCover.history[idx].value) - Number(analysis.value),
        }))
      : [],
    name: 'covered_lines',
    translatedName: translate('project_activity.custom_metric.covered_lines'),
    type: MetricType.Integer,
  };
}

export function generateSeries(
  measuresHistory: MeasureHistory[],
  graph: GraphType,
  metrics: Metric[],
  displayedMetrics: string[],
): Serie[] {
  if (displayedMetrics.length <= 0 || measuresHistory === undefined) {
    return [];
  }

  return sortBy(
    measuresHistory
      .filter((measure) => displayedMetrics.indexOf(measure.metric) >= 0)
      .map((measure) => {
        if (measure.metric === MetricKey.uncovered_lines && !isCustomGraph(graph)) {
          return generateCoveredLinesMetric(measure, measuresHistory);
        }
        const metric = findMetric(measure.metric, metrics);
        const isSoftwareQualityMetric = CCT_SOFTWARE_QUALITY_METRICS.includes(
          metric?.key as MetricKey,
        );
        return {
          data: measure.history.map((analysis) => {
            let { value } = analysis;

            if (value !== undefined && isSoftwareQualityMetric) {
              value = JSON.parse(value).total;
            }
            return {
              x: analysis.date,
              y: metric?.type === MetricType.Level ? value : Number(value),
            };
          }),
          name: measure.metric,
          translatedName: metric ? getLocalizedMetricName(metric) : localizeMetric(measure.metric),
          type: !metric || isSoftwareQualityMetric ? MetricType.Integer : metric.type,
        };
      }),
    (serie) =>
      displayedMetrics.indexOf(
        serie.name === 'covered_lines' ? MetricKey.uncovered_lines : serie.name,
      ),
  );
}

export function saveActivityGraph(
  namespace: string,
  project: string,
  graph: GraphType,
  metrics?: string[],
) {
  save(namespace, graph, project);

  if (isCustomGraph(graph) && metrics) {
    save(`${namespace}.custom`, metrics.join(','), project);
  }
}

export function getActivityGraph(
  namespace: string,
  project: string,
): { customGraphs: string[]; graph: GraphType } {
  const customGraphs = get(`${namespace}.custom`, project);

  return {
    graph: (get(namespace, project) as GraphType) || DEFAULT_GRAPH,
    customGraphs: customGraphs ? customGraphs.split(',') : [],
  };
}

export function getAnalysisEventsForDate(analyses: ParsedAnalysis[], date?: Date) {
  if (date) {
    const analysis = analyses.find((a) => a.date.valueOf() === date.valueOf());
    if (analysis) {
      return analysis.events;
    }
  }

  return [];
}

export function getDeprecatedTranslationKeyForTooltip(metric: MetricKey) {
  const quality = OLD_TO_NEW_TAXONOMY_METRICS_MAP[metric];

  let deprecatedKey = 'severity';
  if (quality) {
    deprecatedKey = 'quality';
  } else if (metric === MetricKey.confirmed_issues) {
    deprecatedKey = 'confirmed';
  }

  return `project_activity.custom_metric.deprecated.${deprecatedKey}`;
}

function findMetric(key: string, metrics: Metric[]) {
  return metrics.find((metric) => metric.key === key);
}
