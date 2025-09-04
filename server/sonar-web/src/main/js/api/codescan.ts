/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import axios from 'axios';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { deleteRequest, post } from '../helpers/request';
import { Notification } from '../types/types';

export function getRawNotificationsForOrganization(key: string): Promise<Notification[]> {
  const params = { organizationId: key };
  return axios.get('/_codescan/notifications', { params }).catch(throwGlobalError);
}

export function deleteProject(uuid: string, deleteProject?: boolean): Promise<void> {
  return deleteRequest(`/_codescan/integrations/${uuid}?deleteProject=${deleteProject}`).catch(
    throwGlobalError,
  );
}

export function deleteBulkProjects(data: {
  analyzedBefore?: string;
  onProvisionedOnly?: boolean;
  organization: string;
  projects?: string;
  q?: string;
  qualifiers?: string;
}): Promise<void> {
  return post('/_codescan/integrations/projects/bulk_delete', data).catch(throwGlobalError);
}

export function getRedirectUrlForZoho(): Promise<string> {
  return axios.get('/_codescan/zoho/redirectUrl').catch(throwGlobalError);
}

// http.ts
export async function postChatRequest<
  TRes,
  TBody extends Record<string, unknown> = Record<string, unknown>,
>(url: string, body: TBody, init?: Omit<RequestInit, 'method' | 'body'>): Promise<TRes> {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(init?.headers || {}) },
    body: JSON.stringify(body),
    ...init,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}${text ? `: ${text}` : ''}`);
  }
  if (res.status === 204) return undefined as unknown as TRes;
  return (await res.json()) as TRes;
}

