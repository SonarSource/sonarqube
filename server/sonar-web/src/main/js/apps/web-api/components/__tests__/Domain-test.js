/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import Domain from '../Domain';

it('should render deprecated actions', () => {
  const actions = [{ key: 'foo', deprecatedSince: '5.0' }];
  const domain = { actions, path: 'api' };
  expect(
    shallow(<Domain domain={domain} searchQuery="" showDeprecated={true} />)
  ).toMatchSnapshot();
});

it('should not render deprecated actions', () => {
  const actions = [{ key: 'foo', deprecatedSince: '5.0' }];
  const domain = { actions, path: 'api' };
  expect(
    shallow(<Domain domain={domain} searchQuery="" showDeprecated={false} />)
  ).toMatchSnapshot();
});

it('should render internal actions', () => {
  const actions = [{ key: 'foo', internal: true }];
  const domain = { actions, path: 'api' };
  expect(shallow(<Domain domain={domain} searchQuery="" showInternal={true} />)).toMatchSnapshot();
});

it('should not render internal actions', () => {
  const actions = [{ key: 'foo', internal: true }];
  const domain = { actions, path: 'api' };
  expect(shallow(<Domain domain={domain} searchQuery="" showInternal={false} />)).toMatchSnapshot();
});

it('should render only actions matching the query', () => {
  const actions = [{ key: 'foo' }, { key: 'bar' }];
  const domain = { actions, path: 'api' };
  expect(shallow(<Domain domain={domain} searchQuery="Foo" />)).toMatchSnapshot();
});

it('should also render actions with a description matching the query', () => {
  const actions = [{ key: 'foo', description: 'foobar' }, { key: 'bar' }, { key: 'baz' }];
  const domain = { actions, path: 'api' };
  expect(
    shallow(<Domain domain={domain} searchQuery="bar" showDeprecated={false} />)
  ).toMatchSnapshot();
});
