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

import { HotspotRatingEnum } from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MessageFormatting } from './issues';
import { FlowLocation, IssueChangelog, IssueChangelogDiff, Paging, TextRange } from './types';
import { UserBase } from './users';

export enum HotspotStatus {
  TO_REVIEW = 'TO_REVIEW',
  REVIEWED = 'REVIEWED',
}

export enum HotspotResolution {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
}

export enum HotspotStatusFilter {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  TO_REVIEW = 'TO_REVIEW',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
}

export enum HotspotStatusOption {
  FIXED = 'FIXED',
  SAFE = 'SAFE',
  TO_REVIEW = 'TO_REVIEW',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
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
  cveId?: string;
  flows?: Array<{
    locations?: Array<Omit<FlowLocation, 'componentName'>>;
  }>;
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
  vulnerabilityProbability: HotspotRatingEnum;
}

export interface Hotspot {
  assignee?: string;
  assigneeUser?: UserBase;
  author: string;
  authorUser: UserBase;
  canChangeStatus: boolean;
  changelog: IssueChangelog[];
  codeVariants?: string[];
  comment: HotspotComment[];
  component: HotspotComponent;
  creationDate: string;
  cveId?: string;
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
  branch?: string;
  key: string;
  longName: string;
  name: string;
  path: string;
  pullRequest?: string;
  qualifier: ComponentQualifier;
}

export interface HotspotUpdateFields {
  resolution?: HotspotResolution;
  status: HotspotStatus;
}

export interface HotspotUpdate extends HotspotUpdateFields {
  key: string;
}

export interface HotspotRule {
  key: string;
  name: string;
  securityCategory: string;
  vulnerabilityProbability: HotspotRatingEnum;
}

export interface HotspotComment {
  createdAt: string;
  htmlText: string;
  key: string;
  login: string;
  markdown: string;
  updatable: boolean;
  user: UserBase;
}

export interface ReviewHistoryElement {
  date: string;
  diffs?: IssueChangelogDiff[];
  html?: string;
  key?: string;
  markdown?: string;
  type: ReviewHistoryType;
  updatable?: boolean;
  user: Pick<UserBase, 'active' | 'avatar' | 'name'>;
}

export enum ReviewHistoryType {
  Creation,
  Diff,
  Comment,
}

export interface HotspotSearchResponse {
  components?: { key: string; name: string; qualifier: string }[];
  hotspots: RawHotspot[];
  paging: Paging;
}

export interface HotspotSetStatusRequest {
  comment?: string;
  resolution?: HotspotResolution;
  status: HotspotStatus;
}

export interface HotspotAssignRequest {
  assignee?: string;
  comment?: string;
}
