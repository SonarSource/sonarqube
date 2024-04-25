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
import { omit } from 'lodash';
import { To } from 'react-router-dom';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { Location, Router } from '~sonar-aligned/types/router';
import { CompareResponse } from '../api/quality-profiles';
import { RuleDescriptionSections } from '../apps/coding-rules/rule';
import { REST_RULE_KEYS_TO_OLD_KEYS } from '../apps/coding-rules/utils';
import { Exporter, Profile, ProfileChangelogEvent } from '../apps/quality-profiles/types';
import { LogsLevels } from '../apps/system/utils';
import { AppState } from '../types/appstate';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../types/clean-code-taxonomy';
import { RuleRepository } from '../types/coding-rules';
import { EditionKey } from '../types/editions';
import {
  IssueDeprecatedStatus,
  IssueScope,
  IssueSeverity,
  IssueStatus,
  IssueType,
  RawIssue,
} from '../types/issues';
import { Language } from '../types/languages';
import { Notification } from '../types/notifications';
import { DumpStatus, DumpTask } from '../types/project-dump';
import { TaskStatuses } from '../types/tasks';
import {
  AlmApplication,
  Condition,
  FlowLocation,
  Group,
  GroupMembership,
  HealthTypes,
  IdentityProvider,
  Issue,
  Measure,
  MeasureEnhanced,
  Metric,
  Paging,
  Period,
  RestRuleDetails,
  Rule,
  RuleActivation,
  RuleDetails,
  RuleParameter,
  SysInfoBase,
  SysInfoCluster,
  SysInfoLogging,
  SysInfoStandalone,
  UserGroupMember,
  UserSelected,
} from '../types/types';
import { CurrentUser, LoggedInUser, RestUserDetailed, User } from '../types/users';

export function mockAlmApplication(overrides: Partial<AlmApplication> = {}): AlmApplication {
  return {
    backgroundColor: '#444444',
    iconPath: '/images/alm/github.svg',
    installationUrl: 'https://github.com/apps/greg-sonarcloud/installations/new',
    key: 'github',
    name: 'GitHub',
    ...overrides,
  };
}

export function mockAppState(overrides: Partial<AppState> = {}): AppState {
  return {
    edition: EditionKey.community,
    productionDatabase: true,
    qualifiers: [ComponentQualifier.Project],
    settings: {},
    version: '1.0',
    versionEOL: '2020-01-01',
    documentationUrl: 'https://docs.sonarsource.com/sonarqube/10.0',
    ...overrides,
  };
}

export function mockBaseSysInfo(overrides: Partial<any> = {}): SysInfoBase {
  return {
    Health: HealthTypes.GREEN,
    'Health Causes': [],
    System: {
      Version: '7.8',
    },
    Database: {
      Database: 'PostgreSQL',
      'Database Version': '10.3',
      Username: 'sonar',
      URL: 'jdbc:postgresql://localhost/sonar',
      Driver: 'PostgreSQL JDBC Driver',
      'Driver Version': '42.2.5',
    },
    'Compute Engine Tasks': {
      'Total Pending': 0,
      'Total In Progress': 0,
    },
    'Search State': { State: 'GREEN', Nodes: 3 },
    'Search Indexes': {
      'Index components - Docs': 30445,
      'Index components - Shards': 10,
    },
    ...overrides,
  };
}

export function mockClusterSysInfo(overrides: Partial<any> = {}): SysInfoCluster {
  const baseInfo = mockBaseSysInfo(overrides);
  return {
    ...baseInfo,
    System: {
      ...baseInfo.System,
      'High Availability': true,
      'Server ID': 'asd564-asd54a-5dsfg45',
    },
    Settings: {
      'sonar.cluster.enabled': 'true',
      'sonar.cluster.node.name': 'server9.example.com',
    },
    'Application Nodes': [
      {
        Name: 'server1.example.com',
        Host: '10.0.0.0',
        Health: HealthTypes.RED,
        'Health Causes': ['Something is wrong'],
        System: {
          Version: '7.8',
        },
        Plugins: {
          java: '5.13.0.17924 [SonarJava]',
        },
        'Web JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 122,
        },
        'Web Database Connection': {
          'Pool Active Connections': 1,
        },
        'Web Logging': { 'Logs Level': 'DEBUG' },
        'Web JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/',
        },
        'Compute Engine Tasks': {
          Pending: 0,
          'In Progress': 0,
        },
        'Compute Engine JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 78,
        },
        'Compute Engine Database Connection': {
          'Pool Initial Size': 0,
          'Pool Active Connections': 0,
        },
        'Compute Engine Logging': {
          'Logs Level': 'INFO',
        },
        'Compute Engine JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/',
        },
      },
      {
        Name: 'server2.example.com',
        Host: '10.0.0.0',
        Health: HealthTypes.YELLOW,
        'Health Causes': ['Friendly warning'],
        System: {
          Version: '7.8',
        },
        Plugins: {
          java: '5.13.0.17924 [SonarJava]',
        },
        'Web JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 111,
        },
        'Web Database Connection': {
          'Pool Active Connections': 0,
          'Pool Max Connections': 60,
        },
        'Web Logging': { 'Logs Level': 'INFO' },
        'Web JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/',
        },
        'Compute Engine Tasks': {
          Pending: 0,
          'In Progress': 0,
        },
        'Compute Engine JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 89,
        },
        'Compute Engine Database Connection': {
          'Pool Initial Size': 0,
          'Pool Active Connections': 0,
        },
        'Compute Engine Logging': {
          'Logs Level': 'INFO',
        },
        'Compute Engine JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/',
        },
      },
    ],
    'Search Nodes': [
      {
        Name: 'server1.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB',
        },
      },
      {
        Name: 'server2.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB',
        },
      },
      {
        Name: 'server3.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB',
        },
      },
    ],
    Statistics: {
      ncloc: 989880,
    },
    ...overrides,
  };
}

export function mockCondition(overrides: Partial<Condition> = {}): Condition {
  return {
    error: '10',
    id: '1',
    metric: 'coverage',
    op: 'LT',
    ...overrides,
  };
}

export function mockCurrentUser(overrides: Partial<CurrentUser> = {}): CurrentUser {
  return {
    isLoggedIn: false,
    dismissedNotices: {
      educationPrinciples: false,
    },
    ...overrides,
  };
}

export function mockLoggedInUser(overrides: Partial<LoggedInUser> = {}): LoggedInUser {
  return {
    groups: [],
    isLoggedIn: true,
    login: 'luke',
    name: 'Skywalker',
    scmAccounts: [],
    dismissedNotices: {
      educationPrinciples: false,
    },
    ...overrides,
  };
}

export function mockGroup(overrides: Partial<Group> = {}): Group {
  return {
    id: Math.random().toString(),
    name: 'Foo',
    managed: false,
    ...overrides,
  };
}

export function mockGroupMembership(overrides: Partial<GroupMembership> = {}): GroupMembership {
  return {
    id: Math.random().toString(),
    userId: Math.random().toString(),
    groupId: Math.random().toString(),
    ...overrides,
  };
}

export function mockRawIssue(withLocations = false, overrides: Partial<RawIssue> = {}): RawIssue {
  const rawIssue: RawIssue = {
    actions: [],
    component: 'main.js',
    key: 'AVsae-CQS-9G3txfbFN2',
    creationDate: '2023-01-15T09:36:01+0100',
    line: 25,
    project: 'myproject',
    rule: 'javascript:S1067',
    severity: IssueSeverity.Major,
    textRange: { startLine: 25, endLine: 26, startOffset: 0, endOffset: 15 },
    type: IssueType.CodeSmell,
    status: IssueDeprecatedStatus.Open,
    issueStatus: IssueStatus.Open,
    transitions: [],
    scope: IssueScope.Main,
    cleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
    cleanCodeAttribute: CleanCodeAttribute.Respectful,
    impacts: [
      { softwareQuality: SoftwareQuality.Maintainability, severity: SoftwareImpactSeverity.Medium },
    ],
    ...overrides,
  };

  if (withLocations) {
    const loc = mockFlowLocation;

    rawIssue.flows = [
      {
        locations: [
          loc({ component: overrides.component }),
          loc({ component: overrides.component }),
        ],
      },
    ];
  }

  return {
    ...rawIssue,
    ...overrides,
  };
}

export function mockIssue(withLocations = false, overrides: Partial<Issue> = {}): Issue {
  const issue: Issue = {
    actions: [],
    component: 'main.js',
    componentEnabled: true,
    componentLongName: 'main.js',
    componentQualifier: 'FIL',
    componentUuid: 'foo1234',
    creationDate: '2017-03-01T09:36:01+0100',
    flows: [],
    flowsWithType: [],
    key: 'AVsae-CQS-9G3txfbFN2',
    line: 25,
    message: 'Reduce the number of conditional operators (4) used in the expression',
    project: 'myproject',
    projectKey: 'foo',
    projectName: 'Foo',
    rule: 'javascript:S1067',
    ruleName: 'foo',
    scope: IssueScope.Main,
    secondaryLocations: [],
    severity: IssueSeverity.Major,
    status: IssueDeprecatedStatus.Open,
    issueStatus: IssueStatus.Open,
    textRange: { startLine: 25, endLine: 26, startOffset: 0, endOffset: 15 },
    transitions: [],
    type: 'BUG',
    cleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
    cleanCodeAttribute: CleanCodeAttribute.Respectful,
    impacts: [
      { softwareQuality: SoftwareQuality.Maintainability, severity: SoftwareImpactSeverity.Medium },
    ],
  };

  const loc = mockFlowLocation;

  if (withLocations) {
    issue.flows = [
      [loc(), loc(), loc()],
      [loc(), loc()],
    ];
    issue.secondaryLocations = [loc(), loc()];
  }

  return {
    ...issue,
    ...overrides,
  };
}

export function mockLocation(overrides: Partial<Location> = {}): Location {
  return {
    hash: '',
    key: 'key',
    pathname: '/path',
    query: {},
    search: '',
    state: {},
    ...overrides,
  };
}

export function mockMetric(
  overrides: Partial<Pick<Metric, 'key' | 'name' | 'type' | 'domain'>> = {},
): Metric {
  const key = overrides.key || MetricKey.coverage;
  const name = overrides.name || key;
  const type = overrides.type || MetricType.Percent;
  return {
    ...overrides,
    key,
    name,
    type,
  };
}

export function mockMeasure(overrides: Partial<Measure> = {}): Measure {
  return {
    bestValue: true,
    metric: 'bugs',
    period: {
      bestValue: true,
      index: 1,
      value: '1.0',
    },
    value: '1.0',
    ...overrides,
  };
}

export function mockMeasureEnhanced(overrides: Partial<MeasureEnhanced> = {}): MeasureEnhanced {
  return {
    bestValue: true,
    leak: '1',
    metric: mockMetric({ ...(overrides.metric || {}) }),
    period: {
      bestValue: true,
      index: 1,
      value: '1.0',
    },
    value: '1.0',
    ...overrides,
  };
}

export function mockNotification(overrides: Partial<Notification> = {}): Notification {
  return {
    channel: 'channel1',
    type: 'type-global',
    project: 'foo',
    projectName: 'Foo',
    ...overrides,
  };
}

export function mockPeriod(overrides: Partial<Period> = {}): Period {
  return {
    date: '2019-04-23T02:12:32+0100',
    index: 0,
    mode: 'previous_version',
    ...overrides,
  };
}

export function mockQualityProfile(overrides: Partial<Profile> = {}): Profile {
  return {
    activeDeprecatedRuleCount: 2,
    activeRuleCount: 10,
    childrenCount: 0,
    depth: 1,
    isBuiltIn: false,
    isDefault: false,
    isInherited: false,
    key: 'key',
    language: 'js',
    languageName: 'JavaScript',
    name: 'name',
    projectCount: 3,
    ...overrides,
  };
}

export function mockCompareResult(overrides: Partial<CompareResponse> = {}): CompareResponse {
  return {
    left: { name: 'Profile A' },
    right: { name: 'Profile B' },
    inLeft: [
      {
        key: 'java:S4604',
        name: 'Rule in left',
        cleanCodeAttributeCategory: CleanCodeAttributeCategory.Adaptable,
        impacts: [
          {
            softwareQuality: SoftwareQuality.Maintainability,
            severity: SoftwareImpactSeverity.Medium,
          },
        ],
      },
    ],
    inRight: [
      {
        key: 'java:S5128',
        name: 'Rule in right',
        cleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
        impacts: [
          {
            softwareQuality: SoftwareQuality.Security,
            severity: SoftwareImpactSeverity.Medium,
          },
        ],
      },
    ],
    modified: [
      {
        impacts: [],
        key: 'java:S1698',
        name: '== and != should not be used when equals is overridden',
        left: { params: {}, severity: 'MINOR' },
        right: { params: {}, severity: 'CRITICAL' },
      },
    ],
    ...overrides,
  };
}

export function mockQualityProfileChangelogEvent(
  eventOverride?: Partial<ProfileChangelogEvent>,
): ProfileChangelogEvent {
  return {
    action: 'ACTIVATED',
    date: '2019-04-23T02:12:32+0100',
    params: {
      severity: IssueSeverity.Major,
    },
    cleanCodeAttributeCategory: CleanCodeAttributeCategory.Responsible,
    impacts: [
      {
        softwareQuality: SoftwareQuality.Maintainability,
        severity: SoftwareImpactSeverity.Low,
      },
      {
        softwareQuality: SoftwareQuality.Security,
        severity: SoftwareImpactSeverity.High,
      },
    ],
    ruleKey: 'rule-key',
    ruleName: 'rule-name',
    sonarQubeVersion: '10.3',
    ...eventOverride,
  };
}

export function mockQualityProfileExporter(override?: Partial<Exporter>): Exporter {
  return {
    key: 'exporter-key',
    name: 'exporter-name',
    languages: ['first-lang', 'second-lang'],
    ...override,
  };
}

export function mockRouter(
  overrides: {
    push?: (loc: To) => void;
    replace?: (loc: To) => void;
  } = {},
) {
  return {
    createHref: jest.fn(),
    createPath: jest.fn(),
    go: jest.fn(),
    goBack: jest.fn(),
    goForward: jest.fn(),
    isActive: jest.fn(),
    navigate: jest.fn(),
    push: jest.fn(),
    replace: jest.fn(),
    searchParams: new URLSearchParams(),
    setRouteLeaveHook: jest.fn(),
    setSearchParams: jest.fn(),
    ...overrides,
  } as Router;
}

export function mockRule(overrides: Partial<Rule> = {}): Rule {
  return {
    key: 'javascript:S1067',
    lang: 'js',
    langName: 'JavaScript',
    name: 'Use foo',
    severity: 'MAJOR',
    status: 'READY',
    sysTags: ['a', 'b'],
    tags: ['x'],
    type: 'CODE_SMELL',
    ...overrides,
  } as Rule;
}

export function mockRuleActivation(overrides: Partial<RuleActivation> = {}): RuleActivation {
  return {
    createdAt: '2020-02-01',
    inherit: 'NONE',
    params: [{ key: 'foo', value: 'Bar' }],
    qProfile: 'baz',
    severity: 'MAJOR',
    ...overrides,
  };
}

export function mockRuleDetails(overrides: Partial<RuleDetails> = {}): RuleDetails {
  return {
    cleanCodeAttributeCategory: CleanCodeAttributeCategory.Intentional,
    cleanCodeAttribute: CleanCodeAttribute.Clear,
    key: 'squid:S1337',
    repo: 'squid',
    name: '".equals()" should not be used to test the values of "Atomic" classes',
    createdAt: '2014-12-16T17:26:54+0100',
    descriptionSections: [
      {
        key: RuleDescriptionSections.DEFAULT,
        content: '<b>Why</b> Because',
      },
    ],
    htmlDesc: '',
    mdDesc: '',
    severity: 'MAJOR',
    status: 'READY',
    isTemplate: false,
    impacts: [
      { softwareQuality: SoftwareQuality.Maintainability, severity: SoftwareImpactSeverity.High },
    ],
    tags: [],
    sysTags: ['multi-threading'],
    lang: 'java',
    langName: 'Java',
    params: [],
    defaultRemFnType: 'CONSTANT_ISSUE',
    defaultRemFnBaseEffort: '5min',
    remFnType: 'CONSTANT_ISSUE',
    remFnBaseEffort: '5min',
    remFnOverloaded: false,
    scope: 'MAIN',
    isExternal: false,
    type: 'BUG',
    ...overrides,
  };
}

export function mockRestRuleDetails(overrides: Partial<RestRuleDetails> = {}): RestRuleDetails {
  const ruleDetails = mockRuleDetails(overrides);
  return {
    ...omit(ruleDetails, Object.values(REST_RULE_KEYS_TO_OLD_KEYS)),
    ...Object.entries(REST_RULE_KEYS_TO_OLD_KEYS).reduce(
      (obj, [key, value]: [keyof RestRuleDetails, keyof RuleDetails]) => {
        obj[key] = ruleDetails[value] as never;
        return obj;
      },
      {} as RestRuleDetails,
    ),
    ...overrides,
  };
}

export function mockRuleDetailsParameter(overrides: Partial<RuleParameter> = {}): RuleParameter {
  return {
    defaultValue: '1',
    htmlDesc: 'description',
    key: '1',
    type: 'number',
    ...overrides,
  };
}

export function mockLogs(logsLevel: LogsLevels = LogsLevels.INFO): SysInfoLogging {
  return { 'Logs Level': logsLevel, 'Logs Dir': '/logs' };
}

export function mockStandaloneSysInfo(overrides: Partial<any> = {}): SysInfoStandalone {
  const baseInfo = mockBaseSysInfo(overrides);
  return {
    ...baseInfo,
    System: {
      ...baseInfo.System,
      'High Availability': false,
      'Server ID': 'asd564-asd54a-5dsfg45',
    },
    Settings: {
      'sonar.cluster.enabled': 'true',
      'sonar.cluster.node.name': 'server9.example.com',
    },
    'Web JVM State': {
      'Max Memory (MB)': 1024,
      'Free Memory (MB)': 111,
    },
    'Web Database Connection': {
      'Pool Active Connections': 0,
      'Pool Max Connections': 60,
    },
    'Web Logging': mockLogs(),
    'Web JVM Properties': {
      'file.encoding': 'UTF-8',
      'file.separator': '/',
    },
    'Compute Engine Tasks': {
      Pending: 0,
      'In Progress': 0,
    },
    'Compute Engine JVM State': {
      'Max Memory (MB)': 1024,
      'Free Memory (MB)': 89,
    },
    'Compute Engine Database Connection': {
      'Pool Initial Size': 0,
      'Pool Active Connections': 0,
    },
    'Compute Engine Logging': mockLogs(),
    'Compute Engine JVM Properties': {
      'file.encoding': 'UTF-8',
      'file.separator': '/',
    },
    ALMs: {},
    Bundled: {},
    Plugins: {},
    ...overrides,
  };
}

export function mockUser(overrides: Partial<User> = {}): User {
  return {
    active: true,
    local: true,
    login: 'john.doe',
    name: 'John Doe',
    managed: false,
    ...overrides,
  };
}

export function mockRestUser(overrides: Partial<RestUserDetailed> = {}): RestUserDetailed {
  return {
    id: Math.random().toString(),
    login: 'buzz.aldrin',
    name: 'Buzz Aldrin',
    email: 'buzz.aldrin@nasa.com',
    active: true,
    local: true,
    managed: false,
    externalProvider: '',
    externalLogin: '',
    sonarQubeLastConnectionDate: null,
    sonarLintLastConnectionDate: null,
    scmAccounts: [],
    avatar: 'buzzonthemoon',
    ...overrides,
  };
}

export function mockUserSelected(overrides: Partial<UserSelected> = {}): UserSelected {
  return {
    active: true,
    login: 'john.doe',
    name: 'John Doe',
    selected: true,
    ...overrides,
  };
}

export function mockUserGroupMember(overrides: Partial<UserGroupMember> = {}): UserGroupMember {
  return {
    login: 'john.doe',
    name: 'John Doe',
    managed: false,
    selected: true,
    ...overrides,
  };
}

export function mockDocumentationMarkdown(
  overrides: Partial<{ content: string; title: string; key: string }> = {},
): string {
  const content =
    overrides.content ||
    `
## Lorem Ipsum

Donec at est elit. In finibus justo ut augue rhoncus, vitae consequat mauris mattis.
Nunc ante est, volutpat ac volutpat ac, pharetra in libero.
`;

  const frontMatter = `
---
${overrides.title ? 'title: ' + overrides.title : ''}
${overrides.key ? 'key: ' + overrides.key : ''}
---`;

  return `${frontMatter}
${content}`;
}

export function mockLanguage(overrides: Partial<Language> = {}): Language {
  return {
    key: 'css',
    name: 'CSS',
    ...overrides,
  };
}

export function mockFlowLocation(overrides: Partial<FlowLocation> = {}): FlowLocation {
  return {
    component: 'main.js',
    textRange: {
      startLine: 1,
      startOffset: 1,
      endLine: 2,
      endOffset: 2,
    },
    ...overrides,
  };
}

export function mockIdentityProvider(overrides: Partial<IdentityProvider> = {}): IdentityProvider {
  return {
    backgroundColor: '#000000',
    iconPath: '/path/icon.svg',
    key: 'github',
    name: 'Github',
    ...overrides,
  };
}

export function mockRef(
  overrides: Partial<React.RefObject<Partial<HTMLElement>>> = {},
): React.RefObject<HTMLElement> {
  return {
    current: {
      getBoundingClientRect: jest.fn(),
      ...overrides.current,
    },
  } as React.RefObject<HTMLElement>;
}

export function mockPaging(overrides: Partial<Paging> = {}): Paging {
  return {
    pageIndex: 1,
    pageSize: 100,
    total: 1000,
    ...overrides,
  };
}

export function mockDumpTask(props: Partial<DumpTask> = {}): DumpTask {
  return {
    status: TaskStatuses.Success,
    startedAt: '2020-03-12T12:20:20Z',
    submittedAt: '2020-03-12T12:15:20Z',
    executedAt: '2020-03-12T12:22:20Z',
    ...props,
  };
}

export function mockDumpStatus(props: Partial<DumpStatus> = {}): DumpStatus {
  return {
    canBeExported: true,
    canBeImported: true,
    dumpToImport: '',
    exportedDump: '',
    ...props,
  };
}

export function mockRuleRepository(override: Partial<RuleRepository> = {}) {
  return { key: 'css', language: 'css', name: 'SonarQube', ...override };
}
