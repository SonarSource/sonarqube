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
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpact,
} from './clean-code-taxonomy';
import { Issue, Paging, TextRange } from './types';
import { UserBase } from './users';

export const ASSIGNEE_ME = '__me__';

export enum IssueType {
  CodeSmell = 'CODE_SMELL',
  Vulnerability = 'VULNERABILITY',
  Bug = 'BUG',
  SecurityHotspot = 'SECURITY_HOTSPOT',
}

// Keep this enum in the correct order (most severe to least severe).

export enum IssueSeverity {
  Blocker = 'BLOCKER',
  Critical = 'CRITICAL',
  Major = 'MAJOR',
  Minor = 'MINOR',
  Info = 'INFO',
}

export enum IssueScope {
  Main = 'MAIN',
  Test = 'TEST',
}

export enum IssueResolution {
  Unresolved = '',
  FalsePositive = 'FALSE-POSITIVE',
  Fixed = 'FIXED',
  Removed = 'REMOVED',
  WontFix = 'WONTFIX',
}

export enum IssueDeprecatedStatus {
  Open = 'OPEN',
  Confirmed = 'CONFIRMED',
  Reopened = 'REOPENED',
  Resolved = 'RESOLVED',
  Closed = 'CLOSED',
}

export enum IssueStatus {
  Open = 'OPEN',
  Fixed = 'FIXED',
  Confirmed = 'CONFIRMED',
  Accepted = 'ACCEPTED',
  FalsePositive = 'FALSE_POSITIVE',
}

export enum IssueActions {
  SetType = 'set_type',
  SetTags = 'set_tags',
  SetSeverity = 'set_severity',
  Comment = 'comment',
  Assign = 'assign',
}

export enum IssueTransition {
  Accept = 'accept',
  Confirm = 'confirm',
  UnConfirm = 'unconfirm',
  Resolve = 'resolve',
  FalsePositive = 'falsepositive',
  WontFix = 'wontfix',
  Reopen = 'reopen',
}

interface Comment {
  createdAt: string;
  htmlText: string;
  key: string;
  login: string;
  markdown: string;
  updatable: boolean;
}

export interface MessageFormatting {
  start: number;
  end: number;
  type: MessageFormattingType;
}

export enum MessageFormattingType {
  CODE = 'CODE',
}

export interface RawFlowLocation {
  component: string;
  index?: number;
  msg?: string;
  msgFormattings?: MessageFormatting[];
  textRange: TextRange;
}

export interface RawIssue {
  actions: string[];
  transitions: IssueTransition[];
  tags?: string[];
  assignee?: string;
  author?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
  cleanCodeAttribute: CleanCodeAttribute;
  impacts: SoftwareImpact[];
  codeVariants?: string[];
  comments?: Comment[];
  creationDate: string;
  component: string;
  flows?: Array<{
    type?: string;
    description?: string;
    locations?: RawFlowLocation[];
  }>;
  key: string;
  line?: number;
  messageFormattings?: MessageFormatting[];
  prioritizedRule?: boolean;
  project: string;
  rule: string;
  resolution?: string;
  message?: string;
  severity: string;
  status: string;
  issueStatus: IssueStatus;
  textRange?: TextRange;
  type: IssueType;
  scope: string;
  ruleDescriptionContextKey?: string;
  ruleStatus?: string;
  quickFixAvailable?: boolean;
}

export interface IssueResponse {
  components?: Array<{ key: string; name: string }>;
  issue: RawIssue;
  rules?: Array<{}>;
  users?: UserBase[];
}

export interface RawIssuesResponse {
  components: ReferencedComponent[];
  effortTotal: number;
  facets: RawFacet[];
  issues: RawIssue[];
  languages: ReferencedLanguage[];
  paging: Paging;
  rules?: Array<{}>;
  users?: UserBase[];
}

export interface ListIssuesResponse {
  components: ReferencedComponent[];
  issues: RawIssue[];
  paging: Paging;
  rules?: Array<{}>;
}

export interface FetchIssuesPromise {
  components?: ReferencedComponent[];
  effortTotal?: number;
  facets?: RawFacet[];
  issues: Issue[];
  languages?: ReferencedLanguage[];
  paging: Paging;
  rules: ReferencedRule[];
  users?: UserBase[];
}

export interface ListIssuesPromise {
  issues: Issue[];
  paging: Paging;
  rules: ReferencedRule[];
}

export interface ReferencedComponent {
  key: string;
  name: string;
  path?: string;
  enabled?: boolean;
  longName?: string;
  uuid: string;
}

export interface ReferencedLanguage {
  name: string;
}

export interface ReferencedRule {
  langName?: string;
  name: string;
}

export interface RawFacet {
  property: string;
  values: Array<{ val: string; count: number }>;
}

export interface Facet {
  [value: string]: number;
}

export enum FacetName {
  AssignedToMe = 'assigned_to_me',
  Assignees = 'assignees',
  Author = 'author',
  CodeVariants = 'codeVariants',
  CreatedAt = 'createdAt',
  Cwe = 'cwe',
  Directories = 'directories',
  Files = 'files',
  Languages = 'languages',
  OwaspTop10 = 'owaspTop10',
  Projects = 'projects',
  Reporters = 'reporters',
  Resolutions = 'resolutions',
  Rules = 'rules',
  Severities = 'severities',
  Statuses = 'statuses',
  Tags = 'tags',
  Types = 'types',
}
