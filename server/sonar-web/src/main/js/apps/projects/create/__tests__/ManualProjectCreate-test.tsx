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
import { ManualProjectCreate } from '../ManualProjectCreate';
import { change, click, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { createProject } from '../../../../api/components';

jest.mock('../../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({ project: { key: 'bar', name: 'Bar' } })
}));

beforeEach(() => {
  (createProject as jest.Mock<any>).mockClear();
});

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should allow to create a new org', async () => {
  const fetchMyOrganizations = jest.fn().mockResolvedValueOnce([]);
  const wrapper = getWrapper({ fetchMyOrganizations });

  click(wrapper.find('.js-new-org'));
  const createForm = wrapper.find('Connect(CreateOrganizationForm)');
  expect(createForm.exists()).toBeTruthy();

  createForm.prop<Function>('onCreate')({ key: 'baz' });
  expect(fetchMyOrganizations).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('selectedOrganization')).toBe('baz');
});

it('should correctly create a project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = getWrapper({ onProjectCreate });
  wrapper.find('Select').prop<Function>('onChange')({ value: 'foo' });
  change(wrapper.find('#project-name'), 'Bar');
  expect(wrapper.find('SubmitButton')).toMatchSnapshot();

  change(wrapper.find('#project-key'), 'bar');
  expect(wrapper.find('SubmitButton')).toMatchSnapshot();

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({ project: 'bar', name: 'Bar', organization: 'foo' });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith([{ key: 'bar', name: 'Bar' }]);
});

function getWrapper(props = {}) {
  return shallow(
    <ManualProjectCreate
      currentUser={{ isLoggedIn: true, login: 'foo', name: 'Foo' }}
      fetchMyOrganizations={jest.fn()}
      onProjectCreate={jest.fn()}
      userOrganizations={[{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }]}
      {...props}
    />
  );
}
