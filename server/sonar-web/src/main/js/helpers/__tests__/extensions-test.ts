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
import exposeLibraries from '../../app/components/extensions/exposeLibraries';
import { getExtensionStart, installScript, installStyles } from '../extensions';
import { installExtensionsHandler } from '../extensionsHandler';

jest.mock('../../app/components/extensions/exposeLibraries', () => jest.fn());

beforeEach(() => {
  jest.clearAllMocks();
  document.body.childNodes.forEach((node) => document.body.removeChild(node));
  document.head.childNodes.forEach((node) => document.head.removeChild(node));
});

describe('installScript', () => {
  it('should add the given script to the dom', () => {
    installScript('custom_script.js');
    expect(document.body.innerHTML).toMatchSnapshot();
  });
});

describe('installStyles', () => {
  it('should add the given stylesheet to the dom', async () => {
    installStyles('custom_styles.css');
    await new Promise(setImmediate);
    expect(document.head.innerHTML).toMatchSnapshot();
  });
});

describe('getExtensionStart', () => {
  const originalCreateElement = document.createElement;
  const scriptTag = document.createElement('script');
  const linkTag = document.createElement('link');

  beforeEach(() => {
    Object.defineProperty(document, 'createElement', {
      writable: true,
      value: jest.fn().mockReturnValueOnce(scriptTag).mockReturnValueOnce(linkTag),
    });
  });

  afterEach(() => {
    Object.defineProperty(document, 'createElement', {
      writable: true,
      value: originalCreateElement,
    });
  });

  it('should install the extension in the to dom', async () => {
    const start = jest.fn();
    installExtensionsHandler();

    const result = getExtensionStart('bar');

    await new Promise(setImmediate);
    expect(exposeLibraries).toHaveBeenCalled();

    (window as any).registerExtension('bar', start, true);

    (scriptTag.onload as Function)();
    await new Promise(setImmediate);

    (linkTag.onload as Function)();
    await new Promise(setImmediate);

    return expect(result).resolves.toBe(start);
  });

  it('should get the extension from the cache', () => {
    const start = jest.fn();
    installExtensionsHandler();
    (window as any).registerExtension('baz', start);
    return expect(getExtensionStart('baz')).resolves.toBe(start);
  });
});
