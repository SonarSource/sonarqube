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

export const parseQuery = (urlQuery: RawQuery): Query => ({
  project: parseAsString(urlQuery['id']),
  category: parseAsString(urlQuery['category'])
});

export const serializeQuery = (query: Query): Query =>
  cleanQuery({
    project: serializeString(query.project),
    category: serializeString(query.category)
  });

export const serializeUrlQuery = (query: Query): RawQuery =>
  cleanQuery({
    id: serializeString(query.project),
    category: serializeString(query.category)
  });
