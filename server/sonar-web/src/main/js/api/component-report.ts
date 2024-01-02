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
import { throwGlobalError } from '../helpers/error';
import { getJSON, post } from '../helpers/request';
import { getBaseUrl } from '../helpers/system';
import { ComponentReportStatus } from '../types/component-report';

export function getReportStatus(
  componentKey: string,
  branchKey?: string
): Promise<ComponentReportStatus> {
  return getJSON('/api/governance_reports/status', { componentKey, branchKey }).catch(
    throwGlobalError
  );
}

export function getReportUrl(componentKey: string, branchKey?: string): string {
  let url = `${getBaseUrl()}/api/governance_reports/download?componentKey=${encodeURIComponent(
    componentKey
  )}`;

  if (branchKey) {
    url += `&branchKey=${branchKey}`;
  }

  return url;
}

export function subscribeToEmailReport(
  componentKey: string,
  branchKey?: string
): Promise<void | Response> {
  return post('/api/governance_reports/subscribe', { componentKey, branchKey }).catch(
    throwGlobalError
  );
}

export function unsubscribeFromEmailReport(
  componentKey: string,
  branchKey?: string
): Promise<void | Response> {
  return post('/api/governance_reports/unsubscribe', { componentKey, branchKey }).catch(
    throwGlobalError
  );
}
