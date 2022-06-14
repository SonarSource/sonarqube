/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getTree } from '../../../../api/components';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { Search } from '../Search';

jest.mock('../../../../api/components', () => {
  const { mockTreeComponent, mockComponent } = jest.requireActual(
    '../../../../helpers/mocks/component'
  );

  return {
    getTree: jest.fn().mockResolvedValue({
      baseComponent: mockTreeComponent(),
      components: [mockComponent()],
      paging: { pageIndex: 0, pageSize: 5, total: 20 }
    })
  };
});

it('should render correcly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ component: mockComponent({ qualifier: ComponentQualifier.Portfolio }) })
  ).toMatchSnapshot('new code toggle for portfolio');
  expect(
    shallowRender({
      component: mockComponent({ qualifier: ComponentQualifier.Portfolio }),
      location: mockLocation({ query: { id: 'foo', search: 'bar' } })
    })
  ).toMatchSnapshot('new code toggle for portfolio disabled');
});

it('should search correct query on mount', async () => {
  const onSearchResults = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ query: { id: 'foo', search: 'bar' } }),
    onSearchResults
  });
  await waitAndUpdate(wrapper);
  expect(getTree).toHaveBeenCalledWith({
    component: 'my-project',
    q: 'bar',
    qualifiers: 'UTS,FIL',
    s: 'qualifier,name'
  });
  expect(onSearchResults).toHaveBeenCalledWith([
    {
      breadcrumbs: [],
      key: 'my-project',
      name: 'MyProject',
      qualifier: 'TRK',
      qualityGate: { isDefault: true, key: '30', name: 'Sonar way' },
      qualityProfiles: [{ deleted: false, key: 'my-qp', language: 'ts', name: 'Sonar way' }],
      tags: []
    }
  ]);
});

it('should handle search correctly', async () => {
  const router = mockRouter();
  const onSearchClear = jest.fn();
  const wrapper = shallowRender({ router, onSearchClear });
  wrapper.instance().handleQueryChange('foo');
  await waitAndUpdate(wrapper);
  expect(router.replace).toHaveBeenCalledWith({
    pathname: '/path',
    query: {
      search: 'foo'
    }
  });
  expect(getTree).toHaveBeenCalledWith({
    component: 'my-project',
    q: 'foo',
    qualifiers: 'UTS,FIL',
    s: 'qualifier,name'
  });

  wrapper.instance().handleQueryChange('');
  await waitAndUpdate(wrapper);
  expect(router.replace).toHaveBeenCalledWith({
    pathname: '/path',
    query: {}
  });
  expect(onSearchClear).toHaveBeenCalledWith();
});

function shallowRender(props?: Partial<Search['props']>) {
  return shallow<Search>(
    <Search
      newCodeSelected={false}
      component={mockComponent()}
      location={mockLocation()}
      onSearchClear={jest.fn()}
      onSearchResults={jest.fn()}
      onNewCodeToggle={jest.fn()}
      router={mockRouter()}
      {...props}
    />
  );
}
