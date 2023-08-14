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

import { TaskStatuses } from './tasks';

export type GithubStatusDisabled = {
  enabled: false;
  nextSync?: never;
  lastSync?: never;
};
export type GithubStatusEnabled = {
  enabled: true;
  nextSync?: { status: TaskStatuses.Pending | TaskStatuses.InProgress };
  lastSync?: {
    executionTimeMs: number;
    finishedAt: number;
    startedAt: number;
    warningMessage?: string;
  } & (
    | {
        status: TaskStatuses.Success;
        summary?: string;
        errorMessage?: never;
      }
    | {
        status: TaskStatuses.Canceled | TaskStatuses.Failed;
        summary?: never;
        errorMessage?: string;
      }
  );
};

export type GithubStatus = GithubStatusDisabled | GithubStatusEnabled;

export enum GitHubProvisioningStatus {
  Success = 'SUCCESS',
  Failed = 'FAILED',
}

type GitHubProvisioning =
  | {
      status: GitHubProvisioningStatus.Success;
      errorMessage?: never;
    }
  | {
      status: GitHubProvisioningStatus.Failed;
      errorMessage: string;
    };

export interface GitHubConfigurationStatus {
  application: {
    jit: GitHubProvisioning;
    autoProvisioning: GitHubProvisioning;
  };
  installations: {
    organization: string;
    jit: GitHubProvisioning;
    autoProvisioning: GitHubProvisioning;
  }[];
}
