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
import throwGlobalError from '../app/utils/throwGlobalError';
import { getJSON, post, postJSON } from '../helpers/request';

export function getProjectLinks(projectKey: string): Promise<T.ProjectLink[]> {
  return getJSON('/api/project_links/search', { projectKey }).then(r => r.links, throwGlobalError);
}

export function deleteLink(linkId: string) {
  return post('/api/project_links/delete', { id: linkId }).catch(throwGlobalError);
}

export function createLink(data: {
  name: string;
  projectKey: string;
  url: string;
}): Promise<T.ProjectLink> {
  return postJSON('/api/project_links/create', data).then(r => r.link, throwGlobalError);
}
