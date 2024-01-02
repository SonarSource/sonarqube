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
import { shallow } from 'enzyme';
import * as React from 'react';
import { ComponentQualifier } from '../../../../types/component';
import SearchResult from '../SearchResult';

it('renders selected', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ selected: true });
  expect(wrapper).toMatchSnapshot();
});

it('renders match', () => {
  const component = {
    key: 'foo',
    name: 'foo',
    match: 'f<mark>o</mark>o',
    qualifier: ComponentQualifier.Project,
  };
  const wrapper = shallowRender({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders favorite', () => {
  const component = {
    isFavorite: true,
    key: 'foo',
    name: 'foo',
    qualifier: ComponentQualifier.Project,
  };
  const wrapper = shallowRender({ component });
  expect(wrapper).toMatchSnapshot();
});

it('renders recently browsed', () => {
  const component = {
    isRecentlyBrowsed: true,
    key: 'foo',
    name: 'foo',
    qualifier: ComponentQualifier.Project,
  };
  const wrapper = shallowRender({ component });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<SearchResult['props']> = {}) {
  return shallow(
    <SearchResult
      component={{ key: 'foo', name: 'foo', qualifier: ComponentQualifier.Project }}
      innerRef={jest.fn()}
      onClose={jest.fn()}
      onSelect={jest.fn()}
      selected={false}
      {...props}
    />
  );
}
