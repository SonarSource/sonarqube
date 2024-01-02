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
import { getFiles } from '../../../../api/components';
import ListStyleFacet from '../../../../components/facet/ListStyleFacet';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { TreeComponentWithPath } from '../../../../types/component';
import { Query } from '../../utils';
import FileFacet from '../FileFacet';

jest.mock('../../../../api/components', () => ({
  getFiles: jest.fn().mockResolvedValue({ components: [] }),
}));

beforeEach(() => jest.clearAllMocks());

const branch = mockBranch();
const component = mockComponent();

const PATH = 'foo/bar.js';

it('should render correctly', () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  expect(wrapper).toMatchSnapshot();
  expect(
    instance.renderSearchResult({ path: PATH } as TreeComponentWithPath, 'bar')
  ).toMatchSnapshot();
  expect(instance.renderFacetItem('fooUuid')).toMatchSnapshot();
});

it('should properly search for file', () => {
  const wrapper = shallowRender();

  const query = 'foo';

  wrapper.find(ListStyleFacet).props().onSearch(query);

  expect(getFiles).toHaveBeenCalledWith({
    branch: branch.name,
    component: component.key,
    q: query,
    ps: 30,
    p: undefined,
  });
});

describe("ListStyleFacet's callback props", () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  it('#getSearchResultText()', () => {
    expect(instance.getSearchResultText({ path: PATH } as TreeComponentWithPath)).toBe(
      'foo/bar.js'
    );
  });

  it('#getSearchResultKey()', () => {
    expect(instance.getSearchResultKey({ key: 'bar', path: 'bar' } as TreeComponentWithPath)).toBe(
      'bar'
    );
  });

  it('#getFacetItemText()', () => {
    expect(instance.getFacetItemText('bar')).toBe('bar');
  });
});

function shallowRender(props: Partial<FileFacet['props']> = {}) {
  return shallow<FileFacet>(
    <FileFacet
      branchLike={branch}
      componentKey={component.key}
      fetching={false}
      files={['foo', 'bar']}
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
