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
import { searchParamsToQuery } from '~sonar-aligned/helpers/router';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { AlmKeys } from '../../types/alm-settings';
import { IssueType } from '../../types/issues';
import { MeasurePageView } from '../../types/measures';
import { mockBranch, mockMainBranch, mockPullRequest } from '../mocks/branch-like';
import { mockLocation } from '../testMocks';
import {
  CodeScope,
  convertGithubApiUrlToLink,
  convertToTo,
  getComponentAdminUrl,
  getComponentDrilldownUrl,
  getComponentDrilldownUrlWithSelection,
  getComponentOverviewUrl,
  getCreateProjectModeLocation,
  getDeprecatedActiveRulesUrl,
  getGlobalSettingsUrl,
  getIssuesUrl,
  getPathUrlAsString,
  getProjectSettingsUrl,
  getQualityGateUrl,
  getQualityGatesUrl,
  getReturnUrl,
  isRelativeUrl,
  stripTrailingSlash,
} from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const METRIC = 'coverage';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);

describe('#convertGithubApiUrlToLink', () => {
  it('should correctly convert a GitHub API URL to a Web URL', () => {
    expect(convertGithubApiUrlToLink('https://api.github.com')).toBe('https://github.com');
    expect(convertGithubApiUrlToLink('https://company.github.com/api/v3')).toBe(
      'https://company.github.com',
    );
  });
});

describe('#stripTrailingSlash', () => {
  it('should correctly strip trailing slashes from any URL', () => {
    expect(stripTrailingSlash('https://example.com/')).toBe('https://example.com');
    expect(convertGithubApiUrlToLink('https://example.com')).toBe('https://example.com');
  });
});

describe('getComponentAdminUrl', () => {
  it.each([
    [
      'Portfolio',
      ComponentQualifier.Portfolio,
      { pathname: '/project/admin/extension/governance/console', search: '?id=key&qualifier=VW' },
    ],
    [
      'Application',
      ComponentQualifier.Application,
      {
        pathname: '/project/admin/extension/developer-server/application-console',
        search: '?id=key',
      },
    ],
    ['Project', ComponentQualifier.Project, { pathname: '/dashboard', search: '?id=key' }],
  ])('should work for %s', (_qualifierName, qualifier, result) => {
    expect(getComponentAdminUrl('key', qualifier)).toEqual(result);
  });
});

describe('#getComponentOverviewUrl', () => {
  it('should return a portfolio url for a portfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Portfolio)).toEqual(
      expect.objectContaining({
        pathname: '/portfolio',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });
  it('should return a portfolio url for a subportfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.SubPortfolio)).toEqual(
      expect.objectContaining({
        pathname: '/portfolio',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });
  it('should return a dashboard url for a project', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Project)).toEqual(
      expect.objectContaining({
        pathname: '/dashboard',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });
  it('should return correct dashboard url for a project when navigating from new code', () => {
    expect(
      getComponentOverviewUrl(
        SIMPLE_COMPONENT_KEY,
        ComponentQualifier.Project,
        undefined,
        CodeScope.New,
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/dashboard',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY, code_scope: 'new' }),
      }),
    );
  });
  it('should return correct dashboard url for a project when navigating from overall code', () => {
    expect(
      getComponentOverviewUrl(
        SIMPLE_COMPONENT_KEY,
        ComponentQualifier.Project,
        undefined,
        CodeScope.Overall,
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/dashboard',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY, code_scope: 'overall' }),
      }),
    );
  });
  it('should return a dashboard url for an app', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Application)).toEqual(
      expect.objectContaining({
        pathname: '/dashboard',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY }),
      }),
    );
  });
});

describe('#getComponentDrilldownUrl', () => {
  it('should return component drilldown url', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: SIMPLE_COMPONENT_KEY, metric: METRIC }),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY, metric: METRIC }),
      }),
    );
  });

  it('should not encode component key', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: COMPLEX_COMPONENT_KEY, metric: METRIC }),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({ id: COMPLEX_COMPONENT_KEY, metric: METRIC }),
      }),
    );
  });

  it('should add asc param only when its list view', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: SIMPLE_COMPONENT_KEY, metric: METRIC, asc: false }),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY, metric: METRIC }),
      }),
    );

    expect(
      getComponentDrilldownUrl({
        componentKey: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        listView: true,
        asc: false,
      }),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          view: 'list',
          asc: 'false',
        }),
      }),
    );
  });
});

describe('#getComponentDrilldownUrlWithSelection', () => {
  it('should return component drilldown url with selection', () => {
    expect(
      getComponentDrilldownUrlWithSelection(SIMPLE_COMPONENT_KEY, COMPLEX_COMPONENT_KEY, METRIC),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          selected: COMPLEX_COMPONENT_KEY,
        }),
      }),
    );
  });

  it('should return component drilldown url with branchLike', () => {
    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockBranch({ name: 'foo' }),
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          branch: 'foo',
          selected: COMPLEX_COMPONENT_KEY,
        }),
      }),
    );
  });

  it('should return component drilldown url with view parameter', () => {
    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        undefined,
        MeasurePageView.list,
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          view: MeasurePageView.list,
          selected: COMPLEX_COMPONENT_KEY,
        }),
      }),
    );

    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockMainBranch(),
        MeasurePageView.treemap,
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          view: MeasurePageView.treemap,
          selected: COMPLEX_COMPONENT_KEY,
        }),
      }),
    );

    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockPullRequest({ key: '1' }),
        MeasurePageView.tree,
      ),
    ).toEqual(
      expect.objectContaining({
        pathname: '/component_measures',
        search: queryToSearchString({
          id: SIMPLE_COMPONENT_KEY,
          metric: METRIC,
          pullRequest: '1',
          selected: COMPLEX_COMPONENT_KEY,
        }),
      }),
    );
  });
});

describe('getDeprecatedActiveRulesUrl', () => {
  it('should include query params', () => {
    expect(getDeprecatedActiveRulesUrl({ languages: 'js' })).toEqual({
      pathname: '/coding_rules',
      search: '?languages=js&activation=true&statuses=DEPRECATED',
    });
  });
  it('should handle empty query', () => {
    expect(getDeprecatedActiveRulesUrl()).toEqual({
      pathname: '/coding_rules',
      search: '?activation=true&statuses=DEPRECATED',
    });
  });
});

describe('#getQualityGate(s)Url', () => {
  it('should work as expected', () => {
    expect(getQualityGatesUrl()).toEqual({ pathname: '/quality_gates' });
    expect(getQualityGateUrl('bar')).toEqual({ pathname: '/quality_gates/show/bar' });
  });
});

describe('#getIssuesUrl', () => {
  it('should work as expected', () => {
    const type = IssueType.Bug;
    expect(getIssuesUrl({ type })).toEqual({
      pathname: '/issues',
      search: queryToSearchString({ type }),
    });
  });
});

describe('#getGlobalSettingsUrl', () => {
  it('should work as expected', () => {
    expect(getGlobalSettingsUrl('foo')).toEqual({
      pathname: '/admin/settings',
      search: queryToSearchString({ category: 'foo' }),
    });
    expect(getGlobalSettingsUrl('foo', { alm: AlmKeys.GitHub })).toEqual({
      pathname: '/admin/settings',
      search: queryToSearchString({ category: 'foo', alm: AlmKeys.GitHub }),
    });
  });
});

describe('#getProjectSettingsUrl', () => {
  it('should work as expected', () => {
    expect(getProjectSettingsUrl('foo')).toEqual({
      pathname: '/project/settings',
      search: queryToSearchString({ id: 'foo' }),
    });
    expect(getProjectSettingsUrl('foo', 'bar')).toEqual({
      pathname: '/project/settings',
      search: queryToSearchString({ id: 'foo', category: 'bar' }),
    });
  });
});

describe('#getPathUrlAsString', () => {
  it('should return component url', () => {
    expect(
      getPathUrlAsString({
        pathname: '/dashboard',
        search: queryToSearchString({ id: SIMPLE_COMPONENT_KEY }),
      }),
    ).toBe('/dashboard?id=' + SIMPLE_COMPONENT_KEY);
  });

  it('should encode component key', () => {
    expect(
      getPathUrlAsString({
        pathname: '/dashboard',
        search: queryToSearchString({ id: COMPLEX_COMPONENT_KEY }),
      }),
    ).toBe('/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
  });

  it('should handle partial arguments', () => {
    expect(getPathUrlAsString({}, true)).toBe('/');
  });
});

describe('#getReturnUrl', () => {
  it('should get the return url', () => {
    expect(getReturnUrl({ query: { return_to: '/test' } })).toBe('/test');
    expect(getReturnUrl({ query: { return_to: 'http://www.google.com' } })).toBe('/');
    expect(getReturnUrl({})).toBe('/');
  });
});

describe('#isRelativeUrl', () => {
  it('should check a relative url', () => {
    expect(isRelativeUrl('/test')).toBe(true);
    expect(isRelativeUrl('http://www.google.com')).toBe(false);
    expect(isRelativeUrl('javascript:alert("test")')).toBe(false);
    expect(isRelativeUrl('\\test')).toBe(false);
    expect(isRelativeUrl('//test')).toBe(false);
  });
});

describe('#getHostUrl', () => {
  beforeEach(() => {
    jest.resetModules();
  });
  it('should return host url on client side', () => {
    jest.mock('../system', () => ({
      getBaseUrl: () => '',
    }));
    const mockedUrls = require('../urls');
    expect(mockedUrls.getHostUrl()).toBe('http://localhost');
  });
});

describe('searchParamsToQuery', () => {
  it('should handle arrays and single params', () => {
    const searchParams = new URLSearchParams([
      ['a', 'v1'],
      ['a', 'v2'],
      ['b', 'awesome'],
      ['a', 'v3'],
    ]);

    const result = searchParamsToQuery(searchParams);

    expect(result).toEqual({ a: ['v1', 'v2', 'v3'], b: 'awesome' });
  });
});

describe('convertToTo', () => {
  it('should handle locations with a query', () => {
    expect(convertToTo(mockLocation({ pathname: '/account', query: { id: 1 } }))).toEqual({
      pathname: '/account',
      search: '?id=1',
    });
  });

  it('should forward strings', () => {
    expect(convertToTo('/whatever')).toBe('/whatever');
  });
});

describe('#get import devops config URL', () => {
  it('should work as expected', () => {
    expect(getCreateProjectModeLocation(AlmKeys.GitHub)).toEqual({
      search: '?mode=github',
    });
  });
});
