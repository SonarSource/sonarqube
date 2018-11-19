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
import { EditionKey } from '../apps/marketplace/utils';

export type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

// Type ordered alphabetically to prevent merge conflicts

export interface AlmApplication extends IdentityProvider {
  installationUrl: string;
}

export interface AlmOrganization extends OrganizationBase {
  key: string;
  personal: boolean;
}

export interface AlmRepository {
  label: string;
  installationKey: string;
  linkedProjectKey?: string;
  linkedProjectName?: string;
  private?: boolean;
}

export interface AlmUnboundApplication {
  installationId: string;
  key: string;
  name: string;
}

export interface Analysis {
  date: string;
  events: AnalysisEvent[];
  key: string;
}

export interface AnalysisEvent {
  category: string;
  description?: string;
  key: string;
  name: string;
  qualityGate?: {
    failing: Array<{ branch: string; key: string; name: string }>;
    status: string;
    stillFailing: boolean;
  };
  definitionChange?: {
    projects: Array<{
      branch?: string;
      changeType: string;
      key: string;
      name: string;
      newBranch?: string;
      oldBranch?: string;
    }>;
  };
}

export interface AppState {
  adminPages?: Extension[];
  authenticationError?: boolean;
  authorizationError?: boolean;
  branchesEnabled?: boolean;
  canAdmin?: boolean;
  defaultOrganization: string;
  edition: EditionKey;
  globalPages?: Extension[];
  organizationsEnabled?: boolean;
  productionDatabase: boolean;
  qualifiers: string[];
  settings: { [key: string]: string };
  standalone?: boolean;
  version: string;
}

export interface Branch {
  analysisDate?: string;
  isMain: boolean;
  name: string;
}

export type BranchLike = Branch | PullRequest;

export type BranchParameters = { branch?: string } | { pullRequest?: string };

export enum BranchType {
  LONG = 'LONG',
  SHORT = 'SHORT'
}

export interface Breadcrumb {
  key: string;
  name: string;
  qualifier: string;
}

export interface Component extends LightComponent {
  alm?: { key: string; url: string };
  analysisDate?: string;
  breadcrumbs: Breadcrumb[];
  configuration?: ComponentConfiguration;
  description?: string;
  extensions?: Extension[];
  isFavorite?: boolean;
  leakPeriodDate?: string;
  name: string;
  path?: string;
  refKey?: string;
  qualityProfiles?: ComponentQualityProfile[];
  qualityGate?: { isDefault?: boolean; key: string; name: string };
  tags?: string[];
  version?: string;
  visibility?: Visibility;
}

interface ComponentConfiguration {
  canApplyPermissionTemplate?: boolean;
  canUpdateProjectVisibilityToPrivate?: boolean;
  extensions?: Extension[];
  showBackgroundTasks?: boolean;
  showHistory?: boolean;
  showLinks?: boolean;
  showManualMeasures?: boolean;
  showQualityGates?: boolean;
  showQualityProfiles?: boolean;
  showPermissions?: boolean;
  showSettings?: boolean;
  showUpdateKey?: boolean;
}

export interface ComponentQualityProfile {
  deleted?: boolean;
  key: string;
  language: string;
  name: string;
}

interface ComponentMeasureIntern {
  branch?: string;
  description?: string;
  isFavorite?: boolean;
  isRecentlyBrowsed?: boolean;
  key: string;
  match?: string;
  name: string;
  organization?: string;
  path?: string;
  project?: string;
  qualifier: string;
  refKey?: string;
}

export interface ComponentMeasure extends ComponentMeasureIntern {
  measures?: Measure[];
}

export interface ComponentMeasureEnhanced extends ComponentMeasureIntern {
  value?: string;
  leak?: string;
  measures: MeasureEnhanced[];
}

export interface Condition {
  error: string;
  id: number;
  metric: string;
  op?: string;
  period?: number;
  warning: string;
}

export interface CoveredFile {
  key: string;
  longName: string;
  coveredLines: number;
}

export interface Coupon {
  billing?: {
    address?: string;
    country?: string;
    email?: string;
    name?: string;
    use?: string;
  };
  maxNcloc: number;
  planActiveUntil: string;
}

export interface CurrentUser {
  isLoggedIn: boolean;
  permissions?: { global: string[] };
  showOnboardingTutorial?: boolean;
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

export interface Duplication {
  blocks: DuplicationBlock[];
}

export interface DuplicationBlock {
  _ref: string;
  from: number;
  size: number;
}

export interface DuplicatedFile {
  key: string;
  name: string;
  project: string;
  projectName: string;
  subProject?: string;
  subProjectName?: string;
}

export interface Extension {
  key: string;
  name: string;
}

export interface FacetValue<T = string> {
  count: number;
  val: T;
}

export interface FlowLocation {
  component: string;
  componentName?: string;
  msg?: string;
  textRange: TextRange;
}

export interface Group {
  default?: boolean;
  description?: string;
  id: number;
  membersCount: number;
  name: string;
}

export type HomePage =
  | { type: HomePageType.Application; branch: string | undefined; component: string }
  | { type: HomePageType.Issues }
  | { type: HomePageType.MyIssues }
  | { type: HomePageType.MyProjects }
  | { type: HomePageType.Organization; organization: string }
  | { type: HomePageType.Portfolio; component: string }
  | { type: HomePageType.Portfolios }
  | { type: HomePageType.Project; branch: string | undefined; component: string }
  | { type: HomePageType.Projects };

export enum HomePageType {
  Application = 'APPLICATION',
  Issues = 'ISSUES',
  MyIssues = 'MY_ISSUES',
  MyProjects = 'MY_PROJECTS',
  Organization = 'ORGANIZATION',
  Portfolio = 'PORTFOLIO',
  Portfolios = 'PORTFOLIOS',
  Project = 'PROJECT',
  Projects = 'PROJECTS'
}

export interface IdentityProvider {
  backgroundColor: string;
  helpMessage?: string;
  iconPath: string;
  key: string;
  name: string;
}

export function hasGlobalPermission(user: CurrentUser, permission: string): boolean {
  if (!user.permissions) {
    return false;
  }
  return user.permissions.global.includes(permission);
}

export function isSameHomePage(a: HomePage, b: HomePage) {
  return (
    a.type === b.type &&
    (a as any).branch === (b as any).branch &&
    (a as any).component === (b as any).component &&
    (a as any).organization === (b as any).organization
  );
}

export interface SecurityHotspot {
  activeRules: number;
  category?: string;
  cwe?: string;
  distribution?: Array<SecurityHotspot>;
  openSecurityHotspots: number;
  toReviewSecurityHotspots: number;
  totalRules: number;
  vulnerabilities: number;
  vulnerabilityRating?: number;
  wontFixSecurityHotspots: number;
}

export interface Issue {
  actions: string[];
  assignee?: string;
  assigneeActive?: string;
  assigneeAvatar?: string;
  assigneeLogin?: string;
  assigneeName?: string;
  author?: string;
  branch?: string;
  comments?: IssueComment[];
  component: string;
  componentLongName: string;
  componentQualifier: string;
  componentUuid: string;
  creationDate: string;
  effort?: string;
  externalRuleEngine?: string;
  fromExternalRule?: boolean;
  key: string;
  flows: FlowLocation[][];
  fromHotspot: boolean;
  line?: number;
  message: string;
  organization: string;
  project: string;
  projectName: string;
  projectOrganization: string;
  projectKey: string;
  pullRequest?: string;
  resolution?: string;
  rule: string;
  ruleName: string;
  secondaryLocations: FlowLocation[];
  severity: string;
  status: string;
  subProject?: string;
  subProjectName?: string;
  subProjectUuid?: string;
  tags?: string[];
  textRange?: TextRange;
  transitions: string[];
  type: IssueType;
}

export interface IssueComment {
  author?: string;
  authorActive?: boolean;
  authorAvatar?: string;
  authorLogin?: string;
  authorName?: string;
  createdAt: string;
  htmlText: string;
  key: string;
  markdown: string;
  updatable: boolean;
}

export enum IssueType {
  Bug = 'BUG',
  Vulnerability = 'VULNERABILITY',
  CodeSmell = 'CODE_SMELL',
  Hotspot = 'SECURITY_HOTSPOT'
}

export interface Language {
  key: string;
  name: string;
}

export interface Languages {
  [key: string]: Language;
}

export interface LightComponent {
  key: string;
  organization: string;
  qualifier: string;
}

export interface LinearIssueLocation {
  from: number;
  index?: number;
  line: number;
  startLine?: number;
  to: number;
}

export interface LoggedInUser extends CurrentUser {
  avatar?: string;
  email?: string;
  externalIdentity?: string;
  externalProvider?: string;
  groups: string[];
  homepage?: HomePage;
  isLoggedIn: true;
  local?: boolean;
  login: string;
  name: string;
  personalOrganization?: string;
  scmAccounts: string[];
}

export interface LongLivingBranch extends Branch {
  isMain: false;
  status?: { qualityGateStatus: string };
  type: BranchType.LONG;
}

export interface MainBranch extends Branch {
  isMain: true;
  status?: { qualityGateStatus: string };
}

export interface Measure extends MeasureIntern {
  metric: string;
}

export interface MeasureEnhanced extends MeasureIntern {
  metric: Metric;
  leak?: string;
}

interface MeasureIntern {
  bestValue?: boolean;
  periods?: PeriodMeasure[];
  value?: string;
}

export interface Metric {
  bestValue?: string;
  custom?: boolean;
  decimalScale?: number;
  description?: string;
  direction?: number;
  domain?: string;
  hidden?: boolean;
  higherValuesAreBetter?: boolean;
  id: string;
  key: string;
  name: string;
  qualitative?: boolean;
  type: string;
  worstValue?: string;
}

export interface MyProject {
  description?: string;
  key: string;
  lastAnalysisDate?: string;
  links: Array<{
    name: string;
    type: string;
    href: string;
  }>;
  name: string;
  qualityGate?: string;
}

export interface Notification {
  channel: string;
  organization?: string;
  project?: string;
  projectName?: string;
  type: string;
}

export interface OrganizationActions {
  admin?: boolean;
  delete?: boolean;
  provision?: boolean;
  executeAnalysis?: boolean;
}

export interface Organization extends OrganizationBase {
  actions?: OrganizationActions;
  alm?: { key: string; url: string };
  adminPages?: Extension[];
  canUpdateProjectsVisibilityToPrivate?: boolean;
  guarded?: boolean;
  isDefault?: boolean;
  key: string;
  pages?: Extension[];
  projectVisibility?: Visibility;
  subscription?: OrganizationSubscription;
}

export interface OrganizationBase {
  avatar?: string;
  description?: string;
  key?: string;
  name: string;
  url?: string;
}

export interface OrganizationMember {
  login: string;
  name: string;
  avatar?: string;
  groupCount?: number;
}

export enum OrganizationSubscription {
  Free = 'FREE',
  Paid = 'PAID',
  SonarQube = 'SONARQUBE'
}

export interface Paging {
  pageIndex: number;
  pageSize: number;
  total: number;
}

export interface Period {
  date: string;
  index: number;
  mode: PeriodMode;
  modeParam?: string;
  parameter?: string;
}

export interface PeriodMeasure {
  bestValue?: boolean;
  index: number;
  value: string;
}

export enum PeriodMode {
  Days = 'days',
  Date = 'date',
  Version = 'version',
  PreviousAnalysis = 'previous_analysis',
  PreviousVersion = 'previous_version'
}

export interface PermissionDefinition {
  key: string;
  name: string;
  description: string;
}

export type PermissionDefinitions = Array<PermissionDefinition | PermissionDefinitionGroup>;

export interface PermissionDefinitionGroup {
  category: string;
  permissions: PermissionDefinition[];
}

export interface PermissionGroup {
  description?: string;
  id?: string;
  name: string;
  permissions: string[];
}

export interface PermissionUser {
  avatar?: string;
  email?: string;
  login: string;
  name: string;
  permissions: string[];
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

export interface ProjectLink {
  id: string;
  name?: string;
  type: string;
  url: string;
}

export interface PullRequest {
  analysisDate?: string;
  base: string;
  branch: string;
  key: string;
  isOrphan?: true;
  status?: {
    bugs: number;
    codeSmells: number;
    qualityGateStatus: string;
    vulnerabilities: number;
  };
  title: string;
  url?: string;
}

export interface QualityGate {
  actions?: {
    associateProjects?: boolean;
    copy?: boolean;
    delete?: boolean;
    manageConditions?: boolean;
    rename?: boolean;
    setAsDefault?: boolean;
  };
  conditions?: Condition[];
  id: number;
  isBuiltIn?: boolean;
  isDefault?: boolean;
  name: string;
}

export interface Rule {
  isTemplate?: boolean;
  key: string;
  lang?: string;
  langName?: string;
  name: string;
  params?: RuleParameter[];
  severity: string;
  status: string;
  sysTags?: string[];
  tags?: string[];
  type: RuleType;
}

export interface RuleActivation {
  createdAt: string;
  inherit: RuleInheritance;
  params: { key: string; value: string }[];
  qProfile: string;
  severity: string;
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
  isExternal?: boolean;
  mdDesc?: string;
  mdNote?: string;
  remFnBaseEffort?: string;
  remFnOverloaded?: boolean;
  remFnType?: string;
  repo: string;
  scope?: RuleScope;
  templateKey?: string;
}

export enum RuleInheritance {
  NotInherited = 'NONE',
  Inherited = 'INHERITED',
  Overridden = 'OVERRIDES'
}

export interface RuleParameter {
  // TODO is this extra really returned?
  extra?: string;
  defaultValue?: string;
  htmlDesc?: string;
  key: string;
  type: string;
}

export enum RuleScope {
  Main = 'MAIN',
  Test = 'TEST',
  All = 'ALL'
}

export enum RuleType {
  Bug = 'BUG',
  Vulnerability = 'VULNERABILITY',
  CodeSmell = 'CODE_SMELL',
  Hotspot = 'SECURITY_HOTSPOT',
  Unknown = 'UNKNOWN'
}

export type Setting = SettingValue & { definition: SettingDefinition };

export enum SettingType {
  String = 'STRING',
  Text = 'TEXT',
  Password = 'PASSWORD',
  Boolean = 'BOOLEAN',
  Float = 'FLOAT',
  Integer = 'INTEGER',
  License = 'LICENSE',
  Long = 'LONG',
  SingleSelectList = 'SINGLE_SELECT_LIST',
  PropertySet = 'PROPERTY_SET'
}

export interface SettingDefinition {
  description?: string;
  key: string;
  name?: string;
  options: string[];
  type?: SettingType;
}

export interface SettingFieldDefinition extends SettingDefinition {
  description: string;
  name: string;
}

export interface SettingCategoryDefinition extends SettingDefinition {
  category: string;
  defaultValue?: string;
  deprecatedKey?: string;
  fields: SettingFieldDefinition[];
  multiValues?: boolean;
  subCategory: string;
}

export interface SettingValue {
  fieldValues?: Array<{ [key: string]: string }>;
  inherited?: boolean;
  key: string;
  parentFieldValues?: Array<{ [key: string]: string }>;
  parentValue?: string;
  parentValues?: string[];
  value?: string;
  values?: string[];
}

export interface ShortLivingBranch extends Branch {
  isMain: false;
  isOrphan?: true;
  mergeBranch: string;
  status?: {
    bugs: number;
    codeSmells: number;
    qualityGateStatus: string;
    vulnerabilities: number;
  };
  type: BranchType.SHORT;
}

export interface SourceLine {
  code?: string;
  conditions?: number;
  coverageStatus?: string;
  coveredConditions?: number;
  duplicated?: boolean;
  isNew?: boolean;
  line: number;
  lineHits?: number;
  scmAuthor?: string;
  scmDate?: string;
  scmRevision?: string;
}

export interface SourceViewerFile {
  canMarkAsFavorite?: boolean;
  fav?: boolean;
  key: string;
  leakPeriodDate?: string;
  measures: {
    coverage?: string;
    duplicationDensity?: string;
    issues?: string;
    lines?: string;
    tests?: string;
  };
  path: string;
  project: string;
  projectName: string;
  q: string;
  subProject?: string;
  subProjectName?: string;
  uuid: string;
}

export interface SubscriptionPlan {
  maxNcloc: number;
  price: number;
}

export interface Task {
  analysisId?: string;
  branch?: string;
  branchType?: string;
  componentKey?: string;
  componentName?: string;
  componentQualifier?: string;
  errorMessage?: string;
  errorStacktrace?: string;
  errorType?: string;
  executedAt?: string;
  executionTimeMs?: number;
  hasErrorStacktrace?: boolean;
  hasScannerContext?: boolean;
  id: string;
  logs?: boolean;
  organization: string;
  pullRequest?: string;
  pullRequestTitle?: string;
  scannerContext?: string;
  startedAt?: string;
  status: string;
  submittedAt: string;
  submitterLogin?: string;
  type: string;
  warningCount?: number;
  warnings?: string[];
}

export interface TestCase {
  coveredLines: number;
  durationInMs: number;
  fileId: string;
  fileKey: string;
  fileName: string;
  id: string;
  message?: string;
  name: string;
  stacktrace?: string;
  status: string;
}

export interface TextRange {
  startLine: number;
  startOffset: number;
  endLine: number;
  endOffset: number;
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

export enum Visibility {
  Public = 'public',
  Private = 'private'
}

export interface Webhook {
  key: string;
  latestDelivery?: WebhookDelivery;
  name: string;
  url: string;
}

export interface WebhookDelivery {
  at: string;
  durationMs: number;
  httpStatus?: number;
  id: string;
  success: boolean;
}
