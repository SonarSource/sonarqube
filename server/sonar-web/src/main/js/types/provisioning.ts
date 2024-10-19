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
import { TaskStatuses } from './tasks';

export type GithubStatusDisabled = {
  enabled: false;
  lastSync?: never;
  nextSync?: never;
};
export interface GithubStatusEnabled extends AlmSyncStatus {
  enabled: true;
}

export interface AlmSyncStatus {
  lastSync?: {
    executionTimeMs: number;
    finishedAt: number;
    startedAt: number;
    warningMessage?: string;
  } & (
    | {
        errorMessage?: never;
        status: TaskStatuses.Success;
        summary?: string;
      }
    | {
        errorMessage?: string;
        status: TaskStatuses.Canceled | TaskStatuses.Failed;
        summary?: never;
      }
  );
  nextSync?: { status: TaskStatuses.Pending | TaskStatuses.InProgress };
}

export type GithubStatus = GithubStatusDisabled | GithubStatusEnabled;

export enum GitHubProvisioningStatus {
  Success = 'SUCCESS',
  Failed = 'FAILED',
}

type GitHubProvisioning =
  | {
      errorMessage?: never;
      status: GitHubProvisioningStatus.Success;
    }
  | {
      errorMessage: string;
      status: GitHubProvisioningStatus.Failed;
    };

export interface GitHubConfigurationStatus {
  application: {
    autoProvisioning: GitHubProvisioning;
    jit: GitHubProvisioning;
  };
  installations: {
    autoProvisioning: GitHubProvisioning;
    jit: GitHubProvisioning;
    organization: string;
  }[];
}

export interface DevopsRolesMapping {
  readonly baseRole?: boolean;
  readonly id: string;
  permissions: {
    admin: boolean;
    codeViewer: boolean;
    issueAdmin: boolean;
    scan: boolean;
    securityHotspotAdmin: boolean;
    user: boolean;
  };
  readonly role: string;
}

export interface GitLabConfigurationCreateBody
  extends Pick<GitlabConfiguration, 'applicationId' | 'synchronizeGroups' | 'url'> {
  secret: string;
}

export type GitLabConfigurationUpdateBody = {
  allowUsersToSignUp?: boolean;
  allowedGroups?: string[];
  applicationId?: string;
  enabled?: boolean;
  provisioningToken?: string;
  provisioningType?: ProvisioningType;
  secret?: string;
  synchronizeGroups?: boolean;
  url?: string;
};

export type GitlabConfiguration = {
  allowUsersToSignUp: boolean;
  allowedGroups: string[];
  applicationId: string;
  enabled: boolean;
  errorMessage?: string;
  id: string;
  isProvisioningTokenSet: boolean;
  provisioningType: ProvisioningType;
  synchronizeGroups: boolean;
  url: string;
};

export enum ProvisioningType {
  jit = 'JIT',
  auto = 'AUTO_PROVISIONING',
}
