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

export interface AppContext {
  jwt: string;
}

export interface PullRequestContext {
  prId: string;
  repoUuid: string;
}

export interface ProjectData {
  analysisDate?: string;
  key: string;
  leakPeriodDate?: string;
  measures: { [key: string]: string };
  name: string;
  organization?: string;
}

export interface PullRequestData {
  analysisDate?: string;
  base?: string;
  branch?: string;
  key: string;
  isPullRequest: boolean;
  projectKey: string;
  status: {
    bugs: number;
    codeSmells: number;
    qualityGateStatus: string;
    vulnerabilities: number;
  };
  title?: string;
  url?: string;
}

export interface RepoComponent {
  key: string;
  name: string;
}

export interface RepositoryData extends RepoComponent {
  organization: string;
  analysisDate?: string;
  leakPeriodDate?: string;
  measures: RepositoryMeasure[];
}

export interface RepositoryMeasure {
  metric: string;
  periods?: Array<{ index: number; value: string }>;
  value?: string;
}

export type WidgetType =
  | 'repository-config'
  | 'repository-code-quality'
  | 'pullrequest-code-quality';
