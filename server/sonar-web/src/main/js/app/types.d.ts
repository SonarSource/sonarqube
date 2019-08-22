/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
declare namespace T {
  // Type ordered alphabetically to prevent merge conflicts

  export interface A11ySkipLink {
    key: string;
    label: string;
    weight?: number;
  }

  export interface AlmApplication extends IdentityProvider {
    installationUrl: string;
  }

  export interface AlmOrganization extends OrganizationBase {
    almUrl: string;
    key: string;
    personal: boolean;
    privateRepos: number;
    publicRepos: number;
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
    buildString?: string;
    date: string;
    events: AnalysisEvent[];
    key: string;
    manualNewCodePeriodBaseline?: boolean;
    projectVersion?: string;
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
    settings: T.Dict<string>;
    standalone?: boolean;
    version: string;
    webAnalyticsJsPath?: string;
  }

  export interface Branch {
    analysisDate?: string;
    isMain: boolean;
    name: string;
    status?: { qualityGateStatus: Status };
  }

  export type BranchLike = Branch | PullRequest;

  export type BranchParameters = { branch?: string } | { pullRequest?: string };

  export type BranchType = 'LONG' | 'SHORT';

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

  export interface CurrentUserSetting {
    key: CurrentUserSettingNames;
    value: string;
  }

  type CurrentUserSettingNames =
    | 'notifications.optOut'
    | 'notifications.readDate'
    | 'newsbox.dismiss.hotspots';

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
    user: T.UserBase;
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
    subProject?: string;
    subProjectName?: string;
  }

  export type EditionKey = 'community' | 'developer' | 'enterprise' | 'datacenter';

  export type ExpandDirection = 'up' | 'down';

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
    index?: number;
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

  export type HealthType = 'RED' | 'YELLOW' | 'GREEN';

  export type HomePage =
    | { type: 'APPLICATION'; branch: string | undefined; component: string }
    | { type: 'ISSUES' }
    | { type: 'MY_ISSUES' }
    | { type: 'MY_PROJECTS' }
    | { type: 'ORGANIZATION'; organization: string }
    | { type: 'PORTFOLIO'; component: string }
    | { type: 'PORTFOLIOS' }
    | { type: 'PROJECT'; branch: string | undefined; component: string }
    | { type: 'PROJECTS' };

  export type HomePageType =
    | 'APPLICATION'
    | 'ISSUES'
    | 'MY_ISSUES'
    | 'MY_PROJECTS'
    | 'ORGANIZATION'
    | 'PORTFOLIO'
    | 'PORTFOLIOS'
    | 'PROJECT'
    | 'PROJECTS';

  export interface IdentityProvider {
    backgroundColor: string;
    helpMessage?: string;
    iconPath: string;
    key: string;
    name: string;
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
    type: T.IssueType;
  }

  export interface IssueChangelog {
    avatar?: string;
    creationDate: string;
    diffs: IssueChangelogDiff[];
    user: string;
    isUserActive: boolean;
    userName: string;
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
  export interface Language {
    key: string;
    name: string;
  }

  export type Languages = T.Dict<Language>;

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
    text?: string;
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

  export interface LoggedInUser extends CurrentUser, UserActive {
    externalIdentity?: string;
    externalProvider?: string;
    groups: string[];
    homepage?: HomePage;
    isLoggedIn: true;
    local?: boolean;
    personalOrganization?: string;
    scmAccounts: string[];
    settings?: CurrentUserSetting[];
  }

  export interface LongLivingBranch extends Branch {
    isMain: false;
    type: 'LONG';
  }

  export interface MainBranch extends Branch {
    isMain: true;
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
    alm?: { key: string; membersSync: boolean; personal: boolean; url: string };
    adminPages?: Extension[];
    canUpdateProjectsVisibilityToPrivate?: boolean;
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

  export interface OrganizationMember extends UserActive {
    groupCount?: number;
  }

  export type OrganizationSubscription = 'FREE' | 'PAID' | 'SONARQUBE';

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
  }

  export interface PermissionUser extends UserActive {
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

  export interface ProfileInheritanceDetails {
    activeRuleCount: number;
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

  export interface PullRequest {
    analysisDate?: string;
    base: string;
    branch: string;
    key: string;
    isOrphan?: true;
    status?: { qualityGateStatus: Status };
    target: string;
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

  export interface QualityGateProjectStatusCondition {
    status: Status;
    metricKey: string;
    comparator: string;
    periodIndex: number;
    errorThreshold: string;
    actualValue: string;
  }

  export interface QualityGateProjectStatus {
    conditions?: QualityGateProjectStatusCondition[];
    ignoredConditions: boolean;
    status: Status;
  }

  export interface QualityGateStatusCondition {
    actual?: string;
    error?: string;
    level: string;
    metric: string;
    op: string;
    period?: number;
    warning?: string;
  }

  export interface QualityGateStatusConditionEnhanced extends QualityGateStatusCondition {
    measure: T.MeasureEnhanced;
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

  export type RuleInheritance = 'NONE' | 'INHERITED' | 'OVERRIDES';

  export interface RuleParameter {
    defaultValue?: string;
    htmlDesc?: string;
    key: string;
    type: string;
  }

  export type RuleScope = 'MAIN' | 'TEST' | 'ALL';

  export type RuleType = 'BUG' | 'VULNERABILITY' | 'CODE_SMELL' | 'SECURITY_HOTSPOT' | 'UNKNOWN';

  export type Setting = SettingValue & { definition: SettingDefinition };

  export type SettingType =
    | 'STRING'
    | 'TEXT'
    | 'PASSWORD'
    | 'BOOLEAN'
    | 'FLOAT'
    | 'INTEGER'
    | 'LICENSE'
    | 'LONG'
    | 'SINGLE_SELECT_LIST'
    | 'PROPERTY_SET';

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
    fieldValues?: Array<T.Dict<string>>;
    inherited?: boolean;
    key: string;
    parentFieldValues?: Array<T.Dict<string>>;
    parentValue?: string;
    parentValues?: string[];
    value?: string;
    values?: string[];
  }

  export interface ShortLivingBranch extends Branch {
    isMain: false;
    isOrphan?: true;
    mergeBranch: string;
    type: 'SHORT';
  }

  export interface Snippet {
    start: number;
    end: number;
    index: number;
    toDelete?: boolean;
  }

  export interface SnippetGroup extends SnippetsByComponent {
    locations: T.FlowLocation[];
  }
  export interface SnippetsByComponent {
    component: SourceViewerFile;
    sources: { [line: number]: SourceLine };
  }

  export interface SourceLine {
    code?: string;
    conditions?: number;
    coverageStatus?: SourceLineCoverageStatus;
    coveredConditions?: number;
    duplicated?: boolean;
    isNew?: boolean;
    line: number;
    lineHits?: number;
    scmAuthor?: string;
    scmDate?: string;
    scmRevision?: string;
  }

  export type SourceLineCoverageStatus = 'uncovered' | 'partially-covered' | 'covered';

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

  export type Standards = {
    [key in StandardType]: T.Dict<{ title: string; description?: string }>
  };

  export type StandardType = 'owaspTop10' | 'sansTop25' | 'cwe' | 'sonarsourceSecurity';

  export type Status = 'ERROR' | 'OK';

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
    Health: HealthType;
    'Health Causes': string[];
    Plugins?: Dict<string>;
    System: {
      Version: string;
    };
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
    | HealthType
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

  export interface SystemUpgrade {
    version: string;
    description: string;
    releaseDate: string;
    changeLogUrl: string;
    downloadUrl: string;
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

  export interface User extends UserBase {
    externalIdentity?: string;
    externalProvider?: string;
    groups?: string[];
    lastConnectionDate?: string;
    local: boolean;
    scmAccounts?: string[];
    tokensCount?: number;
  }

  export interface UserActive extends UserBase {
    active?: true;
    name: string;
  }

  export interface UserBase {
    active?: boolean;
    avatar?: string;
    email?: string;
    login: string;
    name?: string;
  }

  export interface UserSelected extends UserActive {
    selected: boolean;
  }

  export interface UserToken {
    name: string;
    createdAt: string;
    lastConnectionDate?: string;
  }

  export interface NewUserToken extends UserToken {
    login: string;
    token: string;
  }

  export type Visibility = 'public' | 'private';

  export interface Webhook {
    key: string;
    latestDelivery?: WebhookDelivery;
    name: string;
    secret?: string;
    url: string;
  }

  export interface WebhookDelivery {
    at: string;
    durationMs: number;
    httpStatus?: number;
    id: string;
    success: boolean;
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
}
