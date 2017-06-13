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
import type { Query } from './types';
import type { RawQuery } from '../../helpers/query';

export const GRAPH_TYPES = ['overview'];
export const GRAPHS_METRICS = { overview: ['bugs', 'vulnerabilities', 'code_smells'] };

export const parseQuery = (urlQuery: RawQuery): Query => ({
  category: parseAsString(urlQuery['category']),
  graph: parseAsString(urlQuery['graph']) || 'overview',
  project: parseAsString(urlQuery['id'])
});

export const serializeQuery = (query: Query): RawQuery =>
  cleanQuery({
    category: serializeString(query.category),
    project: serializeString(query.project)
  });

export const serializeUrlQuery = (query: Query): RawQuery => {
  const graph = query.graph === 'overview' ? '' : query.graph;
  return cleanQuery({
    category: serializeString(query.category),
    graph: serializeString(graph),
    id: serializeString(query.project)
  });
};
