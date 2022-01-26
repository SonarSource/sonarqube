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
import { AlmKeys } from '../../types/alm-settings';
import { ComponentQualifier } from '../../types/component';
import { IssueType } from '../../types/issues';
import { mockBranch, mockMainBranch, mockPullRequest } from '../mocks/branch-like';
import {
  convertGithubApiUrlToLink,
  getComponentDrilldownUrl,
  getComponentDrilldownUrlWithSelection,
  getComponentIssuesUrl,
  getComponentOverviewUrl,
  getComponentSecurityHotspotsUrl,
  getGlobalSettingsUrl,
  getIssuesUrl,
  getPathUrlAsString,
  getProjectSettingsUrl,
  getQualityGatesUrl,
  getQualityGateUrl,
  getReturnUrl,
  isRelativeUrl,
  stripTrailingSlash
} from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const METRIC = 'coverage';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);

describe('#convertGithubApiUrlToLink', () => {
  it('should correctly convert a GitHub API URL to a Web URL', () => {
    expect(convertGithubApiUrlToLink('https://api.github.com')).toBe('https://github.com');
    expect(convertGithubApiUrlToLink('https://company.github.com/api/v3')).toBe(
      'https://company.github.com'
    );
  });
});

describe('#stripTrailingSlash', () => {
  it('should correctly strip trailing slashes from any URL', () => {
    expect(stripTrailingSlash('https://example.com/')).toBe('https://example.com');
    expect(convertGithubApiUrlToLink('https://example.com')).toBe('https://example.com');
  });
});

describe('#getComponentIssuesUrl', () => {
  it('should work without parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, {})).toEqual({
      pathname: '/project/issues',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });

  it('should work with parameters', () => {
    expect(getComponentIssuesUrl(SIMPLE_COMPONENT_KEY, { resolved: 'false' })).toEqual({
      pathname: '/project/issues',
      query: { id: SIMPLE_COMPONENT_KEY, resolved: 'false' }
    });
  });
});

describe('#getComponentSecurityHotspotsUrl', () => {
  it('should work with no extra parameters', () => {
    expect(getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY, {})).toEqual({
      pathname: '/security_hotspots',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });

  it('should forward some query parameters', () => {
    expect(
      getComponentSecurityHotspotsUrl(SIMPLE_COMPONENT_KEY, {
        sinceLeakPeriod: 'true',
        ignoredParam: '1234'
      })
    ).toEqual({
      pathname: '/security_hotspots',
      query: { id: SIMPLE_COMPONENT_KEY, sinceLeakPeriod: 'true' }
    });
  });
});

describe('#getComponentOverviewUrl', () => {
  it('should return a portfolio url for a portfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Portfolio)).toEqual({
      pathname: '/portfolio',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a portfolio url for a subportfolio', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.SubPortfolio)).toEqual({
      pathname: '/portfolio',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a dashboard url for a project', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Project)).toEqual({
      pathname: '/dashboard',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
  it('should return a dashboard url for an app', () => {
    expect(getComponentOverviewUrl(SIMPLE_COMPONENT_KEY, ComponentQualifier.Application)).toEqual({
      pathname: '/dashboard',
      query: { id: SIMPLE_COMPONENT_KEY }
    });
  });
});

describe('#getComponentDrilldownUrl', () => {
  it('should return component drilldown url', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: SIMPLE_COMPONENT_KEY, metric: METRIC })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC }
    });
  });

  it('should not encode component key', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: COMPLEX_COMPONENT_KEY, metric: METRIC })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: COMPLEX_COMPONENT_KEY, metric: METRIC }
    });
  });

  it('should add asc param only when its list view', () => {
    expect(
      getComponentDrilldownUrl({ componentKey: SIMPLE_COMPONENT_KEY, metric: METRIC, asc: false })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC }
    });

    expect(
      getComponentDrilldownUrl({
        componentKey: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        listView: true,
        asc: false
      })
    ).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC, asc: 'false', view: 'list' }
    });
  });
});

describe('#getComponentDrilldownUrlWithSelection', () => {
  it('should return component drilldown url with selection', () => {
    expect(
      getComponentDrilldownUrlWithSelection(SIMPLE_COMPONENT_KEY, COMPLEX_COMPONENT_KEY, METRIC)
    ).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC, selected: COMPLEX_COMPONENT_KEY }
    });
  });

  it('should return component drilldown url with branchLike', () => {
    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockBranch({ name: 'foo' })
      )
    ).toEqual({
      pathname: '/component_measures',
      query: {
        id: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        selected: COMPLEX_COMPONENT_KEY,
        branch: 'foo'
      }
    });
  });

  it('should return component drilldown url with view parameter', () => {
    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        undefined,
        'list'
      )
    ).toEqual({
      pathname: '/component_measures',
      query: {
        id: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        selected: COMPLEX_COMPONENT_KEY,
        view: 'list'
      }
    });

    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockMainBranch(),
        'treemap'
      )
    ).toEqual({
      pathname: '/component_measures',
      query: {
        id: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        selected: COMPLEX_COMPONENT_KEY,
        view: 'treemap'
      }
    });

    expect(
      getComponentDrilldownUrlWithSelection(
        SIMPLE_COMPONENT_KEY,
        COMPLEX_COMPONENT_KEY,
        METRIC,
        mockPullRequest({ key: '1' }),
        'tree'
      )
    ).toEqual({
      pathname: '/component_measures',
      query: {
        id: SIMPLE_COMPONENT_KEY,
        metric: METRIC,
        selected: COMPLEX_COMPONENT_KEY,
        pullRequest: '1'
      }
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
      query: { type }
    });
  });
});

describe('#getGlobalSettingsUrl', () => {
  it('should work as expected', () => {
    expect(getGlobalSettingsUrl('foo')).toEqual({
      pathname: '/admin/settings',
      query: { category: 'foo' }
    });
    expect(getGlobalSettingsUrl('foo', { alm: AlmKeys.GitHub })).toEqual({
      pathname: '/admin/settings',
      query: { category: 'foo', alm: AlmKeys.GitHub }
    });
  });
});

describe('#getProjectSettingsUrl', () => {
  it('should work as expected', () => {
    expect(getProjectSettingsUrl('foo')).toEqual({
      pathname: '/project/settings',
      query: { id: 'foo' }
    });
    expect(getProjectSettingsUrl('foo', 'bar')).toEqual({
      pathname: '/project/settings',
      query: { id: 'foo', category: 'bar' }
    });
  });
});

describe('#getPathUrlAsString', () => {
  it('should return component url', () => {
    expect(
      getPathUrlAsString({ pathname: '/dashboard', query: { id: SIMPLE_COMPONENT_KEY } })
    ).toBe('/dashboard?id=' + SIMPLE_COMPONENT_KEY);
  });

  it('should encode component key', () => {
    expect(
      getPathUrlAsString({ pathname: '/dashboard', query: { id: COMPLEX_COMPONENT_KEY } })
    ).toBe('/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
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
      getBaseUrl: () => ''
    }));
    jest.mock('../browser', () => ({
      IS_SSR: false
    }));
    const mockedUrls = require('../urls');
    expect(mockedUrls.getHostUrl()).toBe('http://localhost');
  });
  it('should throw on server-side', () => {
    jest.mock('../system', () => ({
      getBaseUrl: () => ''
    }));
    jest.mock('../browser', () => ({
      IS_SSR: true
    }));
    const mockedUrls = require('../urls');
    expect(mockedUrls.getHostUrl).toThrowErrorMatchingInlineSnapshot(
      `"No host url available on server side."`
    );
  });
});
