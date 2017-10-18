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
import { checkStatus, corsRequest, getJSON, parseJSON, postJSON } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface Edition {
  key: string;
  name: string;
  desc: string;
  more_link: string;
  request_license_link: string;
  download_link: string;
}

export interface EditionsPerVersion {
  [version: string]: Edition[];
}

export interface EditionStatus {
  currentEditionKey?: string;
  nextEditionKey?: string;
  installationStatus:
    | 'NONE'
    | 'AUTOMATIC_IN_PROGRESS'
    | 'MANUAL_IN_PROGRESS'
    | 'AUTOMATIC_READY'
    | 'AUTOMATIC_FAILURE'
    | 'UNINSTALL_IN_PROGRESS';
}

export function getEditionStatus(): Promise<EditionStatus> {
  return getJSON('/api/editions/status').catch(throwGlobalError);
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

export function applyLicense(data: { license: string }): Promise<EditionStatus> {
  return postJSON('/api/editions/apply_license', data).catch(throwGlobalError);
}

export function uninstallEdition(): Promise<void | Response> {
  return postJSON('/api/editions/uninstall').catch(throwGlobalError);
}
