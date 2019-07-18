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
import SelectList, { SelectListFilter } from 'sonar-ui-common/components/controls/SelectList';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  associateProject,
  dissociateProject,
  getProfileProjects
} from '../../../../api/quality-profiles';
import ChangeProjectsForm from '../ChangeProjectsForm';

const profile: any = { key: 'profFile_key' };
const organization = 'TEST';

jest.mock('../../../../api/quality-profiles', () => ({
  getProfileProjects: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 3, total: 55 },
    results: [
      { id: 'test1', key: 'test1', name: 'test1', selected: false },
      { id: 'test2', key: 'test2', name: 'test2', selected: false },
      { id: 'test3', key: 'test3', name: 'test3', selected: true }
    ]
  }),
  associateProject: jest.fn().mockResolvedValue({}),
  dissociateProject: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  wrapper
    .find(SelectList)
    .props()
    .onSearch({
      query: '',
      filter: SelectListFilter.Selected,
      page: 1,
      pageSize: 100
    });
  await waitAndUpdate(wrapper);

  expect(wrapper.instance().mounted).toBe(true);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test1')).toMatchSnapshot();
  expect(wrapper.instance().renderElement('test_foo')).toMatchSnapshot();

  expect(getProfileProjects).toHaveBeenCalledWith(
    expect.objectContaining({
      key: profile.key,
      organization,
      p: 1,
      ps: 100,
      q: undefined,
      selected: SelectListFilter.Selected
    })
  );
  expect(wrapper.state().needToReload).toBe(false);

  wrapper.instance().componentWillUnmount();
  expect(wrapper.instance().mounted).toBe(false);
});

it('should handle selection properly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleSelect('toto');
  await waitAndUpdate(wrapper);

  expect(associateProject).toHaveBeenCalledWith(profile.key, 'toto');
  expect(wrapper.state().needToReload).toBe(true);
});

it('should handle deselection properly', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleUnselect('tata');
  await waitAndUpdate(wrapper);

  expect(dissociateProject).toHaveBeenCalledWith(profile.key, 'tata');
  expect(wrapper.state().needToReload).toBe(true);
});

it('should close modal properly', () => {
  const spy = jest.fn();
  const wrapper = shallowRender({ onClose: spy });
  click(wrapper.find('a'));

  expect(spy).toHaveBeenCalled();
});

function shallowRender(props: Partial<ChangeProjectsForm['props']> = {}) {
  return shallow<ChangeProjectsForm>(
    <ChangeProjectsForm
      onClose={jest.fn()}
      organization={organization}
      profile={profile}
      {...props}
    />
  );
}
