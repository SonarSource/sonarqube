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
import {
  getExtensionFromCache,
  getWebAnalyticsPageHandlerFromCache,
  getExtensionStart,
  installExtensionsHandler,
  installScript,
  installWebAnalyticsHandler
} from '../extensions';
import exposeLibraries from '../../app/components/extensions/exposeLibraries';

jest.mock('../../app/components/extensions/exposeLibraries', () => ({
  default: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe('installExtensionsHandler & extensions.getExtensionFromCache', () => {
  it('should register the global "registerExtension" function and retrieve extension', () => {
    expect((window as any).registerExtension).toBeUndefined();
    installExtensionsHandler();
    expect((window as any).registerExtension).toEqual(expect.any(Function));

    const start = jest.fn();
    (window as any).registerExtension('foo', start);
    expect(getExtensionFromCache('foo')).toBe(start);
  });
});

describe('setWebAnalyticsPageChangeHandler & getWebAnalyticsPageHandlerFromCache', () => {
  it('should register the global "setWebAnalyticsPageChangeHandler" function and retrieve analytics extension', () => {
    expect((window as any).setWebAnalyticsPageChangeHandler).toBeUndefined();
    installWebAnalyticsHandler();
    expect((window as any).setWebAnalyticsPageChangeHandler).toEqual(expect.any(Function));

    const pageChange = jest.fn();
    (window as any).setWebAnalyticsPageChangeHandler(pageChange);
    expect(getWebAnalyticsPageHandlerFromCache()).toBe(pageChange);
  });
});

describe('installScript', () => {
  it('should add the given script in the dom', () => {
    installScript('custom_script.js');
    expect(document.body.innerHTML).toMatchSnapshot();
  });
});

describe('getExtensionStart', () => {
  it('should install the extension in the to dom', () => {
    const start = jest.fn();
    const scriptTag = document.createElement('script');
    document.createElement = jest.fn().mockReturnValue(scriptTag);
    installExtensionsHandler();

    const result = getExtensionStart('bar');
    (window as any).registerExtension('bar', start);
    (scriptTag.onload as Function)();

    expect(exposeLibraries).toBeCalled();
    return expect(result).resolves.toBe(start);
  });

  it('should get the extension from the cache', () => {
    const start = jest.fn();
    installExtensionsHandler();
    (window as any).registerExtension('baz', start);
    return expect(getExtensionStart('baz')).resolves.toBe(start);
  });
});
