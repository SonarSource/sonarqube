/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { FlowLocation, Issue, Paging, TextRange } from './types';
import { UserBase } from './users';

export enum IssueType {
  CodeSmell = 'CODE_SMELL',
  Vulnerability = 'VULNERABILITY',
  Bug = 'BUG',
  SecurityHotspot = 'SECURITY_HOTSPOT'
}

export enum IssueScope {
  Main = 'MAIN',
  Test = 'TEST'
}

interface Comment {
  createdAt: string;
  htmlText: string;
  key: string;
  login: string;
  markdown: string;
  updatable: boolean;
}

export interface RawIssue {
  actions: string[];
  assignee?: string;
  author?: string;
  comments?: Array<Comment>;
  component: string;
  flows?: Array<{
    // `componentName` is not available in RawIssue
    locations?: Array<Omit<FlowLocation, 'componentName'>>;
  }>;
  key: string;
  line?: number;
  project: string;
  rule: string;
  message?: string;
  severity: string;
  status: string;
  textRange?: TextRange;
  type: IssueType;
}

export interface IssueResponse {
  components?: Array<{ key: string; name: string }>;
  issue: RawIssue;
  rules?: Array<{}>;
  users?: Array<UserBase>;
}

export interface RawIssuesResponse {
  components: ReferencedComponent[];
  effortTotal: number;
  facets: RawFacet[];
  issues: RawIssue[];
  languages: ReferencedLanguage[];
  paging: Paging;
  rules?: Array<{}>;
  users?: Array<UserBase>;
}

export interface FetchIssuesPromise {
  components: ReferencedComponent[];
  effortTotal: number;
  facets: RawFacet[];
  issues: Issue[];
  languages: ReferencedLanguage[];
  paging: Paging;
  rules: ReferencedRule[];
  users: UserBase[];
}

export interface ReferencedComponent {
  key: string;
  name: string;
  path?: string;
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
