/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as startOfDay from 'date-fns/start_of_day';
import { chunk, flatMap, groupBy, isEqual, sortBy } from 'lodash';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { getLocalizedMetricName, translate } from 'sonar-ui-common/helpers/l10n';
import {
  cleanQuery,
  parseAsArray,
  parseAsDate,
  parseAsString,
  serializeDate,
  serializeString,
  serializeStringArray
} from 'sonar-ui-common/helpers/query';
import { get } from 'sonar-ui-common/helpers/storage';

export type ParsedAnalysis = T.Omit<T.Analysis, 'date'> & { date: Date };

export interface Query {
  category: string;
  customMetrics: string[];
  from?: Date;
  graph: string;
  project: string;
  selectedDate?: Date;
  to?: Date;
}

export interface Point {
  x: Date;
  y: number | string | undefined;
}

export interface Serie {
  data: Point[];
  name: string;
  translatedName: string;
  type: string;
}

export interface HistoryItem {
  date: Date;
  value?: string;
}

export interface MeasureHistory {
  metric: string;
  history: HistoryItem[];
}

export const EVENT_TYPES = ['VERSION', 'QUALITY_GATE', 'QUALITY_PROFILE', 'OTHER'];
export const APPLICATION_EVENT_TYPES = ['QUALITY_GATE', 'DEFINITION_CHANGE', 'OTHER'];
export const DEFAULT_GRAPH = 'issues';
export const GRAPH_TYPES = ['issues', 'coverage', 'duplications', 'custom'];
export const GRAPHS_METRICS_DISPLAYED: T.Dict<string[]> = {
  issues: ['bugs', 'code_smells', 'vulnerabilities'],
  coverage: ['lines_to_cover', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines']
};
export const GRAPHS_METRICS: T.Dict<string[]> = {
  issues: GRAPHS_METRICS_DISPLAYED['issues'].concat([
    'reliability_rating',
    'security_rating',
    'sqale_rating'
  ]),
  coverage: GRAPHS_METRICS_DISPLAYED['coverage'].concat(['coverage']),
  duplications: GRAPHS_METRICS_DISPLAYED['duplications'].concat(['duplicated_lines_density'])
};

export const PROJECT_ACTIVITY_GRAPH = 'sonar_project_activity.graph';
export const PROJECT_ACTIVITY_GRAPH_CUSTOM = 'sonar_project_activity.graph.custom';

export function activityQueryChanged(prevQuery: Query, nextQuery: Query) {
  return prevQuery.category !== nextQuery.category || datesQueryChanged(prevQuery, nextQuery);
}

export function customMetricsChanged(prevQuery: Query, nextQuery: Query) {
  return !isEqual(prevQuery.customMetrics, nextQuery.customMetrics);
}

export function datesQueryChanged(prevQuery: Query, nextQuery: Query) {
  return !isEqual(prevQuery.from, nextQuery.from) || !isEqual(prevQuery.to, nextQuery.to);
}

export function hasDataValues(serie: Serie) {
  return serie.data.some(point => Boolean(point.y || point.y === 0));
}

export function hasHistoryData(series: Serie[]) {
  return series.some(serie => serie.data && serie.data.length > 1);
}

export function hasHistoryDataValue(series: Serie[]) {
  return series.some(serie => serie.data && serie.data.length > 1 && hasDataValues(serie));
}

export function historyQueryChanged(prevQuery: Query, nextQuery: Query) {
  return prevQuery.graph !== nextQuery.graph;
}

export function isCustomGraph(graph: string) {
  return graph === 'custom';
}

export function selectedDateQueryChanged(prevQuery: Query, nextQuery: Query) {
  return !isEqual(prevQuery.selectedDate, nextQuery.selectedDate);
}

export function generateCoveredLinesMetric(
  uncoveredLines: MeasureHistory,
  measuresHistory: MeasureHistory[]
) {
  const linesToCover = measuresHistory.find(measure => measure.metric === 'lines_to_cover');
  return {
    data: linesToCover
      ? uncoveredLines.history.map((analysis, idx) => ({
          x: analysis.date,
          y: Number(linesToCover.history[idx].value) - Number(analysis.value)
        }))
      : [],
    name: 'covered_lines',
    translatedName: translate('project_activity.custom_metric.covered_lines'),
    type: 'INT'
  };
}

function findMetric(key: string, metrics: T.Metric[] | T.Dict<T.Metric>) {
  if (Array.isArray(metrics)) {
    return metrics.find(metric => metric.key === key);
  }
  return metrics[key];
}

export function generateSeries(
  measuresHistory: MeasureHistory[],
  graph: string,
  metrics: T.Metric[] | T.Dict<T.Metric>,
  displayedMetrics: string[]
): Serie[] {
  if (displayedMetrics.length <= 0 || typeof measuresHistory === 'undefined') {
    return [];
  }
  return sortBy(
    measuresHistory
      .filter(measure => displayedMetrics.indexOf(measure.metric) >= 0)
      .map(measure => {
        if (measure.metric === 'uncovered_lines' && !isCustomGraph(graph)) {
          return generateCoveredLinesMetric(measure, measuresHistory);
        }
        const metric = findMetric(measure.metric, metrics);
        return {
          data: measure.history.map(analysis => ({
            x: analysis.date,
            y: metric && metric.type === 'LEVEL' ? analysis.value : Number(analysis.value)
          })),
          name: measure.metric,
          translatedName: metric ? getLocalizedMetricName(metric) : measure.metric,
          type: metric ? metric.type : 'INT'
        };
      }),
    serie =>
      displayedMetrics.indexOf(serie.name === 'covered_lines' ? 'uncovered_lines' : serie.name)
  );
}

export function splitSeriesInGraphs(series: Serie[], maxGraph: number, maxSeries: number) {
  return flatMap(groupBy(series, serie => serie.type), type => chunk(type, maxSeries)).slice(
    0,
    maxGraph
  );
}

export function getSeriesMetricType(series: Serie[]) {
  return series.length > 0 ? series[0].type : 'INT';
}

interface AnalysesByDay {
  byDay: T.Dict<ParsedAnalysis[]>;
  version: string | null;
  key: string | null;
}

export function getAnalysesByVersionByDay(analyses: ParsedAnalysis[], query: Query) {
  return analyses.reduce<AnalysesByDay[]>((acc, analysis) => {
    let currentVersion = acc[acc.length - 1];
    const versionEvent = analysis.events.find(event => event.category === 'VERSION');
    if (versionEvent) {
      const newVersion = { version: versionEvent.name, key: versionEvent.key, byDay: {} };
      if (!currentVersion || Object.keys(currentVersion.byDay).length > 0) {
        acc.push(newVersion);
      } else {
        acc[acc.length - 1] = newVersion;
      }
      currentVersion = newVersion;
    } else if (!currentVersion) {
      // APPs don't have version events, so let's create a fake one
      currentVersion = { version: null, key: null, byDay: {} };
      acc.push(currentVersion);
    }

    const day = startOfDay(parseDate(analysis.date))
      .getTime()
      .toString();

    let matchFilters = true;
    if (query.category || query.from || query.to) {
      const isAfterFrom = !query.from || analysis.date >= query.from;
      const isBeforeTo = !query.to || analysis.date <= query.to;
      const hasSelectedCategoryEvents =
        !query.category || analysis.events.find(event => event.category === query.category) != null;
      matchFilters = isAfterFrom && isBeforeTo && hasSelectedCategoryEvents;
    }

    if (matchFilters) {
      if (!currentVersion.byDay[day]) {
        currentVersion.byDay[day] = [];
      }
      currentVersion.byDay[day].push(analysis);
    }
    return acc;
  }, []);
}

export function getDisplayedHistoryMetrics(graph: string, customMetrics: string[]) {
  return isCustomGraph(graph) ? customMetrics : GRAPHS_METRICS_DISPLAYED[graph];
}

export function getHistoryMetrics(graph: string, customMetrics: string[]) {
  return isCustomGraph(graph) ? customMetrics : GRAPHS_METRICS[graph];
}

export function getProjectActivityGraph(project: string) {
  const customGraphs = get(PROJECT_ACTIVITY_GRAPH_CUSTOM, project);
  return {
    graph: get(PROJECT_ACTIVITY_GRAPH, project) || 'issues',
    customGraphs: customGraphs ? customGraphs.split(',') : []
  };
}

function parseGraph(value?: string) {
  const graph = parseAsString(value);
  return GRAPH_TYPES.includes(graph) ? graph : DEFAULT_GRAPH;
}

function serializeGraph(value: string) {
  return value === DEFAULT_GRAPH ? undefined : value;
}

export function parseQuery(urlQuery: T.RawQuery): Query {
  return {
    category: parseAsString(urlQuery['category']),
    customMetrics: parseAsArray(urlQuery['custom_metrics'], parseAsString),
    from: parseAsDate(urlQuery['from']),
    graph: parseGraph(urlQuery['graph']),
    project: parseAsString(urlQuery['id']),
    to: parseAsDate(urlQuery['to']),
    selectedDate: parseAsDate(urlQuery['selected_date'])
  };
}

export function serializeQuery(query: Query): T.RawQuery {
  return cleanQuery({
    category: serializeString(query.category),
    from: serializeDate(query.from),
    project: serializeString(query.project),
    to: serializeDate(query.to)
  });
}

export function serializeUrlQuery(query: Query): T.RawQuery {
  return cleanQuery({
    category: serializeString(query.category),
    custom_metrics: serializeStringArray(query.customMetrics),
    from: serializeDate(query.from),
    graph: serializeGraph(query.graph),
    id: serializeString(query.project),
    to: serializeDate(query.to),
    selected_date: serializeDate(query.selectedDate)
  });
}
