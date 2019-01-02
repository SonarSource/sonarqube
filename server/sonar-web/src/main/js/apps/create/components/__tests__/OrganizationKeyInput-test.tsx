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
import OrganizationKeyInput from '../OrganizationKeyInput';
import { getOrganization } from '../../../../api/organizations';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/organizations', () => ({
  getOrganization: jest.fn().mockResolvedValue(undefined)
}));

beforeEach(() => {
  (getOrganization as jest.Mock<any>).mockClear();
});

it('should render correctly', () => {
  const wrapper = shallow(<OrganizationKeyInput initialValue="key" onChange={jest.fn()} />);
  expect(wrapper).toMatchSnapshot();
  wrapper.setState({ touched: true });
  expect(wrapper.find('ValidationInput').prop('isValid')).toMatchSnapshot();
});

it('should not display any status when the key is not defined', async () => {
  const wrapper = shallow(<OrganizationKeyInput onChange={jest.fn()} />);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(false);
  expect(wrapper.find('ValidationInput').prop('isValid')).toBe(false);
});

it('should have an error when the key is invalid', async () => {
  const wrapper = shallow(
    <OrganizationKeyInput initialValue="KEy-with#speci@l_char" onChange={jest.fn()} />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(true);
});

it('should have an error when the key already exists', async () => {
  (getOrganization as jest.Mock<any>).mockResolvedValue({});
  const wrapper = shallow(<OrganizationKeyInput initialValue="" onChange={jest.fn()} />);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('ValidationInput').prop('isInvalid')).toBe(true);
});
