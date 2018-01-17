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
import { getJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface ProjectLink {
  id: string;
  name: string;
  type: string;
  url: string;
}

export function getProjectLinks(projectKey: string): Promise<ProjectLink[]> {
  const url = '/api/project_links/search';
  const data = { projectKey };
  return getJSON(url, data).then(r => r.links, throwGlobalError);
}

export function deleteLink(linkId: string): Promise<void> {
  const url = '/api/project_links/delete';
  const data = { id: linkId };
  return post(url, data);
}

export function createLink(projectKey: string, name: string, url: string): Promise<any> {
  const apiURL = '/api/project_links/create';
  const data = { projectKey, name, url };
  return postJSON(apiURL, data).then(r => r.link);
}
