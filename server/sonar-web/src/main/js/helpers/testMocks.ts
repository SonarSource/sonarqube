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
import { InjectedRouter } from 'react-router';
import { Location } from 'history';
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

export function mockCurrentUser(overrides: Partial<T.LoggedInUser> = {}): T.LoggedInUser {
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

  function loc(): T.FlowLocation {
    return {
      component: 'main.js',
      textRange: { startLine: 1, startOffset: 1, endLine: 2, endOffset: 2 }
    };
  }

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
    key: 'key',
    pathname: '/path',
    query: {},
    search: '',
    state: {},
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
    alm: { key: 'github', membersSync: false, url: 'https://github.com/foo', ...almOverrides },
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

export function mockQualityGateProjectStatus(
  overrides: Partial<T.QualityGateProjectStatus['projectStatus']> = {}
): T.QualityGateProjectStatus {
  return {
    projectStatus: {
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
    }
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

export function mockShortLivingBranch(
  overrides: Partial<T.ShortLivingBranch> = {}
): T.ShortLivingBranch {
  return {
    analysisDate: '2018-01-01',
    isMain: false,
    name: 'release-1.0',
    mergeBranch: 'master',
    type: 'SHORT',
    ...overrides
  };
}

export function mockLongLivingBranch(
  overrides: Partial<T.LongLivingBranch> = {}
): T.LongLivingBranch {
  return {
    analysisDate: '2018-01-01',
    isMain: false,
    name: 'master',
    type: 'LONG',
    ...overrides
  };
}
