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
import * as React from 'react';
import { shallow } from 'enzyme';
import Menu from '../Menu';

const ACTION = {
  key: 'foo',
  changelog: [],
  description: 'Foo Desc',
  hasResponseExample: false,
  internal: false,
  post: false
};
const DOMAIN1 = {
  actions: [ACTION],
  path: 'foo',
  description: 'API Foo',
  deprecated: false,
  internal: false
};
const DOMAIN2 = {
  actions: [ACTION],
  path: 'bar',
  description: 'API Bar',
  deprecated: false,
  internal: false
};
const PROPS = {
  domains: [DOMAIN1, DOMAIN2],
  showDeprecated: false,
  showInternal: false,
  searchQuery: '',
  splat: ''
};

it('should render deprecated domains', () => {
  const domain = {
    ...DOMAIN2,
    deprecatedSince: '5.0',
    actions: [{ ...ACTION, deprecatedSince: '5.0' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} showDeprecated={true} />)).toMatchSnapshot();
});

it('should not render deprecated domains', () => {
  const domain = {
    ...DOMAIN2,
    deprecatedSince: '5.0',
    actions: [{ ...ACTION, deprecatedSince: '5.0' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} showDeprecated={false} />)).toMatchSnapshot();
});

it('should render internal domains', () => {
  const domain = { ...DOMAIN2, internal: true, actions: [{ ...ACTION, internal: true }] };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} showInternal={true} />)).toMatchSnapshot();
});

it('should not render internal domains', () => {
  const domain = { ...DOMAIN2, internal: true, actions: [{ ...ACTION, internal: true }] };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} showInternal={false} />)).toMatchSnapshot();
});

it('should render only domains with an action matching the query', () => {
  const domain = {
    ...DOMAIN2,
    actions: [{ ...ACTION, key: 'bar', path: 'bar', description: 'Bar Desc' }]
  };
  const domains = [DOMAIN1, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} searchQuery="Foo" />)).toMatchSnapshot();
});

it('should also render domains with an actions description matching the query', () => {
  const domain = {
    ...DOMAIN1,
    path: 'baz',
    description: 'API Baz',
    actions: [{ ...ACTION, key: 'baz', path: 'baz', description: 'barbaz' }]
  };
  const domains = [DOMAIN1, DOMAIN2, domain];
  expect(shallow(<Menu {...PROPS} domains={domains} searchQuery="Bar" />)).toMatchSnapshot();
});
