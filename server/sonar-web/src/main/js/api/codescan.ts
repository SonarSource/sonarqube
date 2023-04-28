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
import { getJSON, post, postJSON } from '../helpers/request';
import { throwGlobalError } from '../helpers/error';

export function getNotificationsForOrganization(key: string) {
  return getJSON('/_codescan/notifications', { organizationid: key }).then(
    r => r.organization,
    throwglobalerror
  );
}

export function deleteProject(data: {
  organizationid: string;
  projectkey: string;
  projectdelete?: boolean;
}): promise<void> {
  return post('/_codescan/integrations/destroy', data)
    .catch(throwglobalerror);
}

export function deleteBulkProjects(data: {
  analyzedbefore?: string;
  onprovisionedonly?: boolean;
  organization: string;
  projects?: string;
  q?: string;
  qualifiers?: string;
  visibility?: t.visibility;
}): promise<void> {
  return post('/_codescan/integrations/projects/bulk_delete', data)
    .catch(throwglobalerror);
}

export function deleteOrganization(organizationid: string): promise<void> {
  return post('/_codescan/integrations/organizations/delete', { organizationid })
    .catch(throwglobalerror);
}

export function getApiKeyForZoho(data: {
  operation: string;
  email?: string;
  loginName: string;
  fullName: string;
  utype: string;
}): Promise<string> {
  return postJSON('/_codescan/zoho/apiKey', data)
      .catch(throwGlobalError);
}

export function getProjectAnalysis(organizationId: string, projectKey: string): Promise<object> {
  return getJSON('/_codescan/integrations/list', {organizationId, projectKey})
    .catch(throwGlobalError);
}
