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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { RuleDescriptionSection } from '../apps/coding-rules/rule';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpact,
} from './clean-code-taxonomy';
import { Visibility } from './component';
import { IssueStatus, IssueTransition, MessageFormatting } from './issues';
import { NewCodeDefinitionType } from './new-code-definition';
import { UserActive, UserBase } from './users';

export type Dict<T> = { [key: string]: T };
export type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

export interface A11ySkipLink {
  key: string;
  label: string;
  weight?: number;
}

export interface AlmApplication extends IdentityProvider {
  installationUrl: string;
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

export interface Breadcrumb {
  key: string;
  name: string;
  qualifier: string;
}

export namespace Chart {
  export interface Point {
    x: Date;
    y: number | string | undefined;
  }

  export interface Serie {
    data: Point[];
    name: string;
    translatedName: string;
    type: string;
  }
}

export interface Component extends LightComponent {
  alm?: { key: string; url: string };
  analysisDate?: string;
  breadcrumbs: Breadcrumb[];
  branch?: string;
  canBrowseAllChildProjects?: boolean;
  configuration?: ComponentConfiguration;
  description?: string;
  extensions?: Extension[];
  isFavorite?: boolean;
  leakPeriodDate?: string;
  name: string;
  needIssueSync?: boolean;
  path?: string;
  refKey?: string;
  qualityProfiles?: ComponentQualityProfile[];
  qualityGate?: { isDefault?: boolean; key: string; name: string };
  tags?: string[];
  version?: string;
  visibility?: Visibility;
}

export interface NavigationComponent
  extends Omit<Component, 'alm' | 'qualifier' | 'leakPeriodDate' | 'path' | 'tags'> {}

interface ComponentConfiguration {
  canApplyPermissionTemplate?: boolean;
  canBrowseProject?: boolean;
  canUpdateProjectVisibilityToPrivate?: boolean;
  extensions?: Extension[];
  showBackgroundTasks?: boolean;
  showHistory?: boolean;
  showLinks?: boolean;
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

export interface ComponentMeasureIntern {
  analysisDate?: string;
  branch?: string;
  description?: string;
  isFavorite?: boolean;
  isRecentlyBrowsed?: boolean;
  canBrowseAllChildProjects?: boolean;
  key: string;
  match?: string;
  name: string;
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
  id: string;
  metric: string;
  op?: string;
  isCaycCondition?: boolean;
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
  user: UserBase;
  value: string;
  updatedAt?: string;
}

export interface Duplication {
  blocks: DuplicationBlock[];
}

export interface DuplicationBlock {
  _ref?: string;
  from: number;
  size: number;
}

export interface DuplicatedFile {
  key: string;
  name: string;
  project: string;
  projectName: string;
}

export type ExpandDirection = 'up' | 'down';

export interface Extension {
  key: string;
  name: string;
}

export interface FacetValue<T = string> {
  count: number;
  val: T;
}

export enum FlowType {
  DATA = 'DATA',
  EXECUTION = 'EXECUTION',
}

export interface Flow {
  type: FlowType;
  description?: string;
  locations: FlowLocation[];
}

export interface FlowLocation {
  component: string;
  componentName?: string;
  index?: number;
  msg?: string;
  msgFormattings?: MessageFormatting[];
  textRange: TextRange;
}

export interface Group {
  id: string;
  default?: boolean;
  name: string;
  description?: string;
  managed: boolean;
}

export interface GroupMembership {
  id: string;
  groupId: string;
  userId: string;
}

export enum HealthTypes {
  RED = 'RED',
  YELLOW = 'YELLOW',
  GREEN = 'GREEN',
}

export interface IdentityProvider {
  backgroundColor: string;
  helpMessage?: string;
  iconPath: string;
  key: string;
  name: string;
  manage?: boolean;
}

export interface Issue {
  actions: string[];
  assignee?: string;
  assigneeActive?: boolean;
  assigneeAvatar?: string;
  assigneeLogin?: string;
  assigneeName?: string;
  author?: string;
  branch?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
  cleanCodeAttribute: CleanCodeAttribute;
  impacts: SoftwareImpact[];
  codeVariants?: string[];
  comments?: IssueComment[];
  component: string;
  componentEnabled?: boolean;
  componentLongName: string;
  componentQualifier: string;
  componentUuid: string;
  creationDate: string;
  effort?: string;
  externalRuleEngine?: string;
  fromExternalRule?: boolean;
  quickFixAvailable?: boolean;
  key: string;
  flows: FlowLocation[][];
  flowsWithType: Flow[];
  line?: number;
  message: string;
  messageFormattings?: MessageFormatting[];
  project: string;
  projectName: string;
  projectKey: string;
  pullRequest?: string;
  resolution?: string;
  rule: string;
  ruleDescriptionContextKey?: string;
  ruleName: string;
  ruleStatus?: string;
  scope: string;
  secondaryLocations: FlowLocation[];
  severity: string;
  status: string;
  issueStatus: IssueStatus;
  tags?: string[];
  textRange?: TextRange;
  transitions: IssueTransition[];
  type: IssueType;
}

export interface IssueChangelog {
  avatar?: string;
  creationDate: string;
  diffs: IssueChangelogDiff[];
  user: string;
  isUserActive: boolean;
  userName: string;
  externalUser?: string;
  webhookSource?: string;
}

export interface IssueChangelogDiff {
  key: string;
  newValue?: string;
  oldValue?: string;
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

export interface IssuesByLine {
  [key: number]: Issue[];
}

export type IssueType = 'BUG' | 'VULNERABILITY' | 'CODE_SMELL' | 'SECURITY_HOTSPOT';

export interface Language {
  key: string;
  name: string;
}

export type Languages = Dict<Language>;

export interface LightComponent {
  key: string;
  qualifier: string;
}

export interface LinearIssueLocation {
  from: number;
  index?: number;
  line: number;
  startLine?: number;
  text?: string;
  textFormatting?: MessageFormatting[];
  to: number;
}

export interface LineMap {
  [line: number]: SourceLine;
}

export interface LinePopup {
  index?: number;
  line: number;
  name: string;
  open?: boolean;
}

export interface Measure extends MeasureIntern {
  metric: string;
}

export interface MeasureEnhanced extends MeasureIntern {
  metric: Metric;
  leak?: string;
}

export interface MeasureIntern {
  bestValue?: boolean;
  period?: PeriodMeasure;
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

export interface Paging {
  pageIndex: number;
  pageSize: number;
  total: number;
}

export interface Period {
  date: string;
  index?: number;
  mode: PeriodMode | NewCodeDefinitionType;
  modeParam?: string;
  parameter?: string;
}

export interface PeriodMeasure {
  bestValue?: boolean;
  index: number;
  value: string;
}

/*
 * These are old baseline setting types, necessary for
 * backward compatibility.
 */
export type PeriodMode =
  | 'days'
  | 'date'
  | 'version'
  | 'previous_analysis'
  | 'previous_version'
  | 'manual_baseline';

export interface Permission {
  description: string;
  key: string;
  name: string;
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
  managed?: boolean;
}

export interface PermissionUser extends UserActive {
  permissions: string[];
  managed?: boolean;
}

export interface PermissionTemplateGroup {
  key: string;
  usersCount: number;
  groupsCount: number;
  withProjectCreator?: boolean;
}

export interface PermissionTemplate {
  defaultFor: string[];
  id: string;
  name: string;
  description?: string;
  projectKeyPattern?: string;
  createdAt: string;
  updatedAt?: string;
  permissions: Array<PermissionTemplateGroup>;
}

export interface ProfileInheritanceDetails {
  activeRuleCount: number;
  inactiveRuleCount: number;
  isBuiltIn: boolean;
  key: string;
  name: string;
  overridingRuleCount?: number;
}

export interface ProjectLink {
  id: string;
  name?: string;
  type: string;
  url: string;
}

export enum CaycStatus {
  Compliant = 'compliant',
  NonCompliant = 'non-compliant',
  OverCompliant = 'over-compliant',
}

export interface QualityGatePreview {
  isDefault?: boolean;
  name: string;
}

export interface QualityGate extends QualityGatePreview {
  actions?: {
    associateProjects?: boolean;
    copy?: boolean;
    delegate?: boolean;
    delete?: boolean;
    manageConditions?: boolean;
    rename?: boolean;
    setAsDefault?: boolean;
  };
  conditions?: Condition[];
  isBuiltIn?: boolean;
  caycStatus?: CaycStatus;
}

export interface Rule {
  cleanCodeAttributeCategory?: CleanCodeAttributeCategory;
  cleanCodeAttribute?: CleanCodeAttribute;
  impacts: SoftwareImpact[];
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

export interface RestRule {
  cleanCodeAttributeCategory?: CleanCodeAttributeCategory;
  cleanCodeAttribute?: CleanCodeAttribute;
  impacts: SoftwareImpact[];
  template?: boolean;
  key: string;
  language?: string;
  languageName?: string;
  name: string;
  parameters?: RestRuleParameter[];
  severity: string;
  status: string;
  systemTags?: string[];
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

export interface RulesUpdateRequest {
  key: string;
  markdownDescription?: string;
  markdown_note?: string;
  name?: string;
  params?: string;
  remediation_fn_base_effort?: string;
  remediation_fn_type?: string;
  remediation_fy_gap_multiplier?: string;
  status?: string;
  tags?: string;
}

export interface RuleDetails extends Rule {
  createdAt: string;
  defaultRemFnBaseEffort?: string;
  defaultRemFnType?: string;
  descriptionSections?: RuleDescriptionSection[];
  educationPrinciples?: string[];
  gapDescription?: string;
  htmlDesc?: string;
  htmlNote?: string;
  internalKey?: string;
  isExternal?: boolean;
  mdDesc?: string;
  mdNote?: string;
  remFnBaseEffort?: string;
  remFnGapMultiplier?: string;
  remFnOverloaded?: boolean;
  remFnType?: string;
  repo: string;
  scope?: RuleScope;
  templateKey?: string;
}

export interface RestRuleDetails extends RestRule {
  createdAt: string;
  descriptionSections?: RuleDescriptionSection[];
  educationPrinciples?: string[];
  gapDescription?: string;
  htmlDesc?: string;
  htmlNote?: string;
  internalKey?: string;
  external?: boolean;
  markdownDescription?: string;
  markdownNote?: string;
  remFnBaseEffort?: string;
  remFnGapMultiplier?: string;
  remFnType?: string;
  repositoryKey: string;
  scope?: RuleScope;
  templateKey?: string;
}

export type RuleInheritance = 'NONE' | 'INHERITED' | 'OVERRIDES';

export interface RestRuleParameter {
  defaultValue?: string;
  htmlDescription?: string;
  key: string;
  type: string;
}

export interface RuleParameter {
  defaultValue?: string;
  htmlDesc?: string;
  key: string;
  type: string;
}

export type RuleScope = 'MAIN' | 'TEST' | 'ALL';

export type RuleType = 'BUG' | 'VULNERABILITY' | 'CODE_SMELL' | 'SECURITY_HOTSPOT' | 'UNKNOWN';

export interface Snippet {
  start: number;
  end: number;
  index: number;
  toDelete?: boolean;
}

export interface SnippetGroup extends SnippetsByComponent {
  locations: FlowLocation[];
}

export interface SnippetsByComponent {
  component: SourceViewerFile;
  sources: { [line: number]: SourceLine };
}

export interface SourceLine {
  code?: string;
  conditions?: number;
  coverageBlock?: number;
  coverageStatus?: SourceLineCoverageStatus;
  coveredConditions?: number;
  duplicated?: boolean;
  isNew?: boolean;
  line: number;
  lineHits?: number;
  newCodeBlock?: number;
  scmAuthor?: string;
  scmDate?: string;
  scmRevision?: string;
}

export type SourceLineCoverageStatus = 'uncovered' | 'partially-covered' | 'covered';

export interface SourceViewerFile {
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
  canMarkAsFavorite?: boolean;
  path: string;
  name?: string;
  longName?: string;
  project: string;
  projectName: string;
  q: ComponentQualifier;
  uuid: string;
}

export type StandardSecurityCategories = Dict<{ title: string; description?: string }>;

export interface SubscriptionPlan {
  maxNcloc: number;
  price: number;
}

export interface SuggestionLink {
  link: string;
  scope?: 'sonarcloud';
  text: string;
}

export interface SysInfoAppNode extends SysInfoBase {
  'Compute Engine Logging': SysInfoLogging;
  Name: string;
  'Web Logging': SysInfoLogging;
}

export interface SysInfoBase extends SysInfoValueObject {
  Health: HealthTypes;
  'Health Causes': string[];
  Plugins?: Dict<string>;
  System: {
    Version: string;
  };
}

export enum Provider {
  Github = 'github',
  Gitlab = 'gitlab',
  Scim = 'SCIM',
}

export interface SysInfoCluster extends SysInfoBase {
  'Application Nodes': SysInfoAppNode[];
  'Search Nodes': SysInfoSearchNode[];
  Settings: Dict<string>;
  Statistics?: {
    ncloc: number;
  };
  System: {
    'High Availability': true;
    'Server ID': string;
    Version: string;
    'External Users and Groups Provisioning'?: Provider;
  };
}

export interface SysInfoLogging extends Dict<string> {
  'Logs Level': string;
}

export interface SysInfoSearchNode extends SysInfoValueObject {
  Name: string;
}

export interface SysInfoSection extends Dict<SysInfoValueObject> {}

export interface SysInfoStandalone extends SysInfoBase {
  'Compute Engine Logging': SysInfoLogging;
  Settings: Dict<string>;
  Statistics?: {
    ncloc: number;
  } & Dict<string | number>;
  System: {
    'High Availability': false;
    'Server ID': string;
    Version: string;
  };
  'Web Logging': SysInfoLogging;
}

export type SysInfoValue =
  | boolean
  | string
  | number
  | undefined
  | HealthTypes
  | SysInfoValueObject
  | SysInfoValueArray;

export interface SysInfoValueArray extends Array<SysInfoValue> {}

export interface SysInfoValueObject extends Dict<SysInfoValue> {}

export type SysStatus =
  | 'STARTING'
  | 'UP'
  | 'DOWN'
  | 'RESTARTING'
  | 'DB_MIGRATION_NEEDED'
  | 'DB_MIGRATION_RUNNING';

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

export interface UserSelected extends UserActive {
  selected: boolean;
}

export interface UserGroupMember {
  selected: boolean;
  login: string;
  name: string;
  managed: boolean;
}

export namespace WebApi {
  export interface Action {
    key: string;
    changelog: Changelog[];
    description: string;
    deprecatedSince?: string;
    hasResponseExample: boolean;
    internal: boolean;
    params?: Param[];
    post: boolean;
    since?: string;
  }

  export interface Changelog {
    description: string;
    version: string;
  }

  export interface Domain {
    actions: Action[];
    deprecatedSince?: string;
    description: string;
    internal?: boolean;
    path: string;
    since?: string;
  }

  export interface Example {
    example: string;
    format: string;
  }

  export interface Param {
    defaultValue?: string;
    deprecatedKey?: string;
    deprecatedKeySince?: string;
    deprecatedSince?: string;
    description: string;
    exampleValue?: string;
    internal: boolean;
    key: string;
    maximumLength?: number;
    maximumValue?: number;
    maxValuesAllowed?: number;
    minimumLength?: number;
    minimumValue?: number;
    possibleValues?: string[];
    required: boolean;
    since?: string;
  }
}
