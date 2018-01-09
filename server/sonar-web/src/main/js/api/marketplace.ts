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
import { checkStatus, corsRequest, getJSON, parseJSON, post, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface Edition {
  key: string;
  name: string;
  textDescription: string;
  homeUrl: string;
  licenseRequestUrl: string;
  downloadUrl: string;
}

export interface EditionsPerVersion {
  [version: string]: Edition[];
}

export interface EditionStatus {
  currentEditionKey?: string;
  nextEditionKey?: string;
  installError?: string;
  installationStatus:
    | 'NONE'
    | 'AUTOMATIC_IN_PROGRESS'
    | 'MANUAL_IN_PROGRESS'
    | 'AUTOMATIC_READY'
    | 'UNINSTALL_IN_PROGRESS';
}

export function getEditionStatus(): Promise<EditionStatus> {
  return getJSON('/api/editions/status');
}

export function getEditionsList(url: string): Promise<EditionsPerVersion> {
  return corsRequest(url)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

export function getLicensePreview(data: {
  license: string;
}): Promise<{
  nextEditionKey: string;
  previewStatus: 'NO_INSTALL' | 'AUTOMATIC_INSTALL' | 'MANUAL_INSTALL';
}> {
  return postJSON('/api/editions/preview', data).catch(throwGlobalError);
}

export function getFormData(): Promise<{ serverId: string; ncloc: number }> {
  return getJSON('/api/editions/form_data').catch(throwGlobalError);
}

export function applyLicense(data: { license: string }): Promise<EditionStatus> {
  return postJSON('/api/editions/apply_license', data).catch(throwGlobalError);
}

export function uninstallEdition(): Promise<void | Response> {
  return post('/api/editions/uninstall').catch(throwGlobalError);
}

export function dismissErrorMessage(): Promise<void | Response> {
  return post('/api/editions/clear_error_message').catch(throwGlobalError);
}
