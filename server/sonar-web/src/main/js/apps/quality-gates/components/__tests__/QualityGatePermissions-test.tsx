/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { addUser, searchUsers } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../../../helpers/mocks/users';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import QualityGatePermissions from '../QualityGatePermissions';

jest.mock('../../../../api/quality-gates', () => ({
  addUser: jest.fn().mockResolvedValue(undefined),
  searchUsers: jest.fn().mockResolvedValue({ users: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fetch users', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(searchUsers).toBeCalledWith({ qualityGate: '1', selected: 'selected' });
});

it('should fetch users on update', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (searchUsers as jest.Mock).mockClear();

  wrapper.setProps({ qualityGate: mockQualityGate({ id: '2' }) });
  expect(searchUsers).toBeCalledWith({ qualityGate: '2', selected: 'selected' });
});

it('should handleCloseAddPermission', () => {
  const wrapper = shallowRender();
  wrapper.setState({ showAddModal: true });
  wrapper.instance().handleCloseAddPermission();
  expect(wrapper.state().showAddModal).toBe(false);
});

it('should handleClickAddPermission', () => {
  const wrapper = shallowRender();
  wrapper.setState({ showAddModal: false });
  wrapper.instance().handleClickAddPermission();
  expect(wrapper.state().showAddModal).toBe(true);
});

it('should handleSubmitAddPermission', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(0);

  wrapper.instance().handleSubmitAddPermission(mockUserBase({ login: 'user1', name: 'User One' }));
  expect(wrapper.state().addingUser).toBe(true);

  expect(addUser).toBeCalledWith({ qualityGate: '1', userLogin: 'user1' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().addingUser).toBe(false);
  expect(wrapper.state().showAddModal).toBe(false);
  expect(wrapper.state().users).toHaveLength(1);
});

it('should handleSubmitAddPermission if it returns an error', async () => {
  (addUser as jest.Mock).mockRejectedValueOnce(undefined);
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().users).toHaveLength(0);

  wrapper.instance().handleSubmitAddPermission(mockUserBase({ login: 'user1', name: 'User One' }));
  expect(wrapper.state().addingUser).toBe(true);

  expect(addUser).toBeCalledWith({ qualityGate: '1', userLogin: 'user1' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().addingUser).toBe(false);
  expect(wrapper.state().showAddModal).toBe(true);
  expect(wrapper.state().users).toHaveLength(1);
});

function shallowRender(overrides: Partial<QualityGatePermissions['props']> = {}) {
  return shallow<QualityGatePermissions>(
    <QualityGatePermissions qualityGate={mockQualityGate()} {...overrides} />
  );
}
