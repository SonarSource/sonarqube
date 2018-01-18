/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import { chunk, flatMap, groupBy, isEqual, sortBy } from 'lodash';
import {
  cleanQuery,
  parseAsArray,
  parseAsDate,
  parseAsString,
  serializeStringArray,
  serializeDate,
  serializeString
} from '../../helpers/query';
import { parseDate, startOfDay } from '../../helpers/dates';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
/*:: import type { Analysis, MeasureHistory, Metric, Query } from './types'; */
/*:: import type { RawQuery } from '../../helpers/query'; */
/*:: import type { Serie } from '../../components/charts/AdvancedTimeline'; */

export const EVENT_TYPES = ['VERSION', 'QUALITY_GATE', 'QUALITY_PROFILE', 'OTHER'];
export const APPLICATION_EVENT_TYPES = ['QUALITY_GATE', 'OTHER'];
export const DEFAULT_GRAPH = 'issues';
export const GRAPH_TYPES = ['issues', 'coverage', 'duplications', 'custom'];
export const GRAPHS_METRICS_DISPLAYED = {
  issues: ['bugs', 'code_smells', 'vulnerabilities'],
  coverage: ['lines_to_cover', 'uncovered_lines'],
  duplications: ['ncloc', 'duplicated_lines']
};
export const GRAPHS_METRICS = {
  issues: GRAPHS_METRICS_DISPLAYED['issues'].concat([
    'reliability_rating',
    'security_rating',
    'sqale_rating'
  ]),
  coverage: GRAPHS_METRICS_DISPLAYED['coverage'].concat(['coverage']),
  duplications: GRAPHS_METRICS_DISPLAYED['duplications'].concat(['duplicated_lines_density'])
};

export const datesQueryChanged = (prevQuery /*: Query */, nextQuery /*: Query */) =>
  !isEqual(prevQuery.from, nextQuery.from) || !isEqual(prevQuery.to, nextQuery.to);

export const activityQueryChanged = (prevQuery /*: Query */, nextQuery /*: Query */) =>
  prevQuery.category !== nextQuery.category || datesQueryChanged(prevQuery, nextQuery);

export const customMetricsChanged = (prevQuery /*: Query */, nextQuery /*: Query */) =>
  !isEqual(prevQuery.customMetrics, nextQuery.customMetrics);

export const hasDataValues = (serie /*: Serie */) =>
  serie.data.some(point => point.y || point.y === 0);

export const hasHistoryData = (series /*: Array<Serie> */) =>
  series.some(serie => serie.data && serie.data.length > 1);

export const hasHistoryDataValue = (series /*: Array<Serie> */) =>
  series.some(serie => serie.data && serie.data.length > 1 && hasDataValues(serie));

export function historyQueryChanged(prevQuery /*: Query */, nextQuery /*: Query */) /*: boolean */ {
  return prevQuery.graph !== nextQuery.graph;
}

export const isCustomGraph = (graph /*: string */) => graph === 'custom';

export const selectedDateQueryChanged = (prevQuery /*: Query */, nextQuery /*: Query */) =>
  !isEqual(prevQuery.selectedDate, nextQuery.selectedDate);

export const generateCoveredLinesMetric = (
  uncoveredLines /*: MeasureHistory */,
  measuresHistory /*: Array<MeasureHistory> */
) => {
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
};

function findMetric(key /*: string */, metrics /*:  Array<Metric> | { [string]: Metric } */) {
  if (Array.isArray(metrics)) {
    return metrics.find(metric => metric.key === key);
  }
  return metrics[key];
}

export function generateSeries(
  measuresHistory /*: Array<MeasureHistory> */,
  graph /*: string */,
  metrics /*:  Array<Metric> | { [string]: Metric } */,
  displayedMetrics /*: Array<string> */
) /*: Array<Serie> */ {
  if (displayedMetrics.length <= 0) {
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

export const splitSeriesInGraphs = (
  series /*: Array<Serie> */,
  maxGraph /*: number */,
  maxSeries /*: number */
) =>
  flatMap(groupBy(series, serie => serie.type), type => chunk(type, maxSeries)).slice(0, maxGraph);

export const getSeriesMetricType = (series /*: Array<Serie> */) =>
  series.length > 0 ? series[0].type : 'INT';

export function getAnalysesByVersionByDay(analyses /*: Array<Analysis> */, query /*: Query */) {
  return analyses.reduce((acc, analysis) => {
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

export const getDisplayedHistoryMetrics = (
  graph /*: string */,
  customMetrics /*: Array<string> */
) => (isCustomGraph(graph) ? customMetrics : GRAPHS_METRICS_DISPLAYED[graph]);

export const getHistoryMetrics = (graph /*: string */, customMetrics /*: Array<string> */) =>
  isCustomGraph(graph) ? customMetrics : GRAPHS_METRICS[graph];

const parseGraph = (value /*: ?string */) => {
  const graph = parseAsString(value);
  return GRAPH_TYPES.includes(graph) ? graph : DEFAULT_GRAPH;
};

const serializeGraph = (value /*: string */) => (value === DEFAULT_GRAPH ? undefined : value);

export function parseQuery(urlQuery /*: RawQuery */) /*: Query */ {
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

export function serializeQuery(query /*: Query */) /*: RawQuery */ {
  return cleanQuery({
    category: serializeString(query.category),
    from: serializeDate(query.from),
    project: serializeString(query.project),
    to: serializeDate(query.to)
  });
}

export function serializeUrlQuery(query /*: Query */) /*: RawQuery */ {
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
