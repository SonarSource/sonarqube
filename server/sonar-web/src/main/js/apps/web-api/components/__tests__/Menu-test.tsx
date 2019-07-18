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
import { shallow } from 'enzyme';
import * as React from 'react';
import Menu from '../Menu';

const ACTION: T.WebApi.Action = {
  key: 'foo',
  changelog: [],
  description: 'Foo Desc',
  hasResponseExample: false,
  internal: false,
  post: false
};
const DOMAIN1: T.WebApi.Domain = {
  actions: [ACTION],
  path: 'foo',
  description: 'API Foo'
};
const DOMAIN2: T.WebApi.Domain = {
  actions: [ACTION],
  path: 'bar',
  description: 'API Bar'
};
const PROPS = {
  domains: [DOMAIN1, DOMAIN2],
  query: { search: '', deprecated: false, internal: false },
  splat: ''
};

const SHOW_DEPRECATED = { search: '', deprecated: true, internal: false };
const SHOW_INTERNAL = { search: '', deprecated: false, internal: true };
const SEARCH_FOO = { search: 'Foo', deprecated: false, internal: false };
const SEARCH_BAR = { search: 'Bar', deprecated: false, internal: false };

it('should render deprecated domains', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN2,
    deprecatedSince: '5.0',
    actions: [{ ...ACTION, deprecatedSince: '5.0' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} query={SHOW_DEPRECATED} />)).toMatchSnapshot();
});

it('should not render deprecated domains', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN2,
    deprecatedSince: '5.0',
    actions: [{ ...ACTION, deprecatedSince: '5.0' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} />)).toMatchSnapshot();
});

it('should render internal domains', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN2,
    internal: true,
    actions: [{ ...ACTION, internal: true }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} query={SHOW_INTERNAL} />)).toMatchSnapshot();
});

it('should not render internal domains', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN2,
    internal: true,
    actions: [{ ...ACTION, internal: true }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} />)).toMatchSnapshot();
});

it('should render only domains with an action matching the query', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN2,
    actions: [{ ...ACTION, key: 'bar', description: 'Bar Desc' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} query={SEARCH_FOO} />)).toMatchSnapshot();
});

it('should also render domains with an actions description matching the query', () => {
  const domain: T.WebApi.Domain = {
    ...DOMAIN1,
    path: 'baz',
    description: 'API Baz',
    actions: [{ ...ACTION, key: 'baz', description: 'barbaz' }]
  };
  const domains = [DOMAIN1, DOMAIN2, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} query={SEARCH_BAR} />)).toMatchSnapshot();
});
