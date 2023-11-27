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
import { startOfDay } from 'date-fns';
import { isEqual, uniq } from 'lodash';
import { DEFAULT_GRAPH } from '../../components/activity-graph/utils';
import { parseDate } from '../../helpers/dates';
import { MEASURES_REDIRECTION } from '../../helpers/measures';
import {
  cleanQuery,
  parseAsArray,
  parseAsDate,
  parseAsString,
  serializeDate,
  serializeString,
  serializeStringArray,
} from '../../helpers/query';
import { MetricKey } from '../../types/metrics';
import { GraphType, ParsedAnalysis } from '../../types/project-activity';
import { Dict, RawQuery } from '../../types/types';

export interface Query {
  category: string;
  customMetrics: string[];
  from?: Date;
  graph: GraphType;
  project: string;
  selectedDate?: Date;
  to?: Date;
}

export function activityQueryChanged(prevQuery: Query, nextQuery: Query) {
  return prevQuery.category !== nextQuery.category || datesQueryChanged(prevQuery, nextQuery);
}

export function customMetricsChanged(prevQuery: Query, nextQuery: Query) {
  return !isEqual(prevQuery.customMetrics, nextQuery.customMetrics);
}

export function datesQueryChanged(prevQuery: Query, nextQuery: Query) {
  return !isEqual(prevQuery.from, nextQuery.from) || !isEqual(prevQuery.to, nextQuery.to);
}

export function historyQueryChanged(prevQuery: Query, nextQuery: Query) {
  return prevQuery.graph !== nextQuery.graph;
}

export interface AnalysesByDay {
  byDay: Dict<ParsedAnalysis[]>;
  version: string | null;
  key: string | null;
}

export function getAnalysesByVersionByDay(
  analyses: ParsedAnalysis[],
  query: Pick<Query, 'category' | 'from' | 'to'>,
) {
  return analyses.reduce<AnalysesByDay[]>((acc, analysis) => {
    let currentVersion = acc[acc.length - 1];
    const versionEvent = analysis.events.find((event) => event.category === 'VERSION');
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

    const day = startOfDay(parseDate(analysis.date)).getTime().toString();

    let matchFilters = true;
    if (query.category || query.from || query.to) {
      const isAfterFrom = !query.from || analysis.date >= query.from;
      const isBeforeTo = !query.to || analysis.date <= query.to;
      const hasSelectedCategoryEvents =
        !query.category ||
        analysis.events.find((event) => event.category === query.category) != null;
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

export function parseQuery(urlQuery: RawQuery): Query {
  const parsedMetrics = parseAsArray(urlQuery['custom_metrics'], parseAsString<MetricKey>);
  const customMetrics = uniq(parsedMetrics.map((metric) => MEASURES_REDIRECTION[metric] ?? metric));

  return {
    category: parseAsString(urlQuery['category']),
    customMetrics,
    from: parseAsDate(urlQuery['from']),
    graph: parseGraph(urlQuery['graph']),
    project: parseAsString(urlQuery['id']),
    to: parseAsDate(urlQuery['to']),
    selectedDate: parseAsDate(urlQuery['selected_date']),
  };
}

export function serializeQuery(query: Query): RawQuery {
  return cleanQuery({
    category: serializeString(query.category),
    from: serializeDate(query.from),
    project: serializeString(query.project),
    to: serializeDate(query.to),
  });
}

export function serializeUrlQuery(query: Query): RawQuery {
  return cleanQuery({
    category: serializeString(query.category),
    custom_metrics: serializeStringArray(query.customMetrics),
    from: serializeDate(query.from),
    graph: serializeGraph(query.graph),
    id: serializeString(query.project),
    to: serializeDate(query.to),
    selected_date: serializeDate(query.selectedDate),
  });
}

function parseGraph(value?: string) {
  const graph = parseAsString(value);
  return Object.keys(GraphType).includes(graph) ? (graph as GraphType) : DEFAULT_GRAPH;
}

function serializeGraph(value?: GraphType) {
  return value === DEFAULT_GRAPH ? undefined : value;
}
