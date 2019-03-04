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
import * as React from 'react';
import { shallow } from 'enzyme';
import ManualProjectCreate from '../ManualProjectCreate';
import { change, submit, waitAndUpdate } from '../../../../helpers/testUtils';
import { createProject } from '../../../../api/components';

jest.mock('../../../../api/components', () => ({
  createProject: jest.fn().mockResolvedValue({ project: { key: 'bar', name: 'Bar' } })
}));

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(true)
}));

beforeEach(() => {
  (createProject as jest.Mock<any>).mockClear();
});

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should correctly create a public project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = getWrapper({ onProjectCreate });
  wrapper.find('withRouter(OrganizationInput)').prop<Function>('onChange')({ key: 'foo' });

  change(wrapper.find('ProjectKeyInput'), 'bar');
  change(wrapper.find('input#project-name'), 'Bar');
  expect(wrapper.find('SubmitButton').prop('disabled')).toBe(false);

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({
    project: 'bar',
    name: 'Bar',
    organization: 'foo',
    visibility: 'public'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['bar']);
});

it('should correctly create a private project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = getWrapper({ onProjectCreate });
  wrapper.find('withRouter(OrganizationInput)').prop<Function>('onChange')({ key: 'bar' });

  change(wrapper.find('ProjectKeyInput'), 'bar');
  change(wrapper.find('input#project-name'), 'Bar');

  submit(wrapper.find('form'));
  expect(createProject).toBeCalledWith({
    project: 'bar',
    name: 'Bar',
    organization: 'bar',
    visibility: 'private'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['bar']);
});

function getWrapper(props = {}) {
  return shallow(
    <ManualProjectCreate
      currentUser={{ groups: [], isLoggedIn: true, login: 'foo', name: 'Foo', scmAccounts: [] }}
      fetchMyOrganizations={jest.fn()}
      onProjectCreate={jest.fn()}
      userOrganizations={[
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar', subscription: 'PAID' }
      ]}
      {...props}
    />
  );
}
