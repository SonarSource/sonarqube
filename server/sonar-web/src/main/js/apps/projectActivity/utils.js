/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import moment from 'moment';
import { sortBy } from 'lodash';
import {
  cleanQuery,
  parseAsDate,
  parseAsString,
  serializeDate,
  serializeString
} from '../../helpers/query';
import { translate } from '../../helpers/l10n';
import type { Analysis, MeasureHistory, Query } from './types';
import type { RawQuery } from '../../helpers/query';
import type { Serie } from '../../components/charts/AdvancedTimeline';

export const EVENT_TYPES = ['VERSION', 'QUALITY_GATE', 'QUALITY_PROFILE', 'OTHER'];
export const GRAPH_TYPES = ['overview', 'coverage', 'duplications'];
export const GRAPHS_METRICS = {
  overview: ['bugs', 'code_smells', 'vulnerabilities'],
  coverage: ['uncovered_lines', 'lines_to_cover'],
  duplications: ['duplicated_lines', 'ncloc']
};

export const activityQueryChanged = (prevQuery: Query, nextQuery: Query): boolean =>
  prevQuery.category !== nextQuery.category ||
  prevQuery.from !== nextQuery.from ||
  prevQuery.to !== nextQuery.to;

export const datesQueryChanged = (prevQuery: Query, nextQuery: Query): boolean =>
  prevQuery.from !== nextQuery.from || prevQuery.to !== nextQuery.to;

export const historyQueryChanged = (prevQuery: Query, nextQuery: Query): boolean =>
  prevQuery.graph !== nextQuery.graph;

export const generateCoveredLinesMetric = (
  uncoveredLines: MeasureHistory,
  measuresHistory: Array<MeasureHistory>,
  style: string
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
    style,
    translatedName: translate('project_activity.custom_metric.covered_lines')
  };
};

export const generateSeries = (
  measuresHistory: Array<MeasureHistory>,
  graph: string,
  dataType: string
): Array<Serie> =>
  measuresHistory.map(measure => {
    if (measure.metric === 'uncovered_lines') {
      return generateCoveredLinesMetric(
        measure,
        measuresHistory,
        GRAPHS_METRICS[graph].indexOf(measure.metric)
      );
    }
    return {
      name: measure.metric,
      translatedName: translate('metric', measure.metric, 'name'),
      style: GRAPHS_METRICS[graph].indexOf(measure.metric),
      data: measure.history.map(analysis => ({
        x: analysis.date,
        y: dataType === 'LEVEL' ? analysis.value : Number(analysis.value)
      }))
    };
  });

export const getAnalysesByVersionByDay = (
  analyses: Array<Analysis>
): Array<{
  version: ?string,
  key: ?string,
  byDay: { [string]: Array<Analysis> }
}> =>
  analyses.reduce((acc, analysis) => {
    if (acc.length === 0) {
      acc.push({ version: undefined, key: undefined, byDay: {} });
    }
    const currentVersion = acc[acc.length - 1];
    const day = moment(analysis.date).startOf('day').valueOf().toString();
    if (!currentVersion.byDay[day]) {
      currentVersion.byDay[day] = [];
    }
    const sortedEvents = sortBy(
      analysis.events,
      // versions last
      event => (event.category === 'VERSION' ? 1 : 0),
      // then the rest sorted by category
      'category'
    );
    currentVersion.byDay[day].push({ ...analysis, events: sortedEvents });

    const lastEvent = sortedEvents[sortedEvents.length - 1];
    if (lastEvent && lastEvent.category === 'VERSION') {
      currentVersion.version = lastEvent.name;
      currentVersion.key = lastEvent.key;
      acc.push({ version: undefined, key: undefined, byDay: {} });
    }
    return acc;
  }, []);

const parseGraph = (value?: string): string => {
  const graph = parseAsString(value);
  return GRAPH_TYPES.includes(graph) ? graph : 'overview';
};

const serializeGraph = (value: string): ?string => (value === 'overview' ? undefined : value);

export const parseQuery = (urlQuery: RawQuery): Query => ({
  category: parseAsString(urlQuery['category']),
  from: parseAsDate(urlQuery['from']),
  graph: parseGraph(urlQuery['graph']),
  project: parseAsString(urlQuery['id']),
  to: parseAsDate(urlQuery['to'])
});

export const serializeQuery = (query: Query): RawQuery =>
  cleanQuery({
    category: serializeString(query.category),
    from: serializeDate(query.from),
    project: serializeString(query.project),
    to: serializeDate(query.to)
  });

export const serializeUrlQuery = (query: Query): RawQuery => {
  return cleanQuery({
    category: serializeString(query.category),
    from: serializeDate(query.from),
    graph: serializeGraph(query.graph),
    id: serializeString(query.project),
    to: serializeDate(query.to)
  });
};
