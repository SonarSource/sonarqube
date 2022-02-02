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
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import QualityGatePermissionsAddModal from '../QualityGatePermissionsAddModal';

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

  expect(wrapper.state().loading).toBe(true);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(searchUsers).toBeCalledWith({ gateName: 'qualitygate', q: '', selected: 'deselected' });
  expect(searchGroups).toBeCalledWith({
    gateName: 'qualitygate',
    q: '',
    selected: 'deselected'
  });
  expect(wrapper.state().searchResults).toHaveLength(2);
});

it('should fetch users and groups', async () => {
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [mockUserBase()] });
  (searchGroups as jest.Mock).mockResolvedValueOnce({ groups: [{ name: 'group' }] });

  const wrapper = shallowRender();
  const query = 'query';

  wrapper.instance().handleSearch(query);

  expect(wrapper.state().loading).toBe(true);
  expect(searchUsers).toBeCalledWith({ gateName: 'qualitygate', q: query, selected: 'deselected' });
  expect(searchGroups).toBeCalledWith({
    gateName: 'qualitygate',
    q: query,
    selected: 'deselected'
  });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().searchResults).toHaveLength(2);
});

it('should handle input change', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleSearch = jest.fn();
  const { handleSearch } = wrapper.instance();

  wrapper.instance().handleInputChange('a');

  expect(wrapper.state().query).toBe('a');
  expect(handleSearch).toBeCalled();

  const query = 'query';
  wrapper.instance().handleInputChange(query);

  expect(wrapper.state().query).toBe(query);
  expect(handleSearch).toBeCalledWith(query);

  jest.clearAllMocks();
  wrapper.instance().handleInputChange(query); // input change with same parameter

  expect(wrapper.state().query).toBe(query);
  expect(handleSearch).not.toBeCalled();
});

it('should handleSelection', () => {
  const wrapper = shallowRender();
  const selection = mockUserBase();
  wrapper.instance().handleSelection(selection);
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
