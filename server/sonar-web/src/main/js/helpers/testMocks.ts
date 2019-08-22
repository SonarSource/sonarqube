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
/* eslint-disable sonarjs/no-duplicate-string */
import { Location } from 'history';
import { InjectedRouter } from 'react-router';
import { createStore, Store } from 'redux';
import { DocumentationEntry } from '../apps/documentation/utils';
import { ParsedAnalysis } from '../apps/projectActivity/utils';
import { Profile } from '../apps/quality-profiles/types';

export function mockAlmApplication(overrides: Partial<T.AlmApplication> = {}): T.AlmApplication {
  return {
    backgroundColor: '#444444',
    iconPath: '/images/sonarcloud/github-white.svg',
    installationUrl: 'https://github.com/apps/greg-sonarcloud/installations/new',
    key: 'github',
    name: 'GitHub',
    ...overrides
  };
}

export function mockAlmOrganization(overrides: Partial<T.AlmOrganization> = {}): T.AlmOrganization {
  return {
    avatar: 'http://example.com/avatar',
    almUrl: 'https://github.com/foo',
    description: 'description-foo',
    key: 'foo',
    name: 'foo',
    personal: false,
    privateRepos: 0,
    publicRepos: 3,
    url: 'http://example.com/foo',
    ...overrides
  };
}

export function mockParsedAnalysis(overrides: Partial<ParsedAnalysis> = {}): ParsedAnalysis {
  return {
    date: new Date('2017-03-01T09:36:01+0100'),
    events: [],
    key: 'foo',
    projectVersion: '1.0',
    ...overrides
  };
}

export function mockAnalysisEvent(overrides: Partial<T.AnalysisEvent> = {}): T.AnalysisEvent {
  return {
    category: 'QUALITY_GATE',
    key: 'E11',
    description: 'Lorem ipsum dolor sit amet',
    name: 'Lorem ipsum',
    qualityGate: {
      status: 'ERROR',
      stillFailing: true,
      failing: [
        {
          key: 'foo',
          name: 'Foo',
          branch: 'master'
        },
        {
          key: 'bar',
          name: 'Bar',
          branch: 'feature/bar'
        }
      ]
    },
    ...overrides
  };
}

export function mockAppState(overrides: Partial<T.AppState> = {}): T.AppState {
  return {
    defaultOrganization: 'foo',
    edition: 'community',
    productionDatabase: true,
    qualifiers: ['TRK'],
    settings: {},
    version: '1.0',
    ...overrides
  };
}

export function mockBaseSysInfo(overrides: Partial<any> = {}): T.SysInfoBase {
  return {
    Health: 'GREEN' as T.HealthType,
    'Health Causes': [],
    System: {
      Version: '7.8'
    },
    Database: {
      Database: 'PostgreSQL',
      'Database Version': '10.3',
      Username: 'sonar',
      URL: 'jdbc:postgresql://localhost/sonar',
      Driver: 'PostgreSQL JDBC Driver',
      'Driver Version': '42.2.5'
    },
    'Compute Engine Tasks': {
      'Total Pending': 0,
      'Total In Progress': 0
    },
    'Search State': { State: 'GREEN', Nodes: 3 },
    'Search Indexes': {
      'Index components - Docs': 30445,
      'Index components - Shards': 10
    },
    ...overrides
  };
}

export function mockClusterSysInfo(overrides: Partial<any> = {}): T.SysInfoCluster {
  const baseInfo = mockBaseSysInfo(overrides);
  return {
    ...baseInfo,
    System: {
      ...baseInfo.System,
      'High Availability': true,
      'Server ID': 'asd564-asd54a-5dsfg45'
    },
    Settings: {
      'sonar.cluster.enabled': 'true',
      'sonar.cluster.node.name': 'server9.example.com'
    },
    'Application Nodes': [
      {
        Name: 'server9.example.com',
        Host: '10.0.0.0',
        Health: 'GREEN' as T.HealthType,
        'Health Causes': [],
        System: {
          Version: '7.8'
        },
        Plugins: {
          java: '5.13.0.17924 [SonarJava]'
        },
        'Web JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 122
        },
        'Web Database Connection': {
          'Pool Active Connections': 1
        },
        'Web Logging': { 'Logs Level': 'DEBUG' },
        'Web JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/'
        },
        'Compute Engine Tasks': {
          Pending: 0,
          'In Progress': 0
        },
        'Compute Engine JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 78
        },
        'Compute Engine Database Connection': {
          'Pool Initial Size': 0,
          'Pool Active Connections': 0
        },
        'Compute Engine Logging': {
          'Logs Level': 'INFO'
        },
        'Compute Engine JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/'
        }
      },
      {
        Name: 'server9.example.com',
        Host: '10.0.0.0',
        Health: 'GREEN' as T.HealthType,
        'Health Causes': [],
        System: {
          Version: '7.8'
        },
        Plugins: {
          java: '5.13.0.17924 [SonarJava]'
        },
        'Web JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 111
        },
        'Web Database Connection': {
          'Pool Active Connections': 0,
          'Pool Max Connections': 60
        },
        'Web Logging': { 'Logs Level': 'INFO' },
        'Web JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/'
        },
        'Compute Engine Tasks': {
          Pending: 0,
          'In Progress': 0
        },
        'Compute Engine JVM State': {
          'Max Memory (MB)': 1024,
          'Free Memory (MB)': 89
        },
        'Compute Engine Database Connection': {
          'Pool Initial Size': 0,
          'Pool Active Connections': 0
        },
        'Compute Engine Logging': {
          'Logs Level': 'INFO'
        },
        'Compute Engine JVM Properties': {
          'file.encoding': 'UTF-8',
          'file.separator': '/'
        }
      }
    ],
    'Search Nodes': [
      {
        Name: 'server.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB'
        }
      },
      {
        Name: 'server.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB'
        }
      },
      {
        Name: 'server.example.com',
        Host: '10.0.0.0',
        'Search State': {
          'CPU Usage (%)': 0,
          'Disk Available': '93 GB'
        }
      }
    ],
    Statistics: {
      ncloc: 989880
    },
    ...overrides
  };
}

export function mockComponent(overrides: Partial<T.Component> = {}): T.Component {
  return {
    breadcrumbs: [],
    key: 'my-project',
    name: 'MyProject',
    organization: 'foo',
    qualifier: 'TRK',
    qualityGate: { isDefault: true, key: '30', name: 'Sonar way' },
    qualityProfiles: [
      {
        deleted: false,
        key: 'my-qp',
        language: 'ts',
        name: 'Sonar way'
      }
    ],
    tags: [],
    ...overrides
  };
}

export function mockComponentMeasure(
  file = false,
  overrides: Partial<T.ComponentMeasure> = {}
): T.ComponentMeasure {
  if (file) {
    return {
      key: 'foo:src/index.tsx',
      name: 'index.tsx',
      qualifier: 'FIL',
      path: 'src/index.tsx',
      measures: [{ metric: 'bugs', value: '1', bestValue: false }],
      ...overrides
    };
  }
  return {
    key: 'foo',
    name: 'Foo',
    qualifier: 'TRK',
    measures: [{ metric: 'bugs', value: '12', bestValue: false }],
    ...overrides
  };
}

export function mockCondition(overrides: Partial<T.Condition> = {}): T.Condition {
  return {
    error: '10',
    id: 1,
    metric: 'coverage',
    op: 'LT',
    ...overrides
  };
}

export function mockQualityGateStatusCondition(
  overrides: Partial<T.QualityGateStatusCondition> = {}
): T.QualityGateStatusCondition {
  return {
    actual: '10',
    error: '0',
    level: 'ERROR',
    metric: 'foo',
    op: 'GT',
    ...overrides
  };
}

export function mockSnippetsByComponent(
  component = 'main.js',
  lines: number[] = [16]
): T.SnippetsByComponent {
  const sources = lines.reduce((lines: { [key: number]: T.SourceLine }, line) => {
    lines[line] = mockSourceLine({ line });
    return lines;
  }, {});
  return {
    component: mockSourceViewerFile({
      key: component,
      path: component
    }),
    sources
  };
}

export function mockSourceLine(overrides: Partial<T.SourceLine> = {}): T.SourceLine {
  return {
    line: 16,
    code: '<span class="k">import</span> java.util.<span class="sym-9 sym">ArrayList</span>;',
    coverageStatus: 'covered',
    coveredConditions: 2,
    scmRevision: '80f564becc0c0a1c9abaa006eca83a4fd278c3f0',
    scmAuthor: 'simon.brandhof@sonarsource.com',
    scmDate: '2018-12-11T10:48:39+0100',
    duplicated: false,
    isNew: true,
    ...overrides
  };
}

export function mockCurrentUser(overrides: Partial<T.CurrentUser> = {}): T.CurrentUser {
  return {
    isLoggedIn: false,
    ...overrides
  };
}

export function mockLoggedInUser(overrides: Partial<T.LoggedInUser> = {}): T.LoggedInUser {
  return {
    groups: [],
    isLoggedIn: true,
    login: 'luke',
    name: 'Skywalker',
    scmAccounts: [],
    ...overrides
  };
}

export function mockEvent(overrides = {}) {
  return {
    target: { blur() {} },
    currentTarget: { blur() {} },
    preventDefault() {},
    stopPropagation() {},
    ...overrides
  } as any;
}

export function mockIssue(withLocations = false, overrides: Partial<T.Issue> = {}) {
  const issue: T.Issue = {
    actions: [],
    component: 'main.js',
    componentLongName: 'main.js',
    componentQualifier: 'FIL',
    componentUuid: 'foo1234',
    creationDate: '2017-03-01T09:36:01+0100',
    flows: [],
    fromHotspot: false,
    key: 'AVsae-CQS-9G3txfbFN2',
    line: 25,
    message: 'Reduce the number of conditional operators (4) used in the expression',
    organization: 'myorg',
    project: 'myproject',
    projectKey: 'foo',
    projectName: 'Foo',
    projectOrganization: 'org',
    rule: 'javascript:S1067',
    ruleName: 'foo',
    secondaryLocations: [],
    severity: 'MAJOR',
    status: 'OPEN',
    textRange: { startLine: 25, endLine: 26, startOffset: 0, endOffset: 15 },
    transitions: [],
    type: 'BUG'
  };

  const loc = mockFlowLocation;

  if (withLocations) {
    issue.flows = [[loc(), loc(), loc()], [loc(), loc()]];
    issue.secondaryLocations = [loc(), loc()];
  }

  return {
    ...issue,
    ...overrides
  };
}

export function mockLocation(overrides: Partial<Location> = {}): Location {
  return {
    action: 'PUSH',
    hash: '',
    key: 'key',
    pathname: '/path',
    query: {},
    search: '',
    state: {},
    ...overrides
  };
}

export function mockMetric(overrides: Partial<T.Metric> = {}): T.Metric {
  return {
    id: 'coverage',
    key: 'coverage',
    name: 'Coverage',
    type: 'PERCENT',
    ...overrides
  };
}

export function mockMeasure(overrides: Partial<T.Measure> = {}): T.Measure {
  return {
    bestValue: true,
    metric: 'bugs',
    periods: [
      {
        bestValue: true,
        index: 1,
        value: '1.0'
      }
    ],
    value: '1.0',
    ...overrides
  };
}

export function mockMeasureEnhanced(overrides: Partial<T.MeasureEnhanced> = {}): T.MeasureEnhanced {
  return {
    bestValue: true,
    leak: '1',
    metric: mockMetric({ ...(overrides.metric || {}) }),
    periods: [
      {
        bestValue: true,
        index: 1,
        value: '1.0'
      }
    ],
    value: '1.0',
    ...overrides
  };
}

export function mockOrganization(overrides: Partial<T.Organization> = {}): T.Organization {
  return { key: 'foo', name: 'Foo', ...overrides };
}

export function mockOrganizationWithAdminActions(
  overrides: Partial<T.Organization> = {},
  actionsOverrides: Partial<T.Organization['actions']> = {}
) {
  return mockOrganization({ actions: { admin: true, ...actionsOverrides }, ...overrides });
}

export function mockOrganizationWithAlm(
  overrides: Partial<T.Organization> = {},
  almOverrides: Partial<T.Organization['alm']> = {}
): T.Organization {
  return mockOrganization({
    alm: {
      key: 'github',
      membersSync: false,
      personal: false,
      url: 'https://github.com/foo',
      ...almOverrides
    },
    ...overrides
  });
}

export function mockQualityGate(overrides: Partial<T.QualityGate> = {}): T.QualityGate {
  return {
    id: 1,
    name: 'qualitygate',
    ...overrides
  };
}

export function mockPullRequest(overrides: Partial<T.PullRequest> = {}): T.PullRequest {
  return {
    analysisDate: '2018-01-01',
    base: 'master',
    branch: 'feature/foo/bar',
    key: '1001',
    target: 'master',
    title: 'Foo Bar feature',
    ...overrides
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
    organization: 'foo',
    ...overrides
  };
}

export function mockQualityProfileInheritance(
  overrides: Partial<T.ProfileInheritanceDetails> = {}
): T.ProfileInheritanceDetails {
  return {
    activeRuleCount: 4,
    isBuiltIn: false,
    key: 'foo',
    name: 'Foo',
    overridingRuleCount: 0,
    ...overrides
  };
}

export function mockQualityGateProjectStatus(
  overrides: Partial<T.QualityGateProjectStatus> = {}
): T.QualityGateProjectStatus {
  return {
    conditions: [
      {
        actualValue: '0',
        comparator: 'GT',
        errorThreshold: '1.0',
        metricKey: 'new_bugs',
        periodIndex: 1,
        status: 'OK'
      }
    ],
    ignoredConditions: false,
    status: 'OK',
    ...overrides
  };
}

export function mockRouter(overrides: { push?: Function; replace?: Function } = {}) {
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
    ...overrides
  } as InjectedRouter;
}

export function mockRule(overrides: Partial<T.Rule> = {}): T.Rule {
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
    ...overrides
  } as T.Rule;
}

export function mockRuleDetails(overrides: Partial<T.RuleDetails> = {}): T.RuleDetails {
  return {
    key: 'squid:S1337',
    repo: 'squid',
    name: '".equals()" should not be used to test the values of "Atomic" classes',
    createdAt: '2014-12-16T17:26:54+0100',
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
    ...overrides
  };
}

export function mockRuleDetailsParameter(
  overrides: Partial<T.RuleParameter> = {}
): T.RuleParameter {
  return {
    defaultValue: '1',
    htmlDesc: 'description',
    key: '1',
    type: 'number',
    ...overrides
  };
}

export function mockShortLivingBranch(
  overrides: Partial<T.ShortLivingBranch> = {}
): T.ShortLivingBranch {
  return {
    analysisDate: '2018-01-01',
    isMain: false,
    name: 'feature/foo',
    mergeBranch: 'master',
    type: 'SHORT',
    ...overrides
  };
}

export function mockSourceViewerFile(
  overrides: Partial<T.SourceViewerFile> = {}
): T.SourceViewerFile {
  return {
    key: 'foo',
    measures: {
      coverage: '85.2',
      duplicationDensity: '1.0',
      issues: '12',
      lines: '56'
    },
    path: 'foo/bar.ts',
    project: 'my-project',
    projectName: 'MyProject',
    q: 'FIL',
    uuid: 'foo-bar',
    ...overrides
  };
}

export function mockStandaloneSysInfo(overrides: Partial<any> = {}): T.SysInfoStandalone {
  const baseInfo = mockBaseSysInfo(overrides);
  return {
    ...baseInfo,
    System: {
      ...baseInfo.System,
      'High Availability': false,
      'Server ID': 'asd564-asd54a-5dsfg45'
    },
    Settings: {
      'sonar.cluster.enabled': 'true',
      'sonar.cluster.node.name': 'server9.example.com'
    },
    'Web JVM State': {
      'Max Memory (MB)': 1024,
      'Free Memory (MB)': 111
    },
    'Web Database Connection': {
      'Pool Active Connections': 0,
      'Pool Max Connections': 60
    },
    'Web Logging': { 'Logs Level': 'INFO', 'Logs Dir': '/logs' },
    'Web JVM Properties': {
      'file.encoding': 'UTF-8',
      'file.separator': '/'
    },
    'Compute Engine Tasks': {
      Pending: 0,
      'In Progress': 0
    },
    'Compute Engine JVM State': {
      'Max Memory (MB)': 1024,
      'Free Memory (MB)': 89
    },
    'Compute Engine Database Connection': {
      'Pool Initial Size': 0,
      'Pool Active Connections': 0
    },
    'Compute Engine Logging': {
      'Logs Level': 'DEBUG',
      'Logs Dir': '/logs'
    },
    'Compute Engine JVM Properties': {
      'file.encoding': 'UTF-8',
      'file.separator': '/'
    },
    ...overrides
  };
}

export function mockLongLivingBranch(
  overrides: Partial<T.LongLivingBranch> = {}
): T.LongLivingBranch {
  return {
    analysisDate: '2018-01-01',
    isMain: false,
    name: 'branch-6.7',
    type: 'LONG',
    ...overrides
  };
}

export function mockStore(state: any = {}, reducer = (state: any) => state): Store {
  return createStore(reducer, state);
}

export function mockUser(overrides: Partial<T.User> = {}): T.User {
  return {
    active: true,
    local: true,
    login: 'john.doe',
    name: 'John Doe',
    ...overrides
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

export function mockDocumentationEntry(
  overrides: Partial<DocumentationEntry> = {}
): DocumentationEntry {
  return {
    content: 'Lorem ipsum dolor sit amet fredum',
    relativeName: 'Lorem',
    navTitle: undefined,
    text: 'Lorem ipsum dolor sit amet fredum',
    title: 'Lorem',
    url: '/lorem/ipsum',
    ...overrides
  };
}

export function mockMainBranch(overrides: Partial<T.MainBranch> = {}): T.MainBranch {
  return {
    analysisDate: '2018-01-01',
    isMain: true,
    name: 'master',
    ...overrides
  };
}

export function mockLanguage(overrides: Partial<T.Language> = {}): T.Language {
  return {
    key: 'css',
    name: 'CSS',
    ...overrides
  };
}

export function mockFlowLocation(overrides: Partial<T.FlowLocation> = {}): T.FlowLocation {
  return {
    component: 'main.js',
    textRange: {
      startLine: 1,
      startOffset: 1,
      endLine: 2,
      endOffset: 2
    },
    ...overrides
  };
}
