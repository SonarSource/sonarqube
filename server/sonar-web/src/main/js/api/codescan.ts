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
import { deleteRequest, getJSON, post, postJSON } from '../helpers/request';
import { throwGlobalError } from '../helpers/error';
import { Visibility, Notification } from "../types/types";

export function getRawNotificationsForOrganization(key: string) : Promise<Notification> {
  return getJSON('/_codescan/notifications', { organizationId: key })
      .catch(throwGlobalError);
}

export function deleteProject(uuid: string, deleteProject?: boolean): Promise<void> {
  return deleteRequest(`/_codescan/integrations/${uuid}?deleteProject=${deleteProject}`)
      .catch(throwGlobalError);
}

export function deleteBulkProjects(data: {
  analyzedBefore?: string;
  onProvisionedOnly?: boolean;
  organization: string;
  projects?: string;
  q?: string;
  qualifiers?: string;
  visibility?: Visibility;
}): Promise<void> {
  return post('/_codescan/integrations/projects/bulk_delete', data)
      .catch(throwGlobalError);
}

export function getRedirectUrlForZoho(): Promise<string> {
  return getJSON('/_codescan/zoho/redirectUrl')
      .catch(throwGlobalError);
}
