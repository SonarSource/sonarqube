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
import {
  getComponentIssuesUrl,
  getComponentDrilldownUrl,
  getPathUrlAsString,
  getProjectUrl,
  getQualityGatesUrl,
  getQualityGateUrl,
  isUrl
} from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);
const METRIC = 'coverage';

let oldBaseUrl: string;

beforeEach(() => {
  oldBaseUrl = (window as any).baseUrl;
});

afterEach(() => {
  (window as any).baseUrl = oldBaseUrl;
});

describe('#getPathUrlAsString', () => {
  it('should return component url', () => {
    expect(getPathUrlAsString(getProjectUrl(SIMPLE_COMPONENT_KEY, 'branch:7.0'))).toBe(
      '/dashboard?id=' + SIMPLE_COMPONENT_KEY + '&branch=branch%3A7.0'
    );
  });

  it('should encode component key', () => {
    expect(getPathUrlAsString(getProjectUrl(COMPLEX_COMPONENT_KEY))).toBe(
      '/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED
    );
  });

  it('should take baseUrl into account', () => {
    (window as any).baseUrl = '/context';
    expect(getPathUrlAsString(getProjectUrl(COMPLEX_COMPONENT_KEY))).toBe(
      '/context/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED
    );
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

describe('#getComponentDrilldownUrl', () => {
  it('should return component drilldown url', () => {
    expect(getComponentDrilldownUrl(SIMPLE_COMPONENT_KEY, METRIC)).toEqual({
      pathname: '/component_measures',
      query: { id: SIMPLE_COMPONENT_KEY, metric: METRIC }
    });
  });

  it('should not encode component key', () => {
    expect(getComponentDrilldownUrl(COMPLEX_COMPONENT_KEY, METRIC)).toEqual({
      pathname: '/component_measures',
      query: { id: COMPLEX_COMPONENT_KEY, metric: METRIC }
    });
  });
});

describe('#getQualityGate(s)Url', () => {
  it('should take organization key into account', () => {
    expect(getQualityGatesUrl()).toEqual({ pathname: '/quality_gates' });
    expect(getQualityGatesUrl('foo')).toEqual({ pathname: '/organizations/foo/quality_gates' });
    expect(getQualityGateUrl('bar')).toEqual({ pathname: '/quality_gates/show/bar' });
    expect(getQualityGateUrl('bar', 'foo')).toEqual({
      pathname: '/organizations/foo/quality_gates/show/bar'
    });
  });

  it('should encode keys', () => {
    expect(getQualityGatesUrl(COMPLEX_COMPONENT_KEY)).toEqual({
      pathname: '/organizations/' + COMPLEX_COMPONENT_KEY_ENCODED + '/quality_gates'
    });
    expect(getQualityGateUrl(COMPLEX_COMPONENT_KEY)).toEqual({
      pathname: '/quality_gates/show/' + COMPLEX_COMPONENT_KEY_ENCODED
    });
  });
});

describe('#isUrl', () => {
  it('should be valid', () => {
    expect(isUrl('https://localhost')).toBeTruthy();
    expect(isUrl('https://localhost/')).toBeTruthy();
    expect(isUrl('https://localhost:9000')).toBeTruthy();
    expect(isUrl('https://localhost:9000/')).toBeTruthy();
    expect(isUrl('https://foo:bar@localhost:9000')).toBeTruthy();
    expect(isUrl('https://foo@localhost')).toBeTruthy();
    expect(isUrl('http://foo.com/blah_blah')).toBeTruthy();
    expect(isUrl('http://foo.com/blah_blah/')).toBeTruthy();
    expect(isUrl('http://www.example.com/wpstyle/?p=364')).toBeTruthy();
    expect(isUrl('https://www.example.com/foo/?bar=baz&inga=42&quux')).toBeTruthy();
    expect(isUrl('http://userid@example.com')).toBeTruthy();
    expect(isUrl('http://userid@example.com/')).toBeTruthy();
    expect(isUrl('http://userid:password@example.com:8080')).toBeTruthy();
    expect(isUrl('http://userid:password@example.com:8080/')).toBeTruthy();
    expect(isUrl('http://userid@example.com:8080')).toBeTruthy();
    expect(isUrl('http://userid@example.com:8080/')).toBeTruthy();
    expect(isUrl('http://userid:password@example.com')).toBeTruthy();
    expect(isUrl('http://userid:password@example.com/')).toBeTruthy();
    expect(isUrl('http://142.42.1.1/')).toBeTruthy();
    expect(isUrl('http://142.42.1.1:8080/')).toBeTruthy();
    expect(isUrl('http://foo.com/blah_(wikipedia)#cite-1')).toBeTruthy();
    expect(isUrl('http://foo.com/blah_(wikipedia)_blah#cite-1')).toBeTruthy();
    expect(isUrl('http://foo.com/(something)?after=parens')).toBeTruthy();
    expect(isUrl('http://code.google.com/events/#&product=browser')).toBeTruthy();
    expect(isUrl('http://j.mp')).toBeTruthy();
    expect(isUrl('http://foo.bar/?q=Test%20URL-encoded%20stuff')).toBeTruthy();
    expect(isUrl('http://1337.net')).toBeTruthy();
    expect(isUrl('http://a.b-c.de')).toBeTruthy();
    expect(isUrl('http://223.255.255.254')).toBeTruthy();
    expect(isUrl('https://foo_bar.example.com/')).toBeTruthy();
  });

  it('should not be valid', () => {
    expect(isUrl('http://')).toBeFalsy();
    expect(isUrl('http://?')).toBeFalsy();
    expect(isUrl('http://??')).toBeFalsy();
    expect(isUrl('http://??/')).toBeFalsy();
    expect(isUrl('http://#')).toBeFalsy();
    expect(isUrl('http://##')).toBeFalsy();
    expect(isUrl('http://##/')).toBeFalsy();
    expect(isUrl('//')).toBeFalsy();
    expect(isUrl('//a')).toBeFalsy();
    expect(isUrl('///a')).toBeFalsy();
    expect(isUrl('///')).toBeFalsy();
    expect(isUrl('foo.com')).toBeFalsy();
    expect(isUrl('http:// shouldfail.com')).toBeFalsy();
    expect(isUrl(':// should fail')).toBeFalsy();
  });
});
