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

// Diff / Omit taken from https://github.com/Microsoft/TypeScript/issues/12215#issuecomment-311923766
export type Diff<T extends string, U extends string> = ({ [P in T]: P } &
  { [P in U]: never } & { [x: string]: never })[T];

export type Omit<T, K extends keyof T> = Pick<T, Diff<keyof T, K>>;

export enum BranchType {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export interface MainBranch {
  analysisDate?: string;
  isMain: true;
  name: string;
  status?: {
    qualityGateStatus: string;
  };
}

export interface LongLivingBranch {
  analysisDate?: string;
  isMain: false;
  name: string;
  status?: {
    qualityGateStatus: string;
  };
  type: BranchType.LONG;
}

export interface ShortLivingBranch {
  analysisDate?: string;
  isMain: false;
  isOrphan?: true;
  mergeBranch: string;
  name: string;
  status?: {
    bugs: number;
    codeSmells: number;
    vulnerabilities: number;
  };
  type: BranchType.SHORT;
}

export type Branch = MainBranch | LongLivingBranch | ShortLivingBranch;

export interface Extension {
  key: string;
  name: string;
}

export interface Breadcrumb {
  key: string;
  name: string;
  qualifier: string;
}

export interface LightComponent {
  key: string;
  organization: string;
  qualifier: string;
}

export interface Component extends LightComponent {
  analysisDate?: string;
  breadcrumbs: Breadcrumb[];
  configuration?: ComponentConfiguration;
  description?: string;
  extensions?: Extension[];
  isFavorite?: boolean;
  name: string;
  path?: string;
  refKey?: string;
  qualityProfiles?: { key: string; language: string; name: string }[];
  qualityGate?: { isDefault?: boolean; key: string; name: string };
  tags?: string[];
  version?: string;
  visibility?: string;
}

interface ComponentConfiguration {
  canApplyPermissionTemplate?: boolean;
  extensions?: Extension[];
  showBackgroundTasks?: boolean;
  showLinks?: boolean;
  showManualMeasures?: boolean;
  showQualityGates?: boolean;
  showQualityProfiles?: boolean;
  showPermissions?: boolean;
  showSettings?: boolean;
  showUpdateKey?: boolean;
}

export interface Metric {
  custom?: boolean;
  decimalScale?: number;
  description?: string;
  direction?: number;
  domain?: string;
  hidden?: boolean;
  id: string;
  key: string;
  name: string;
  qualitative?: boolean;
  type: string;
}

export interface Organization {
  adminPages?: { key: string; name: string }[];
  avatar?: string;
  canAdmin?: boolean;
  canDelete?: boolean;
  canProvisionProjects?: boolean;
  canUpdateProjectsVisibilityToPrivate?: boolean;
  description?: string;
  isAdmin?: boolean;
  isDefault?: boolean;
  key: string;
  name: string;
  pages?: { key: string; name: string }[];
  projectVisibility: Visibility;
  url?: string;
}

export interface Paging {
  pageIndex: number;
  pageSize: number;
  total: number;
}

export enum Visibility {
  Public = 'public',
  Private = 'private'
}

export interface CurrentUser {
  isLoggedIn: boolean;
  showOnboardingTutorial?: boolean;
}

export interface Group {
  default?: boolean;
  description?: string;
  id: number;
  membersCount: number;
  name: string;
}

export enum HomePageType {
  Project = 'PROJECT',
  Organization = 'ORGANIZATION',
  MyProjects = 'MY_PROJECTS',
  MyIssues = 'MY_ISSUES'
}

export interface HomePage {
  parameter?: string;
  type: HomePageType;
}

export function isSameHomePage(a: HomePage, b: HomePage) {
  return a.type === b.type && a.parameter === b.parameter;
}

export interface LoggedInUser extends CurrentUser {
  avatar?: string;
  email?: string;
  homepage?: HomePage;
  isLoggedIn: true;
  login: string;
  name: string;
}

export function isLoggedIn(user: CurrentUser): user is LoggedInUser {
  return user.isLoggedIn;
}

export interface AppState {
  adminPages?: Extension[];
  authenticationError?: boolean;
  authorizationError?: boolean;
  canAdmin?: boolean;
  globalPages?: Extension[];
  organizationsEnabled?: boolean;
  qualifiers: string[];
}

export interface Rule {
  isTemplate?: boolean;
  key: string;
  lang: string;
  langName: string;
  name: string;
  params?: RuleParameter[];
  severity: string;
  status: string;
  sysTags?: string[];
  tags?: string[];
  type: string;
}

export interface RuleDetails extends Rule {
  createdAt: string;
  debtOverloaded?: boolean;
  debtRemFnCoeff?: string;
  debtRemFnOffset?: string;
  debtRemFnType?: string;
  defaultDebtRemFnOffset?: string;
  defaultDebtRemFnType?: string;
  defaultRemFnBaseEffort?: string;
  defaultRemFnType?: string;
  effortToFixDescription?: string;
  htmlDesc?: string;
  htmlNote?: string;
  internalKey?: string;
  mdDesc?: string;
  mdNote?: string;
  remFnBaseEffort?: string;
  remFnOverloaded?: boolean;
  remFnType?: string;
  repo: string;
  scope?: RuleScope;
  templateKey?: string;
}

export interface RuleActivation {
  createdAt: string;
  inherit: RuleInheritance;
  params: { key: string; value: string }[];
  qProfile: string;
  severity: string;
}

export interface RuleParameter {
  // TODO is this extra really returned?
  extra?: string;
  defaultValue?: string;
  htmlDesc?: string;
  key: string;
  type: string;
}

export enum RuleInheritance {
  NotInherited = 'NONE',
  Inherited = 'INHERITED',
  Overridden = 'OVERRIDES'
}

export enum RuleScope {
  Main = 'MAIN',
  Test = 'TEST',
  All = 'ALL'
}

export interface IdentityProvider {
  backgroundColor: string;
  helpMessage?: string;
  iconPath: string;
  key: string;
  name: string;
}

export interface User {
  active: boolean;
  avatar?: string;
  email?: string;
  externalIdentity?: string;
  externalProvider?: string;
  groups?: string[];
  local: boolean;
  login: string;
  name: string;
  scmAccounts?: string[];
  tokensCount?: number;
}

export interface CustomMeasure {
  createdAt?: string;
  description?: string;
  id: string;
  metric: {
    key: string;
    name: string;
    domain?: string;
    type: string;
  };
  projectKey: string;
  pending?: boolean;
  user: {
    active?: boolean;
    email?: string;
    login: string;
    name: string;
  };
  value: string;
  updatedAt?: string;
}

export interface PermissionTemplate {
  defaultFor: string[];
  id: string;
  name: string;
  description?: string;
  projectKeyPattern?: string;
  createdAt: string;
  updatedAt?: string;
  permissions: Array<{
    key: string;
    usersCount: number;
    groupsCount: number;
    withProjectCreator?: boolean;
  }>;
}
