/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import SelectList, { SelectListFilter } from 'sonar-ui-common/components/controls/SelectList';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  addProjectToApplication,
  getApplicationProjects,
  removeProjectFromApplication
} from '../../../api/application';
import { mockApplication } from '../../../helpers/mocks/application';
import ApplicationProjects from '../ApplicationProjects';

jest.mock('../../../api/application', () => ({
  getApplicationProjects: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 3, total: 55 },
    projects: [
      { key: 'test1', name: 'test1', selected: false },
      { key: 'test2', name: 'test2', selected: false, disabled: true, includedIn: 'foo' },
      { key: 'test3', name: 'test3', selected: true }
    ]
  }),
  addProjectToApplication: jest.fn().mockResolvedValue({}),
  removeProjectFromApplication: jest.fn().mockResolvedValue({})
}));

beforeEach(jest.clearAllMocks);

it('should render correctly in application mode', async () => {
  const wrapper = shallowRender();
  wrapper
    .find(SelectList)
    .props()
    .onSearch({ query: '', filter: SelectListFilter.Selected, page: 1, pageSize: 100 });
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test1')).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test2')).toMatchSnapshot();

  expect(getApplicationProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      application: 'foo',
      p: 1,
      ps: 100,
      q: undefined,
      selected: SelectListFilter.Selected
    })
  );

  wrapper.instance().handleSelect('test1');
  await waitAndUpdate(wrapper);
  expect(addProjectToApplication).toHaveBeenCalledWith('foo', 'test1');

  wrapper.instance().fetchProjects({ query: 'bar', filter: SelectListFilter.Selected });
  await waitAndUpdate(wrapper);
  expect(getApplicationProjects).toHaveBeenCalledWith(
    expect.objectContaining({ application: 'foo', q: 'bar', selected: SelectListFilter.Selected })
  );

  wrapper.instance().handleUnselect('test1');
  await waitAndUpdate(wrapper);
  expect(removeProjectFromApplication).toHaveBeenCalledWith('foo', 'test1');
});

it('should refresh properly if props changes', () => {
  const wrapper = shallowRender();
  const spy = jest.spyOn(wrapper.instance(), 'fetchProjects');

  wrapper.setProps({ application: { key: 'bar' } as any });
  expect(wrapper.state().lastSearchParams.applicationKey).toBe('bar');
  expect(spy).toHaveBeenCalled();
});

function shallowRender(props: Partial<ApplicationProjects['props']> = {}) {
  return shallow<ApplicationProjects>(
    <ApplicationProjects application={mockApplication()} {...props} />
  );
}
