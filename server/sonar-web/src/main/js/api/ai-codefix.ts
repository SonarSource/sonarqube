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
  parseJSON,
  get,
  request,
  checkStatus,
} from '../helpers/request';
import { throwGlobalError } from '~sonar-aligned/helpers/error';

export interface CodefixFixedFileResponse {
  jobId: string;
  fixedFileContent: string;
}

export interface CodefixCreatePrDraft {
  branchNamePrefix: string;
  baseBranch: string;
  pullRequestTitle: string;
  commitMessage: string;
  pullRequestDescription: string;
}

export interface CodefixCreatePrSubmit {
  baseBranch: string;
  pullRequestTitle: string;
  commitMessage: string;
  pullRequestDescription: string;
}

export interface CodefixStatusResponse {
  status: string;
}

const CODEFIX_BASE = '/_codescan/codefix';

export function queueCodeFix(data: {
    organizationKey: string;
    projectKey: string;
    issueKey: string;
}): Promise<void> {
  return postJSONBody(`${CODEFIX_BASE}/queue`, data);
}

export function getCodefixQuota(organizationKey: string): Promise<{
  dailyLimit: number;
  currentUsage: number;
}> {
  return get(`${CODEFIX_BASE}/quota`, { organizationKey })
    .then(parseJSON)
    .catch(throwGlobalError);
}

/**
 * Fetches the fixed file content for an issue from the internal codefix API.
 * Request is sent with same-origin credentials (session cookie) so the backend
 * can authenticate the user. The backend must restrict this API to logged-in
 * users and, when proxying to CodeScan, forward the request with the SQ token
 * so it is not callable from outside with a raw token.
 */
export function getCodefixFixedFile(issueKey: string): Promise<CodefixFixedFileResponse> {
  return get(`${CODEFIX_BASE}/fixed-file`, { issueKey }).then(parseJSON);
}

export function getCodefixCreatePrDraft(jobId: string): Promise<CodefixCreatePrDraft> {
  return get(`${CODEFIX_BASE}/create-pr-draft`, { jobId }).then(parseJSON);
}

export function createCodefixPr(
  jobId: string,
  body?: Partial<CodefixCreatePrSubmit>,
): Promise<void> {
  return new Promise((resolve, reject) => {
    request(`${CODEFIX_BASE}/create-pr/${encodeURIComponent(jobId)}`)
      .setMethod('POST')
      .setData(body ?? {}, true)
      .submit()
      .then((response) => checkStatus(response, false))
      .then(() => resolve(), reject);
  });
}

export function getCodefixStatus(issueKey: string): Promise<CodefixStatusResponse> {
  return get(`${CODEFIX_BASE}/get-status`, { issueKey }).then(parseJSON);
}
