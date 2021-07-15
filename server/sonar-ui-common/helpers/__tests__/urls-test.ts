/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import Initializer from '../init';
import { getPathUrlAsString, getReturnUrl, isRelativeUrl } from '../urls';

const SIMPLE_COMPONENT_KEY = 'sonarqube';
const COMPLEX_COMPONENT_KEY = 'org.sonarsource.sonarqube:sonarqube';
const COMPLEX_COMPONENT_KEY_ENCODED = encodeURIComponent(COMPLEX_COMPONENT_KEY);

afterEach(() => {
  Initializer.setUrlContext('');
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

  it('should take baseUrl into account', () => {
    Initializer.setUrlContext('/context');
    expect(
      getPathUrlAsString({ pathname: '/dashboard', query: { id: COMPLEX_COMPONENT_KEY } })
    ).toBe('/context/dashboard?id=' + COMPLEX_COMPONENT_KEY_ENCODED);
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
    jest.mock('../init', () => ({
      getUrlContext: () => '',
      IS_SSR: false,
    }));
    const mockedUrls = require('../urls');
    expect(mockedUrls.getHostUrl()).toBe('http://localhost');
  });
  it('should throw on server-side', () => {
    jest.mock('../init', () => ({
      getUrlContext: () => '',
      IS_SSR: true,
    }));
    const mockedUrls = require('../urls');
    expect(mockedUrls.getHostUrl).toThrowErrorMatchingInlineSnapshot(
      `"No host url available on server side."`
    );
  });
});
