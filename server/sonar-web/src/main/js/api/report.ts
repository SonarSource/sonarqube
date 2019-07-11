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
import { getJSON, post } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface ReportStatus {
  canDownload?: boolean;
  canSubscribe: boolean;
  componentFrequency?: string;
  globalFrequency: string;
  subscribed?: boolean;
}

export function getReportStatus(component: string): Promise<ReportStatus> {
  return getJSON('/api/governance_reports/status', { componentKey: component }).catch(
    throwGlobalError
  );
}

export function getReportUrl(component: string): string {
  return (
    (window as any).baseUrl +
    '/api/governance_reports/download?componentKey=' +
    encodeURIComponent(component)
  );
}

export function subscribe(component: string): Promise<void | Response> {
  return post('/api/governance_reports/subscribe', { componentKey: component }).catch(
    throwGlobalError
  );
}

export function unsubscribe(component: string): Promise<void | Response> {
  return post('/api/governance_reports/unsubscribe', { componentKey: component }).catch(
    throwGlobalError
  );
}
