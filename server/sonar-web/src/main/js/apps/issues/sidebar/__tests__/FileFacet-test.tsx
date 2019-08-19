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
import { Query, ReferencedComponent } from '../../utils';
import FileFacet from '../FileFacet';

it('should render correctly', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(wrapper).toMatchSnapshot();
  expect(
    instance.renderSearchResult({ path: 'foo/bar.js' } as TreeComponentWithPath, 'foo')
  ).toMatchSnapshot();
  expect(instance.renderFacetItem('fooUuid')).toMatchSnapshot();
});

describe("ListStyleFacet's callback props", () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  test('#getSearchResultText()', () => {
    expect(instance.getSearchResultText({ path: 'foo/bar.js' } as TreeComponentWithPath)).toBe(
      'foo/bar.js'
    );
  });

  test('#getSearchResultKey()', () => {
    expect(instance.getSearchResultKey({ key: 'foo' } as TreeComponentWithPath)).toBe('fooUuid');
    expect(instance.getSearchResultKey({ key: 'bar' } as TreeComponentWithPath)).toBe('bar');
  });

  test('#getFacetItemText()', () => {
    expect(instance.getFacetItemText('fooUuid')).toBe('foo/bar.js');
    expect(instance.getFacetItemText('bar')).toBe('bar');
  });
});

function shallowRender(props: Partial<FileFacet['props']> = {}) {
  return shallow<FileFacet>(
    <FileFacet
      componentKey="foo"
      fetching={false}
      fileUuids={['foo', 'bar']}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      query={{} as Query}
      referencedComponents={{
        fooUuid: { key: 'foo', uuid: 'fooUuid', path: 'foo/bar.js' } as ReferencedComponent
      }}
      stats={undefined}
      {...props}
    />
  );
}
