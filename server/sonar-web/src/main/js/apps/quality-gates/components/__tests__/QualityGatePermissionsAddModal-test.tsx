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
import { searchGroups, searchUsers } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../../../helpers/mocks/users';
import { mockEvent } from '../../../../helpers/testUtils';
import QualityGatePermissionsAddModal from '../QualityGatePermissionsAddModal';
import QualityGatePermissionsAddModalRenderer from '../QualityGatePermissionsAddModalRenderer';

jest.mock('../../../../api/quality-gates', () => ({
  searchUsers: jest.fn().mockResolvedValue({ users: [] }),
  searchGroups: jest.fn().mockResolvedValue({ groups: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fetch users and groups on mount', async () => {
  (searchUsers as jest.Mock).mockResolvedValue({ users: [mockUserBase()] });
  (searchGroups as jest.Mock).mockResolvedValue({ groups: [{ name: 'group' }] });

  const wrapper = shallowRender();

  const query = 'Waldo';
  const results = await new Promise(resolve => {
    wrapper.instance().handleSearch(query, resolve);
  });

  expect(searchUsers).toBeCalledWith(expect.objectContaining({ q: query }));
  expect(searchGroups).toBeCalledWith(expect.objectContaining({ q: query }));

  expect(results).toHaveLength(2);
});

it('should handleSelection', () => {
  const wrapper = shallowRender();
  const selection = { ...mockUserBase(), value: 'value' };
  wrapper
    .find(QualityGatePermissionsAddModalRenderer)
    .props()
    .onSelection(selection);
  expect(wrapper.state().selection).toBe(selection);
});

it('should handleSubmit', () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });

  wrapper.instance().handleSubmit(mockEvent());

  expect(onSubmit).not.toBeCalled();

  const selection = mockUserBase();
  wrapper.setState({ selection });
  wrapper.instance().handleSubmit(mockEvent());
  expect(onSubmit).toBeCalledWith(selection);
});

function shallowRender(overrides: Partial<QualityGatePermissionsAddModal['props']> = {}) {
  return shallow<QualityGatePermissionsAddModal>(
    <QualityGatePermissionsAddModal
      onClose={jest.fn()}
      onSubmit={jest.fn()}
      submitting={false}
      qualityGate={mockQualityGate()}
      {...overrides}
    />
  );
}
