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
import getPages from '../../pages';
import { isSonarCloud } from '../../../../helpers/system';

// mock `remark` and `remark-react` to work around the issue with cjs imports
jest.mock('remark', () => {
  const remark = require.requireActual('remark');
  return { default: remark };
});

jest.mock('unist-util-visit', () => {
  const exp = require.requireActual('unist-util-visit');
  return { default: exp };
});

jest.mock('../../documentation.directory-loader', () => [
  {
    path: 'all',
    content: `
    ---
    title: All
    ---

    all all all`
  },
  {
    path: 'sonarqube-foo',
    content: `
    ---
    title: Foo
    scope: sonarqube
    ---

    foo foo foo`
  },
  {
    path: 'sonarcloud-bar',
    content: `
    ---
    title: Bar
    scope: sonarcloud
    ---

    bar bar bar`
  },
  {
    path: 'static-baz',
    content: `
    ---
    title: Baz
    scope: static
    ---

    baz baz baz`
  }
]);

jest.mock('../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

it('should filter pages for SonarQube', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(false);
  expect(getPages().map(page => page.title)).toEqual(['All', 'Foo']);
});

it('should filter pages for SonarCloud', () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  expect(getPages().map(page => page.title)).toEqual(['All', 'Bar']);
});
