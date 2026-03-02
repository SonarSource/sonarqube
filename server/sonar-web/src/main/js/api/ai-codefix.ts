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
import {
  postJSONBody,
  getCSRFToken,
  parseJSON,
  post,
  checkStatus,
} from '../helpers/request';
import { getBaseUrl } from '../helpers/system';

export interface CodefixFixedFileResponse {
  jobId: string;
  fixedFileContent: string;
}

const CODEFIX_BASE = '/_codescan/codefix';

export function queueCodeFix(data: {
    organizationKey: string;
    projectKey: string;
    issueKey: string;
}): Promise<void> {
  return postJSONBody('/_codescan/codefix/queue', data);
}

/**
 * Fetches the fixed file content for an issue from the internal codefix API.
 * Request is sent with same-origin credentials (session cookie) so the backend
 * can authenticate the user. The backend must restrict this API to logged-in
 * users and, when proxying to CodeScan, forward the request with the SQ token
 * so it is not callable from outside with a raw token.
 */
export function getCodefixFixedFile(issueKey: string): Promise<CodefixFixedFileResponse> {
  const url = `${getBaseUrl()}${CODEFIX_BASE}/fixed-file?issueKey=${encodeURIComponent(issueKey)}`;
  return fetch(url, {
    method: 'GET',
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...getCSRFToken(),
    },
  })
    .then(checkStatus)
    .then(parseJSON);
}

export function createCodefixPr(jobId: string): Promise<void> {
  return post(`${CODEFIX_BASE}/create-pr/${encodeURIComponent(jobId)}`);
}

export function getCodefixStatus(issueKey: string): Promise<CodefixJobStatusResponse> {
  const url = `${getBaseUrl()}${CODEFIX_BASE}/get-status?issueKey=${encodeURIComponent(issueKey)}`;
  return fetch(url, {
    method: 'GET',
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...getCSRFToken(),
    },
  })
    .then(checkStatus)
    .then(parseJSON);
}
