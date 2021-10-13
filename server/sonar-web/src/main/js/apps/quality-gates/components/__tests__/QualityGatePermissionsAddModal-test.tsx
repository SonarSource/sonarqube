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
import { searchUsers } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../../../helpers/mocks/users';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import QualityGatePermissionsAddModal from '../QualityGatePermissionsAddModal';

jest.mock('../../../../api/quality-gates', () => ({
  searchUsers: jest.fn().mockResolvedValue({ users: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ submitting: true })).toMatchSnapshot('submitting');
});

it('should fetch users', async () => {
  (searchUsers as jest.Mock).mockResolvedValueOnce({ users: [mockUserBase()] });

  const wrapper = shallowRender();

  const query = 'query';

  wrapper.instance().handleSearch(query);

  expect(wrapper.state().loading).toBe(true);
  expect(searchUsers).toBeCalledWith({ qualityGate: '1', q: query, selected: 'deselected' });

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().searchResults).toHaveLength(1);
});

it('should handle input change', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleSearch = jest.fn();
  const { handleSearch } = wrapper.instance();

  wrapper.instance().handleInputChange('a');

  expect(wrapper.state().query).toBe('a');
  expect(handleSearch).not.toBeCalled();

  const query = 'query';
  wrapper.instance().handleInputChange(query);

  expect(wrapper.state().query).toBe(query);
  expect(handleSearch).toBeCalledWith(query);
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
