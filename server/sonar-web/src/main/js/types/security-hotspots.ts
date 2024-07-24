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
import { ComponentQualifier } from './component';
import { MessageFormatting } from './issues';
import { FlowLocation, IssueChangelog, IssueChangelogDiff, Paging, TextRange } from './types';
import { UserBase } from './users';

export enum RiskExposure {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
}

export enum HotspotStatus {
  TO_REVIEW = 'TO_REVIEW',
  REVIEWED = 'REVIEWED',
}

export enum HotspotResolution {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  EXCEPTION = 'EXCEPTION',
}

export enum HotspotStatusFilter {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  TO_REVIEW = 'TO_REVIEW',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  EXCEPTION = 'EXCEPTION',
}

export enum HotspotStatusOption {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  TO_REVIEW = 'TO_REVIEW',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  EXCEPTION = 'EXCEPTION',
}

export interface HotspotFilters {
  assignedToMe: boolean;
  inNewCodePeriod: boolean;
  status: HotspotStatusFilter;
}

export interface RawHotspot {
  assignee?: string;
  author?: string;
  component: string;
  creationDate: string;
  key: string;
  line?: number;
  message: string;
  messageFormattings?: MessageFormatting[];
  project: string;
  resolution?: HotspotResolution;
  rule: string;
  securityCategory: string;
  status: HotspotStatus;
  updateDate: string;
  vulnerabilityProbability: RiskExposure;
  flows?: Array<{
    locations?: Array<Omit<FlowLocation, 'componentName'>>;
  }>;
}

export interface Hotspot {
  assignee?: string;
  assigneeUser?: UserBase;
  author: string;
  authorUser: UserBase;
  canChangeStatus: boolean;
  changelog: IssueChangelog[];
  comment: HotspotComment[];
  component: HotspotComponent;
  creationDate: string;
  flows: { locations: FlowLocation[] }[];
  key: string;
  line?: number;
  message: string;
  messageFormattings?: MessageFormatting[];
  project: HotspotComponent;
  resolution?: HotspotResolution;
  rule: HotspotRule;
  status: HotspotStatus;
  textRange?: TextRange;
  updateDate: string;
  users: UserBase[];
}

export interface HotspotComponent {
  key: string;
  qualifier: ComponentQualifier;
  name: string;
  longName: string;
  path: string;
  branch?: string;
  pullRequest?: string;
}

export interface HotspotUpdateFields {
  status: HotspotStatus;
  resolution?: HotspotResolution;
}

export interface HotspotUpdate extends HotspotUpdateFields {
  key: string;
}

export interface HotspotRule {
  key: string;
  name: string;
  securityCategory: string;
  vulnerabilityProbability: RiskExposure;
}

export interface HotspotComment {
  key: string;
  htmlText: string;
  markdown: string;
  updatable: boolean;
  createdAt: string;
  login: string;
  user: UserBase;
}

export interface ReviewHistoryElement {
  type: ReviewHistoryType;
  date: string;
  user: Pick<UserBase, 'active' | 'avatar' | 'name'>;
  diffs?: IssueChangelogDiff[];
  html?: string;
  key?: string;
  updatable?: boolean;
  markdown?: string;
}

export enum ReviewHistoryType {
  Creation,
  Diff,
  Comment,
}

export interface HotspotSearchResponse {
  components?: { key: string; qualifier: string; name: string }[];
  hotspots: RawHotspot[];
  paging: Paging;
}

export interface HotspotSetStatusRequest {
  status: HotspotStatus;
  resolution?: HotspotResolution;
  comment?: string;
}

export interface HotspotAssignRequest {
  assignee?: string;
  comment?: string;
}
