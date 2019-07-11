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
import { partition, sortBy } from 'lodash';
import { translate } from 'sonar-ui-common/helpers/l10n';

const PROVIDED_TYPES = ['homepage', 'ci', 'issue', 'scm', 'scm_dev'];
type NameAndType = Pick<T.ProjectLink, 'name' | 'type'>;

export function isProvided(link: Pick<T.ProjectLink, 'type'>) {
  return PROVIDED_TYPES.includes(link.type);
}

export function orderLinks<T extends NameAndType>(links: T[]) {
  const [provided, unknown] = partition<T>(links, isProvided);
  return [
    ...sortBy(provided, link => PROVIDED_TYPES.indexOf(link.type)),
    ...sortBy(unknown, link => link.name && link.name.toLowerCase())
  ];
}

export function getLinkName(link: NameAndType) {
  return isProvided(link) ? translate('project_links', link.type) : link.name;
}
