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
  installExtensionsHandler,
  installWebAnalyticsHandler
} from '../extensionsHandler';

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
