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
import { logOut } from '../../../../api/auth';
import { addGlobalErrorMessage } from '../../../../helpers/globalMessages';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import Logout from '../Logout';

jest.mock('../../../../api/auth', () => ({
  logOut: jest.fn().mockResolvedValue(true)
}));

jest.mock('../../../../helpers/globalMessages', () => ({
  addGlobalErrorMessage: jest.fn()
}));

const originalLocation = window.location;
beforeAll(() => {
  const location = {
    ...window.location,
    replace: jest.fn()
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location
  });
});

beforeEach(() => {
  jest.clearAllMocks();
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation
  });
});

it('should logout correctly', async () => {
  (logOut as jest.Mock).mockResolvedValue(true);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(logOut).toHaveBeenCalled();
  expect(window.location.replace).toHaveBeenCalledWith('/');
  expect(addGlobalErrorMessage).not.toHaveBeenCalled();
});

it('should not redirect if logout fails', async () => {
  (logOut as jest.Mock).mockRejectedValue(false);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(logOut).toHaveBeenCalled();
  expect(window.location.replace).not.toHaveBeenCalled();
  expect(addGlobalErrorMessage).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender() {
  return shallow(<Logout />);
}
