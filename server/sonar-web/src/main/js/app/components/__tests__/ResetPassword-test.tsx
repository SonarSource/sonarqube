/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import ResetPasswordForm from '../../../components/common/ResetPasswordForm';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { ResetPassword, ResetPasswordProps } from '../ResetPassword';

jest.mock('../../../helpers/system', () => ({
  getBaseUrl: jest.fn().mockReturnValue('/context'),
}));

const originalLocation = window.location;

beforeAll(() => {
  const location = {
    ...window.location,
    href: null,
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should navigate to the homepage after submission', () => {
  const wrapper = shallowRender();
  const form = wrapper.find(ResetPasswordForm);
  const { onPasswordChange } = form.props();

  if (onPasswordChange) {
    onPasswordChange();
  }

  expect(window.location.href).toBe('/context/');
});

function shallowRender(props: Partial<ResetPasswordProps> = {}) {
  return shallow(<ResetPassword currentUser={mockLoggedInUser()} {...props} />);
}
