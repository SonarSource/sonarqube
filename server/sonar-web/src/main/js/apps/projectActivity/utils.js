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
import { cleanQuery, parseAsString, serializeString } from '../../helpers/query';
import { translate } from '../../helpers/l10n';
import type { MeasureHistory, Query } from './types';
import type { RawQuery } from '../../helpers/query';

export const GRAPH_TYPES = ['overview', 'coverage', 'duplications', 'remediation'];
export const GRAPHS_METRICS = {
  overview: ['bugs', 'vulnerabilities', 'code_smells'],
  coverage: ['uncovered_lines', 'lines_to_cover'],
  duplications: ['duplicated_lines', 'ncloc'],
  remediation: ['reliability_remediation_effort', 'security_remediation_effort', 'sqale_index']
};
export const GRAPHS_METRICS_STYLE = {
  overview: { bugs: '0', code_smells: '1', vulnerabilities: '2' },
  coverage: {
    lines_to_cover: '1',
    uncovered_lines: '0'
  },
  duplications: {
    duplicated_lines: '0',
    ncloc: '1'
  },
  remediation: {
    reliability_remediation_effort: '0',
    security_remediation_effort: '2',
    sqale_index: '1'
  }
};

const parseGraph = (value?: string): string => {
  const graph = parseAsString(value);
  return GRAPH_TYPES.includes(graph) ? graph : 'overview';
};

const serializeGraph = (value: string): string => (value === 'overview' ? '' : value);

export const parseQuery = (urlQuery: RawQuery): Query => ({
  category: parseAsString(urlQuery['category']),
  graph: parseGraph(urlQuery['graph']),
  project: parseAsString(urlQuery['id'])
});

export const serializeQuery = (query: Query): RawQuery =>
  cleanQuery({
    category: serializeString(query.category),
    project: serializeString(query.project)
  });

export const serializeUrlQuery = (query: Query): RawQuery => {
  return cleanQuery({
    category: serializeString(query.category),
    graph: serializeGraph(query.graph),
    id: serializeString(query.project)
  });
};

export const generateCoveredLinesMetric = (
  uncoveredLines: MeasureHistory,
  measuresHistory: Array<MeasureHistory>
) => {
  const linesToCover = measuresHistory.find(measure => measure.metric === 'lines_to_cover');
  return {
    name: 'covered_lines',
    translatedName: translate('project_activity.custom_metric.covered_lines'),
    data: linesToCover
      ? uncoveredLines.history.map((analysis, idx) => ({
          x: analysis.date,
          y: Number(linesToCover.history[idx].value) - Number(analysis.value)
        }))
      : []
  };
};
