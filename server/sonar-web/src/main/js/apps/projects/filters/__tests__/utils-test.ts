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
import { getFilterUrl } from '../utils';

it('works in trivial cases', () => {
  expect(getFilterUrl({ query: {} }, {})).toEqual({ pathname: '/projects', query: {} });
  expect(getFilterUrl({ query: { foo: 'bar' } }, {})).toEqual({
    pathname: '/projects',
    query: { foo: 'bar' }
  });
  expect(getFilterUrl({ query: {} }, { foo: 'bar' })).toEqual({
    pathname: '/projects',
    query: { foo: 'bar' }
  });
  expect(getFilterUrl({ query: { foo: 'bar' } }, { foo: 'qux' })).toEqual({
    pathname: '/projects',
    query: { foo: 'qux' }
  });
  expect(getFilterUrl({ query: { foo: 'bar' } }, { baz: 'qux' })).toEqual({
    pathname: '/projects',
    query: { foo: 'bar', baz: 'qux' }
  });
});

it('works for favorites', () => {
  expect(getFilterUrl({ isFavorite: true, query: {} }, { foo: 'bar' })).toEqual({
    pathname: '/projects/favorite',
    query: { foo: 'bar' }
  });
});

it('works with organization', () => {
  expect(getFilterUrl({ organization: { key: 'org' }, query: {} }, { foo: 'bar' })).toEqual({
    pathname: '/organizations/org/projects',
    query: { foo: 'bar' }
  });
});
