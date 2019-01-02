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
import * as React from 'react';
import { shallow } from 'enzyme';
import Domain from '../Domain';

const ACTION = {
  key: 'foo',
  changelog: [],
  description: 'Foo Desc',
  hasResponseExample: false,
  internal: false,
  post: false
};
const DOMAIN = {
  actions: [ACTION],
  path: 'api',
  description: 'API Desc',
  deprecated: false,
  internal: false
};
const DEFAULT_PROPS = {
  domain: DOMAIN,
  query: { search: '', deprecated: false, internal: false }
};
const SHOW_DEPRECATED = { search: '', deprecated: true, internal: false };
const SHOW_INTERNAL = { search: '', deprecated: false, internal: true };
const SEARCH_FOO = { search: 'Foo', deprecated: false, internal: false };

it('should render deprecated actions', () => {
  const action = { ...ACTION, deprecatedSince: '5.0' };
  const domain = { ...DOMAIN, actions: [action] };
  expect(
    shallow(<Domain {...DEFAULT_PROPS} domain={domain} query={SHOW_DEPRECATED} />)
  ).toMatchSnapshot();
});

it('should not render deprecated actions', () => {
  const action = { ...ACTION, deprecatedSince: '5.0' };
  const domain = { ...DOMAIN, actions: [action] };
  expect(
    shallow(<Domain {...DEFAULT_PROPS} domain={domain} query={SHOW_INTERNAL} />)
  ).toMatchSnapshot();
});

it('should render internal actions', () => {
  const action = { ...ACTION, internal: true };
  const domain = { ...DOMAIN, actions: [action] };
  expect(
    shallow(<Domain {...DEFAULT_PROPS} domain={domain} query={SHOW_INTERNAL} />)
  ).toMatchSnapshot();
});

it('should not render internal actions', () => {
  const action = { ...ACTION, internal: true };
  const domain = { ...DOMAIN, actions: [action] };
  expect(shallow(<Domain {...DEFAULT_PROPS} domain={domain} />)).toMatchSnapshot();
});

it('should render only actions matching the query', () => {
  const actions = [ACTION, { ...ACTION, key: 'bar', description: 'Bar desc' }];
  const domain = { ...DOMAIN, actions };
  expect(
    shallow(<Domain {...DEFAULT_PROPS} domain={domain} query={SEARCH_FOO} />)
  ).toMatchSnapshot();
});

it('should also render actions with a description matching the query', () => {
  const actions = [
    ACTION,
    { ...ACTION, key: 'bar', description: 'Bar desc' },
    { ...ACTION, key: 'baz', description: 'foobar' }
  ];
  const domain = { ...DOMAIN, actions };
  expect(
    shallow(<Domain {...DEFAULT_PROPS} domain={domain} query={SEARCH_FOO} />)
  ).toMatchSnapshot();
});
