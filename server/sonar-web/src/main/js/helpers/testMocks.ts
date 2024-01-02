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
import { To } from 'react-router-dom';
import { CompareResponse } from '../api/quality-profiles';
import { RuleDescriptionSections } from '../apps/coding-rules/rule';
import { Exporter, Profile } from '../apps/quality-profiles/types';
import { Location, Router } from '../components/hoc/withRouter';
import { AppState } from '../types/appstate';
import { RuleRepository } from '../types/coding-rules';
import { EditionKey } from '../types/editions';
import { IssueType, RawIssue } from '../types/issues';
import { Language } from '../types/languages';
import { DumpStatus, DumpTask } from '../types/project-dump';
import { TaskStatuses } from '../types/tasks';
import {
  AlmApplication,
  Condition,
  FlowLocation,
  Group,
  HealthType,
  IdentityProvider,
  Issue,
  Measure,
  MeasureEnhanced,
  Metric,
  Paging,
  Period,
  Permission,
  PermissionGroup,
  PermissionTemplate,
  PermissionTemplateGroup,
  PermissionUser,
  ProfileInheritanceDetails,
  Rule,
  RuleActivation,
  RuleDetails,
  RuleParameter,
  SysInfoBase,
  SysInfoCluster,
  SysInfoStandalone,
} from '../types/types';
import { CurrentUser, LoggedInUser, User } from '../types/users';

export function mockAlmApplication(overrides: Partial<AlmApplication> = {}): AlmApplication {
  return {
    backgroundColor: '#444444',
    iconPath: '/images/sonarcloud/github-white.svg',
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
    qualifiers: ['TRK'],
    settings: {},
    version: '1.0',
    ...overrides,
  };
}

export function mockBaseSysInfo(overrides: Partial<any> = {}): SysInfoBase {
  return {
    Health: 'GREEN' as HealthType,
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
        Name: 'server9.example.com',
        Host: '10.0.0.0',
        Health: 'GREEN' as HealthType,
        'Health Causes': [],
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
        Name: 'server9.example.com',
        Host: '10.0.0.0',
        Health: 'GREEN' as HealthType,
        'Health Causes': [],
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
        Name: 'server.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB',
        },
      },
      {
        Name: 'server.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB',
        },
      },
      {
        Name: 'server.example.com',
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
    id: 1,
    membersCount: 1,
    name: 'Foo',
    ...overrides,
  };
}

export function mockRawIssue(withLocations = false, overrides: Partial<RawIssue> = {}): RawIssue {
  const rawIssue: RawIssue = {
    actions: [],
    component: 'main.js',
    key: 'AVsae-CQS-9G3txfbFN2',
    line: 25,
    project: 'myproject',
    rule: 'javascript:S1067',
    severity: 'MAJOR',
    status: 'OPEN',
    textRange: { startLine: 25, endLine: 26, startOffset: 0, endOffset: 15 },
    type: IssueType.CodeSmell,
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

export function mockIssue(withLocations = false, overrides: Partial<Issue> = {}) {
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
    secondaryLocations: [],
    severity: 'MAJOR',
    status: 'OPEN',
    textRange: { startLine: 25, endLine: 26, startOffset: 0, endOffset: 15 },
    transitions: [],
    type: 'BUG',
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

export function mockMetric(overrides: Partial<Pick<Metric, 'key' | 'name' | 'type'>> = {}): Metric {
  const key = overrides.key || 'coverage';
  const name = overrides.name || key;
  const type = overrides.type || 'PERCENT';
  return {
    id: key,
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
        severity: 'MINOR',
      },
    ],
    inRight: [
      {
        key: 'java:S5128',
        name: 'Rule in right',
        severity: 'MAJOR',
      },
    ],
    modified: [
      {
        key: 'java:S1698',
        name: '== and != should not be used when equals is overridden',
        left: { params: {}, severity: 'MINOR' },
        right: { params: {}, severity: 'CRITICAL' },
      },
    ],
    ...overrides,
  };
}

export function mockQualityProfileInheritance(
  overrides: Partial<ProfileInheritanceDetails> = {}
): ProfileInheritanceDetails {
  return {
    activeRuleCount: 4,
    isBuiltIn: false,
    key: 'foo',
    name: 'Foo',
    overridingRuleCount: 0,
    ...overrides,
  };
}

export function mockQualityProfileChangelogEvent(eventOverride?: any) {
  return {
    action: 'ACTIVATED',
    date: '2019-04-23T02:12:32+0100',
    params: {
      severity: 'MAJOR',
    },
    ruleKey: 'rule-key',
    ruleName: 'rule-name',
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
  } = {}
) {
  return {
    createHref: jest.fn(),
    createPath: jest.fn(),
    go: jest.fn(),
    goBack: jest.fn(),
    goForward: jest.fn(),
    isActive: jest.fn(),
    push: jest.fn(),
    replace: jest.fn(),
    setRouteLeaveHook: jest.fn(),
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
    tags: [],
    sysTags: ['multi-threading'],
    lang: 'java',
    langName: 'Java',
    params: [],
    defaultDebtRemFnType: 'CONSTANT_ISSUE',
    defaultDebtRemFnOffset: '5min',
    debtOverloaded: false,
    debtRemFnType: 'CONSTANT_ISSUE',
    debtRemFnOffset: '5min',
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

export function mockRuleDetailsParameter(overrides: Partial<RuleParameter> = {}): RuleParameter {
  return {
    defaultValue: '1',
    htmlDesc: 'description',
    key: '1',
    type: 'number',
    ...overrides,
  };
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
    'Web Logging': { 'Logs Level': 'INFO', 'Logs Dir': '/logs' },
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
      'Logs Level': 'DEBUG',
      'Logs Dir': '/logs',
    },
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
    ...overrides,
  };
}

export function mockDocumentationMarkdown(
  overrides: Partial<{ content: string; title: string; key: string }> = {}
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
  overrides: Partial<React.RefObject<Partial<HTMLElement>>> = {}
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

export function mockPermission(override: Partial<Permission> = {}) {
  return {
    key: 'admin',
    name: 'Admin',
    description: 'Can do anything he/she wants',
    ...override,
  };
}

export function mockPermissionTemplateGroup(override: Partial<PermissionTemplateGroup> = {}) {
  return {
    groupsCount: 1,
    usersCount: 1,
    key: 'admin',
    withProjectCreator: true,
    ...override,
  };
}

export function mockPermissionTemplate(override: Partial<PermissionTemplate> = {}) {
  return {
    id: 'template1',
    name: 'Permission Template 1',
    createdAt: '',
    defaultFor: [],
    permissions: [mockPermissionTemplateGroup()],
    ...override,
  };
}

export function mockTemplateUser(override: Partial<PermissionUser> = {}) {
  return {
    login: 'admin',
    name: 'Admin Admin',
    permissions: ['admin', 'codeviewer'],
    ...override,
  };
}

export function mockTemplateGroup(override: Partial<PermissionGroup> = {}) {
  return {
    id: 'Anyone',
    name: 'Anyone',
    description: 'everyone',
    permissions: ['admin', 'codeviewer'],
    ...override,
  };
}
