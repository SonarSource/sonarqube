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
// @flow
import React from 'react';
import { shallow } from 'enzyme';
import SearchResult from '../SearchResult';

function render(props /*: ?Object */) {
  return shallow(
    // $FlowFixMe
    <SearchResult
      appState={{ organizationsEnabled: false }}
      component={{ key: 'foo', name: 'foo', qualifier: 'TRK', organization: 'bar' }}
      innerRef={jest.fn()}
      onClose={jest.fn()}
      onSelect={jest.fn()}
      organizations={{ bar: { name: 'bar' } }}
      projects={{ foo: { name: 'foo' } }}
      selected={false}
      {...props}
    />
  );
}

jest.useFakeTimers();

it('renders selected', () => {
  const wrapper = render();
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ selected: true });
  expect(wrapper).toMatchSnapshot();
});

it('renders match', () => {
  const component = {
    key: 'foo',
    name: 'foo',
    match: 'f<mark>o</mark>o',
    qualifier: 'TRK',
    organization: 'bar'
  };
  const wrapper = render({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders favorite', () => {
  const component = {
    isFavorite: true,
    key: 'foo',
    name: 'foo',
    qualifier: 'TRK',
    organization: 'bar'
  };
  const wrapper = render({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders recently browsed', () => {
  const component = {
    isRecentlyBrowsed: true,
    key: 'foo',
    name: 'foo',
    qualifier: 'TRK',
    organization: 'bar'
  };
  const wrapper = render({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders projects', () => {
  const component = {
    isRecentlyBrowsed: true,
    key: 'qwe',
    name: 'qwe',
    qualifier: 'BRC',
    project: 'foo'
  };
  const wrapper = render({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders organizations', () => {
  const component = {
    isRecentlyBrowsed: true,
    key: 'foo',
    name: 'foo',
    qualifier: 'TRK',
    organization: 'bar'
  };
  const wrapper = render({ appState: { organizationsEnabled: true }, component });
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ appState: { organizationsEnabled: false } });
  expect(wrapper).toMatchSnapshot();
});

it('shows tooltip after delay', () => {
  const wrapper = render();
  expect(wrapper.find('Tooltip').prop('visible')).toBe(false);

  wrapper.setProps({ selected: true });
  expect(wrapper.find('Tooltip').prop('visible')).toBe(false);

  jest.runAllTimers();
  wrapper.update();
  expect(wrapper.find('Tooltip').prop('visible')).toBe(true);

  wrapper.setProps({ selected: false });
  expect(wrapper.find('Tooltip').prop('visible')).toBe(false);
});
