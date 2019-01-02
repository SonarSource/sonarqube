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
import { each, isNil, omitBy } from 'lodash';
import { RawQuery } from '../../../helpers/query';

interface OwnProps {
  isFavorite?: boolean;
  organization?: { key: string };
  query: RawQuery;
}

export function getFilterUrl(ownProps: OwnProps, part: RawQuery) {
  const basePathName = ownProps.organization
    ? `/organizations/${ownProps.organization.key}/projects`
    : '/projects';
  const pathname = basePathName + (ownProps.isFavorite ? '/favorite' : '');
  const query: RawQuery = omitBy({ ...ownProps.query, ...part }, isNil);
  each(query, (value, key) => {
    if (Array.isArray(value)) {
      query[key] = value.join(',');
    }
  });
  return { pathname, query };
}
