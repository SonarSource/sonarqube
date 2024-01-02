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
import { getLinkName, isProvided, orderLinks } from '../projectLinks';

it('#isProvided', () => {
  expect(isProvided({ type: 'homepage' })).toBe(true);
  expect(isProvided({ type: 'custom' })).toBe(false);
});

it('#orderLinks', () => {
  const homepage = { type: 'homepage' };
  const issues = { type: 'issue' };
  const foo = { name: 'foo', type: 'foo' };
  const bar = { name: 'bar', type: 'bar' };
  expect(orderLinks([foo, homepage, issues, bar])).toEqual([homepage, issues, bar, foo]);
  expect(orderLinks([foo, bar])).toEqual([bar, foo]);
  expect(orderLinks([issues, homepage])).toEqual([homepage, issues]);
});

it('#getLinkName', () => {
  expect(getLinkName({ type: 'homepage' })).toBe('project_links.homepage');
  expect(getLinkName({ name: 'foo', type: 'custom' })).toBe('foo');
});
