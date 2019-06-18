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
import Projects, { SearchParams } from '../Projects';
import SelectList, { Filter } from '../../../../components/SelectList/SelectList';
import { mockQualityGate } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import {
  searchProjects,
  associateGateWithProject,
  dissociateGateWithProject
} from '../../../../api/quality-gates';

jest.mock('../../../../api/quality-gates', () => ({
  searchProjects: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 3, total: 55 },
    results: [
      { id: 'test1', name: 'test1', selected: false },
      { id: 'test2', name: 'test2', selected: false },
      { id: 'test3', name: 'test3', selected: true }
    ]
  }),
  associateGateWithProject: jest.fn().mockResolvedValue({}),
  dissociateGateWithProject: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(searchProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      page: 1
    })
  );

  wrapper.setState({ listHasBeenTouched: true });
  expect(wrapper.find(SelectList).props().needReload).toBe(true);

  wrapper.setState({ lastSearchParams: { selected: Filter.All } as SearchParams });
  expect(wrapper.find(SelectList).props().needReload).toBe(false);
});

it('should handle reload properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleReload();
  expect(searchProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      page: 1
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle search reload properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSearch('foo', Filter.Selected);
  expect(searchProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      page: 1,
      query: 'foo',
      selected: Filter.Selected
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle load more properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleLoadMore();
  expect(searchProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      page: 2
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(false);
});

it('should handle selection properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSelect('toto');
  await waitAndUpdate(wrapper);
  expect(associateGateWithProject).toHaveBeenCalledWith(
    expect.objectContaining({
      projectId: 'toto'
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(true);
});

it('should handle deselection properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleUnselect('tata');
  await waitAndUpdate(wrapper);
  expect(dissociateGateWithProject).toHaveBeenCalledWith(
    expect.objectContaining({
      projectId: 'tata'
    })
  );
  expect(wrapper.state().listHasBeenTouched).toBe(true);
});

function shallowRender(props: Partial<Projects['props']> = {}) {
  return shallow<Projects>(<Projects qualityGate={mockQualityGate()} {...props} />);
}
