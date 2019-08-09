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
import { TreeComponentWithPath } from '../../../../api/components';
import { Query } from '../../utils';
import DirectoryFacet from '../DirectoryFacet';

it('should render correctly', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(wrapper).toMatchSnapshot();
  expect(
    instance.renderSearchResult({ path: 'foo/bar' } as TreeComponentWithPath, 'foo')
  ).toMatchSnapshot();
  expect(instance.renderFacetItem('foo/bar')).toMatchSnapshot();
});

describe("ListStyleFacet's callback props", () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  test('#getSearchResultText()', () => {
    expect(instance.getSearchResultText({ name: 'bar' } as TreeComponentWithPath)).toBe('bar');
  });

  test('#getSearchResultKey()', () => {
    expect(instance.getSearchResultKey({ path: 'foo/bar' } as TreeComponentWithPath)).toBe(
      'foo/bar'
    );
  });

  test('#getFacetItemText()', () => {
    expect(instance.getFacetItemText('foo/bar')).toBe('foo/bar');
  });
});

function shallowRender(props: Partial<DirectoryFacet['props']> = {}) {
  return shallow<DirectoryFacet>(
    <DirectoryFacet
      componentKey="foo"
      directories={['foo/', 'bar/baz/']}
      fetching={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      query={{} as Query}
      stats={undefined}
      {...props}
    />
  );
}
